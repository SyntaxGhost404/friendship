package app.nearfolk.core.designsystem.component

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.nearfolk.core.designsystem.R
import app.nearfolk.core.designsystem.theme.Nf
import java.util.UUID

/**
 * Rendering rules for anything that lives in its own window: sheets, menus,
 * dialogs and full screen viewers.
 *
 * Popup windows are translucent, and on a number of devices the stretch
 * overscroll shader corrupts its texture there, which shows up as static
 * across the popup when a list is flung against its edge. Inside a popup:
 *
 * 1. Scroll containers have no overscroll effect. They stop at the edge.
 * 2. Surfaces are filled with their shape (see [shapeSurface]) instead of
 *    clipping a whole scroll container to a path.
 * 3. Movement is a translation. Nothing is scaled, and nothing is drawn
 *    through an offscreen buffer.
 *
 * Wrap the content of every popup window in this.
 */
@Composable
fun NfPopupContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollFactory provides null, content = content)
}

/**
 * Fills the bounds with [shape] and optionally outlines it, without clipping
 * the content. Used for popup surfaces, where a path clip around a scrolling
 * list is what we are avoiding. Content has to stay inside the shape by its
 * own padding.
 */
fun Modifier.shapeSurface(
    fill: Color,
    shape: Shape,
    stroke: Color = Color.Unspecified,
    strokeWidth: Dp = 1.dp,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val width = strokeWidth.toPx()
    val inner = if (stroke == Color.Unspecified || size.minDimension <= width) {
        null
    } else {
        shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
    }
    onDrawBehind {
        drawOutline(outline, fill)
        if (inner != null) {
            translate(width / 2f, width / 2f) { drawOutline(inner, stroke, style = Stroke(width)) }
        }
    }
}

/**
 * A full screen, transparent window above the current one. The content draws
 * everything, including the scrim, and is responsible for its own entrance
 * and exit. The window itself never animates, dims or resizes, which keeps
 * the system from scaling or fading the surface behind our back.
 *
 * Back and Escape call [onDismissRequest]. The window stays until the caller
 * removes this composable. A `BackHandler` inside [content] runs first, so a
 * popup with pages of its own can step back through them.
 */
@Composable
fun NfOverlayWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val lightBars = !Nf.colors.isDark
    val parent = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val id = rememberSaveable { UUID.randomUUID() }
    val host = remember(view) {
        OverlayHost(view, id, onDismissRequest, layoutDirection, lightBars).apply {
            val host = this
            setContent(parent) {
                // Back presses arrive at this window, not at the screen behind it.
                CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides host) {
                    Box(Modifier.semantics { dialog() }) {
                        NfPopupContent { currentContent() }
                    }
                }
            }
        }
    }
    DisposableEffect(host) {
        host.show()
        onDispose {
            host.dismiss()
            host.disposeComposition()
        }
    }
    SideEffect { host.update(onDismissRequest, layoutDirection, lightBars) }
}

private class OverlayHost(
    anchor: View,
    id: UUID,
    private var onDismissRequest: () -> Unit,
    layoutDirection: LayoutDirection,
    lightBars: Boolean,
) : ComponentDialog(anchor.context, R.style.NfOverlayWindow) {

    private val layout: OverlayLayout

    init {
        val window = window ?: error("Overlay has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        // The content pads itself for the keyboard, so the window must not resize or pan.
        window.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.isNavigationBarContrastEnforced = false
        // A screen that hides its content from screenshots keeps doing so in the overlay.
        val hostFlags = (anchor.rootView.layoutParams as? WindowManager.LayoutParams)?.flags ?: 0
        if (hostFlags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        layout = OverlayLayout(context, window).apply {
            // Lets rememberSaveable inside the overlay survive recreation.
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Overlay:$id")
            clipChildren = false
        }
        setContentView(layout)
        // State owners come from the screen that opened the overlay, not from the dialog.
        layout.setViewTreeLifecycleOwner(anchor.findViewTreeLifecycleOwner())
        layout.setViewTreeViewModelStoreOwner(anchor.findViewTreeViewModelStoreOwner())
        layout.setViewTreeSavedStateRegistryOwner(anchor.findViewTreeSavedStateRegistryOwner())

        update(onDismissRequest, layoutDirection, lightBars)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = this@OverlayHost.onDismissRequest()
            },
        )
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) = layout.setContent(parent, content)

    fun update(onDismissRequest: () -> Unit, layoutDirection: LayoutDirection, lightBars: Boolean) {
        this.onDismissRequest = onDismissRequest
        layout.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
            LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
        }
        val window = window ?: return
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }

    fun disposeComposition() = layout.disposeComposition()

    /** Dismissal is the caller's decision, so the platform shortcut does nothing. */
    override fun cancel() = Unit
}

@SuppressLint("ViewConstructor")
private class OverlayLayout(context: Context, override val window: Window) :
    AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}
