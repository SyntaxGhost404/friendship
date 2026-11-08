package app.nearfolk.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nearfolk.core.designsystem.component.NfPresencePip
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfArtTones
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.MediaRef
import coil3.compose.AsyncImage
import java.io.File
import kotlin.math.sin
import kotlin.random.Random

private val avatarTones = listOf(
    NfArtTones.moss, NfArtTones.pine, NfArtTones.olive, NfArtTones.clay, NfArtTones.umber,
    NfArtTones.fern, NfArtTones.sage, NfArtTones.sand, NfArtTones.straw, NfArtTones.lichen,
)

private fun initials(name: String): String =
    name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/**
 * Squircle avatar. Photos load from storage; members without one get a
 * monogram on an earthy tone picked from their seed or name.
 */
@Composable
fun MemberAvatar(
    name: String,
    media: MediaRef?,
    size: Dp,
    modifier: Modifier = Modifier,
    online: Boolean? = null,
    ringColor: Color = Nf.colors.canvas,
) {
    val description = stringResource(R.string.ui_avatar_of, name)
    Box(modifier.size(size).semantics { contentDescription = description }) {
        Box(Modifier.fillMaxSize().clip(NfShapes.avatar)) {
            when (media) {
                is MediaRef.Local, is MediaRef.Remote -> AsyncImage(
                    model = when (media) {
                        is MediaRef.Local -> File(media.path)
                        is MediaRef.Remote -> media.url
                        else -> null
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Nf.colors.surfaceRaised),
                )
                else -> Monogram(name, (media as? MediaRef.Generated)?.seed ?: name.hashCode().toLong(), size)
            }
        }
        if (online == true) {
            val pip = (size * 0.24f).coerceIn(10.dp, 18.dp)
            NfPresencePip(
                online = true,
                ringColor = ringColor,
                size = pip,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun Monogram(name: String, seed: Long, size: Dp) {
    val tone = remember(seed) { avatarTones[Random(seed).nextInt(avatarTones.size)] }
    val ink = if (tone.luminance() > 0.35f) NfArtTones.charcoal else NfArtTones.bone
    val fontSize = with(LocalDensity.current) { (size * 0.36f).toSp() }
    Box(Modifier.fillMaxSize().background(tone), contentAlignment = Alignment.Center) {
        Text(
            initials(name),
            color = ink,
            style = Nf.type.title.copy(fontSize = fontSize, lineHeight = fontSize, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        )
    }
}

/** Renders any [MediaRef] as a filled image. Generated refs draw [Artwork]. */
@Composable
fun MediaImage(ref: MediaRef, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    when (ref) {
        is MediaRef.Generated -> Artwork(ref.seed, modifier)
        is MediaRef.Local -> AsyncImage(
            model = File(ref.path),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier.background(Nf.colors.surfaceRaised),
        )
        is MediaRef.Remote -> AsyncImage(
            model = ref.url,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier.background(Nf.colors.surfaceRaised),
        )
    }
}

/**
 * Flat generative compositions used for sample content: layered fields,
 * squircle tiles, a low sun over a horizon, or striped terrain. Earth tones
 * only, no gradients or glow.
 */
@Composable
fun Artwork(seed: Long, modifier: Modifier = Modifier) {
    val plan = remember(seed) { ArtPlan.from(seed) }
    BoxWithConstraints(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) { plan.draw(this) }
    }
}

private class ArtPlan(
    val style: Int,
    val tones: List<Color>,
    val rng: Long,
) {
    fun draw(scope: DrawScope) = with(scope) {
        val r = Random(rng)
        drawRect(tones[0])
        when (style) {
            0 -> fields(r)
            1 -> tiles(r)
            2 -> sun(r)
            else -> stripes(r)
        }
    }

    private fun DrawScope.fields(r: Random) {
        val bands = 3 + r.nextInt(3)
        for (i in 0 until bands) {
            val baseY = size.height * (0.35f + i * (0.6f / bands))
            val amp = size.height * (0.03f + r.nextFloat() * 0.06f)
            val freq = 1f + r.nextFloat() * 2f
            val phase = r.nextFloat() * 6f
            val path = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                while (x <= size.width) {
                    lineTo(x, baseY + amp * sin(phase + freq * x / size.width * 6.28f))
                    x += size.width / 40f
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, tones[1 + i % (tones.size - 1)])
        }
    }

    private fun DrawScope.tiles(r: Random) {
        val cols = 3 + r.nextInt(3)
        val cell = size.width / cols
        val rows = (size.height / cell).toInt() + 1
        val gap = cell * 0.12f
        for (row in 0 until rows) for (col in 0 until cols) {
            if (r.nextFloat() < 0.18f) continue
            val tone = tones[1 + r.nextInt(tones.size - 1)]
            val outline = NfShapes.large.createOutline(Size(cell - gap, cell - gap), LayoutDirection.Ltr, this)
            translate(col * cell + gap / 2, row * cell + gap / 2) {
                drawOutlineShape(outline, tone)
            }
        }
    }

    private fun DrawScope.sun(r: Random) {
        val horizon = size.height * (0.55f + r.nextFloat() * 0.15f)
        val radius = size.minDimension * (0.14f + r.nextFloat() * 0.1f)
        val cx = size.width * (0.25f + r.nextFloat() * 0.5f)
        clipRect(bottom = horizon) {
            drawCircle(tones[2], radius, Offset(cx, horizon - radius * 0.35f))
        }
        drawRect(tones[1], topLeft = Offset(0f, horizon), size = Size(size.width, size.height - horizon))
        val lines = 2 + r.nextInt(3)
        for (i in 1..lines) {
            val y = horizon + (size.height - horizon) * i / (lines + 1)
            drawRect(tones[3 % tones.size], topLeft = Offset(0f, y), size = Size(size.width, size.height * 0.012f))
        }
    }

    private fun DrawScope.stripes(r: Random) {
        val angle = listOf(-30f, -15f, 15f, 30f)[r.nextInt(4)]
        val width = size.maxDimension / (6 + r.nextInt(6))
        rotate(angle) {
            var x = -size.maxDimension
            var i = 0
            while (x < size.maxDimension * 2) {
                if (i % 2 == 0) {
                    drawRect(tones[1 + (i / 2) % (tones.size - 1)], topLeft = Offset(x, -size.maxDimension), size = Size(width, size.maxDimension * 3))
                }
                x += width
                i++
            }
        }
    }

    private fun DrawScope.drawOutlineShape(outline: androidx.compose.ui.graphics.Outline, color: Color) {
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Generic -> drawPath(outline.path, color)
            is androidx.compose.ui.graphics.Outline.Rectangle -> drawRect(color, outline.rect.topLeft, outline.rect.size)
            is androidx.compose.ui.graphics.Outline.Rounded -> drawRect(color, Offset.Zero, Size(outline.roundRect.width, outline.roundRect.height))
        }
    }

    companion object {
        private val palettes = listOf(
            listOf(NfArtTones.bone, NfArtTones.sage, NfArtTones.moss, NfArtTones.pine),
            listOf(NfArtTones.sand, NfArtTones.straw, NfArtTones.olive, NfArtTones.umber),
            listOf(NfArtTones.charcoal, NfArtTones.moss, NfArtTones.straw, NfArtTones.lichen),
            listOf(NfArtTones.lichen, NfArtTones.fern, NfArtTones.pine, NfArtTones.charcoal),
            listOf(NfArtTones.bone, NfArtTones.clay, NfArtTones.umber, NfArtTones.sand),
            listOf(NfArtTones.pine, NfArtTones.sage, NfArtTones.bone, NfArtTones.straw),
        )

        fun from(seed: Long): ArtPlan {
            val r = Random(seed)
            return ArtPlan(style = r.nextInt(4), tones = palettes[r.nextInt(palettes.size)], rng = r.nextLong())
        }
    }
}
