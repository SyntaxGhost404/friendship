package app.nearfolk.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Spring presets. Interactive elements settle quickly with a hint of
 * overshoot, layout changes settle without bounce.
 */
object NfMotion {
    /** Press feedback and toggles. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.62f, stiffness = 700f)

    /** Indicators that travel, such as the tab selection squircle. */
    fun <T> glide(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.78f, stiffness = 420f)

    /** Content entering or leaving. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)

    val slide: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = 0.9f, stiffness = 420f)
}
