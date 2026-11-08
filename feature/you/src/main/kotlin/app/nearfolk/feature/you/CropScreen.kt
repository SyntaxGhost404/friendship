package app.nearfolk.feature.you

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pan and zoom crop. The window is a squircle for avatars and a wide
 * rounded frame for covers; the image always covers the window.
 */
@Composable
fun CropRouteScreen(onDone: () -> Unit, viewModel: CropViewModel = hiltViewModel()) {
    val load by viewModel.load.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    val target = viewModel.target
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        NfTopBar(
            stringResource(if (target == CropTarget.Avatar) R.string.crop_avatar_title else R.string.crop_banner_title),
            onBack = onDone,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
        )
        when (val state = load) {
            CropViewModel.Load.Loading -> NfSpinner()
            CropViewModel.Load.Failed -> NfEmptyState(Icons.Rounded.BrokenImage, stringResource(R.string.crop_failed), "")
            is CropViewModel.Load.Ready -> {
                val bitmap = state.bitmap
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                var zoom by remember { mutableFloatStateOf(1f) }
                var pan by remember { mutableStateOf(Offset.Zero) }
                var window by remember { mutableStateOf(Size.Zero) }
                var base by remember { mutableFloatStateOf(1f) }
                val density = LocalDensity.current
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val margin = with(density) { 24.dp.toPx() }
                    val w = constraints.maxWidth - 2 * margin
                    val h = w / target.aspect
                    val fit = max(w / bitmap.width, h / bitmap.height)
                    SideEffect {
                        window = Size(w, h)
                        base = fit
                    }
                    fun clamp(p: Offset, z: Float): Offset {
                        val s = fit * z
                        val maxX = ((bitmap.width * s) - w).coerceAtLeast(0f) / 2
                        val maxY = ((bitmap.height * s) - h).coerceAtLeast(0f) / 2
                        return Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
                    }
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                detectTransformGestures { _, panChange, zoomChange, _ ->
                                    val z = (zoom * zoomChange).coerceIn(1f, 5f)
                                    zoom = z
                                    pan = clamp(pan + panChange, z)
                                }
                            },
                    ) {
                        val s = fit * zoom
                        val cx = size.width / 2
                        val cy = size.height / 2
                        drawImage(
                            image,
                            dstOffset = IntOffset((cx + pan.x - bitmap.width * s / 2).roundToInt(), (cy + pan.y - bitmap.height * s / 2).roundToInt()),
                            dstSize = IntSize((bitmap.width * s).roundToInt(), (bitmap.height * s).roundToInt()),
                        )
                        val shape = if (target == CropTarget.Avatar) NfShapes.avatar else NfShapes.large
                        val outline = shape.createOutline(Size(w, h), LayoutDirection.Ltr, this)
                        val path = Path().apply {
                            if (outline is Outline.Generic) addPath(outline.path, Offset(cx - w / 2, cy - h / 2))
                        }
                        clipPath(path, ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.62f)) }
                        translate(cx - w / 2, cy - h / 2) {
                            if (outline is Outline.Generic) drawPath(outline.path, Color.White.copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.crop_hint), style = Nf.type.bodySmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    NfButton(
                        stringResource(R.string.crop_use),
                        onClick = {
                            val s = base * zoom
                            viewModel.save(
                                left = bitmap.width / 2f - (window.width / 2 + pan.x) / s,
                                top = bitmap.height / 2f - (window.height / 2 + pan.y) / s,
                                width = window.width / s,
                                height = window.height / s,
                            )
                        },
                        loading = saving,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

