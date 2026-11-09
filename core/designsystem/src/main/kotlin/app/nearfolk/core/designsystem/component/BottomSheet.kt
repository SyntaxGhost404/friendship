package app.nearfolk.core.designsystem.component

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet.
 *
 * The sheet rests at the bottom of the screen at the height of its content.
 * From there it can only move down: it follows the finger when dragged down,
 * also through a list that is already at its top, and closes when it is
 * released far enough or fast enough. It never rises above its resting place,
 * stretches or scales, however hard the content is flung. See
 * [NfPopupContent] for why.
 *
 * The sheet pads itself for the navigation bar and the keyboard, so content
 * must not add `imePadding` or `navigationBarsPadding` of its own.
 *
 * [modifier] applies to the moving surface.
 */
@Composable
fun NfBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val motion = remember(scope, density) {
        SheetMotion(scope, flingThreshold = with(density) { FlingToClose.toPx() }) { currentOnDismiss() }
    }
    LaunchedEffect(motion) { motion.enter() }

    NfOverlayWindow(onDismissRequest = { motion.hide() }) {
        val scrim = Nf.colors.scrim
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(scrim, alpha = motion.visibleFraction) }
                    .pointerInput(motion) { detectTapGestures { motion.hide() } },
            )
            CompositionLocalProvider(LocalContentColor provides Nf.colors.textPrimary) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(top = 8.dp)
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .offset { IntOffset(0, motion.placement) }
                        .then(modifier)
                        .onSizeChanged { motion.height = it.height }
                        .nestedScroll(motion.connection)
                        .draggable(
                            state = rememberDraggableState { motion.dragBy(it) },
                            orientation = Orientation.Vertical,
                            enabled = !motion.hiding,
                            onDragStopped = { velocity -> motion.settle(velocity) },
                        )
                        .shapeSurface(Nf.colors.surface, NfShapes.sheet)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .semantics {
                            isTraversalGroup = true
                            if (title != null) paneTitle = title
                            dismiss {
                                motion.hide()
                                true
                            }
                        }
                        .padding(bottom = 12.dp),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp, bottom = 6.dp)
                            .size(width = 36.dp, height = 5.dp)
                            .background(Nf.colors.lineStrong, NfShapes.badge),
                    )
                    if (title != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(title, style = Nf.type.title, color = Nf.colors.textPrimary)
                        }
                    }
                    content()
                }
            }
        }
    }
}

private val FlingToClose = 600.dp
private const val CloseFraction = 0.4f
private const val RestStiffness = 450f
private const val HideStiffness = 600f

/**
 * Position of a sheet. [offset] is how far the sheet sits below its resting
 * place, in pixels: zero at rest, [height] when fully off screen. It is
 * clamped to that range everywhere it is written, which is what keeps the
 * sheet from ever travelling above rest.
 */
@Stable
private class SheetMotion(
    private val scope: CoroutineScope,
    private val flingThreshold: Float,
    private val onHidden: () -> Unit,
) {
    var offset by mutableFloatStateOf(0f)
        private set
    var height by mutableIntStateOf(0)
    var hiding by mutableStateOf(false)
        private set
    private var entered by mutableStateOf(false)
    private var job: Job? = null

    /** Vertical placement. Off screen until the first measure has told us the height. */
    val placement: Int get() = if (entered) offset.roundToInt() else height

    /** 0 while off screen, 1 at rest. Drives the scrim. */
    val visibleFraction: Float
        get() = if (!entered || height == 0) 0f else 1f - (offset / height).coerceIn(0f, 1f)

    val connection = object : NestedScrollConnection {
        // Finger moving up while the sheet is pulled down: bring the sheet back before the content scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y >= 0f || offset <= 0f) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        // Content is at its top and the finger keeps moving down: the sheet follows.
        // Leftover movement from a fling is ignored, so a flung list never moves the sheet.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    fun enter() = run {
        val measured = snapshotFlow { height }.first { it > 0 }
        offset = measured.toFloat()
        entered = true
        rest(0f)
    }

    /** Moves the sheet with the finger. Returns how much of [delta] was used. */
    fun dragBy(delta: Float): Float {
        if (hiding || !entered || delta == 0f) return 0f
        job?.cancel()
        val target = (offset + delta).coerceIn(0f, height.toFloat())
        val used = target - offset
        offset = target
        return used
    }

    /** Called when the finger lifts. [velocity] is positive downwards. */
    fun settle(velocity: Float) {
        if (hiding || !entered) return
        val close = when {
            velocity > flingThreshold -> true
            velocity < -flingThreshold -> false
            else -> offset > height * CloseFraction
        }
        if (close) hide(velocity) else run { rest(velocity) }
    }

    fun hide(velocity: Float = 0f) {
        if (hiding) return
        hiding = true
        run {
            if (entered) {
                val end = height.toFloat()
                // Aim past the edge so the sheet leaves at speed instead of creeping the last pixels.
                AnimationState(offset, velocity.coerceAtLeast(0f)).animateTo(
                    end * 1.15f + 1f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = HideStiffness),
                ) {
                    offset = value.coerceIn(0f, end)
                    if (value >= end) cancelAnimation()
                }
                offset = end
            }
            onHidden()
            // The caller normally removes the sheet now, which cancels this scope.
            // If it keeps the sheet, bring it back.
            repeat(2) { withFrameNanos { } }
            hiding = false
            rest(0f)
        }
    }

    private suspend fun rest(velocity: Float) {
        // A critically damped spring passes its target once if it starts faster than
        // sqrt(stiffness) * distance. Cap the speed so the sheet arrives without overshoot.
        val fastest = sqrt(RestStiffness) * offset
        AnimationState(offset, velocity.coerceAtLeast(-fastest)).animateTo(
            0f,
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = RestStiffness, visibilityThreshold = 0.5f),
        ) {
            offset = value.coerceIn(0f, height.toFloat())
            if (value <= 0f) cancelAnimation()
        }
        offset = 0f
    }

    private fun run(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch { block() }
    }
}
