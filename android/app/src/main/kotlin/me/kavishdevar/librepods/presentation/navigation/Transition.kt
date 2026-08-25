package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("No SharedTransitionScope provided") }

val LocalTransitionProgress = compositionLocalOf { 0f }
val LocalIsCurrentEntry = compositionLocalOf { false }


/*

 */
object DummySharedTransitionScope: SharedTransitionScope {
    override val isTransitionActive: Boolean
        get() = false

    override fun Modifier.skipToLookaheadSize(enabled: () -> Boolean): Modifier = this

    override fun Modifier.renderInSharedTransitionScopeOverlay(
        zIndexInOverlay: Float,
        renderInOverlay: () -> Boolean
    ): Modifier = this

    override fun Modifier.sharedElement(
        sharedContentState: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip
    ): Modifier = this

    override fun Modifier.sharedBounds(
        sharedContentState: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        enter: EnterTransition,
        exit: ExitTransition,
        boundsTransform: BoundsTransform,
        resizeMode: SharedTransitionScope.ResizeMode,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip
    ): Modifier = this

    override fun Modifier.sharedElementWithCallerManagedVisibility(
        sharedContentState: SharedTransitionScope.SharedContentState,
        visible: Boolean,
        boundsTransform: BoundsTransform,
        placeholderSize: SharedTransitionScope.PlaceholderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip
    ): Modifier = this

    override fun OverlayClip(clipShape: Shape): SharedTransitionScope.OverlayClip {
        return object : SharedTransitionScope.OverlayClip {
            override fun getClipPath(
                sharedContentState: SharedTransitionScope.SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density
            ): Path? {
                return null
            }

        }
    }

    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() = DummyLookaheadCoordinates

    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        return DummyLookaheadCoordinates
    }
}

object DummyLookaheadCoordinates: LayoutCoordinates {
    override val size: IntSize
        get() = IntSize(0, 0)
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = setOf()
    override val parentLayoutCoordinates: LayoutCoordinates?
        get() = null
    override val parentCoordinates: LayoutCoordinates?
        get() = null
    override val isAttached: Boolean
        get() = false

    override fun windowToLocal(relativeToWindow: Offset): Offset = Offset(0f, 0f)

    override fun localToWindow(relativeToLocal: Offset): Offset = Offset(0f, 0f)

    override fun localToRoot(relativeToLocal: Offset): Offset = Offset(0f, 0f)

    override fun localPositionOf(
        sourceCoordinates: LayoutCoordinates,
        relativeToSource: Offset
    ): Offset = Offset(0f, 0f)

    override fun localBoundingBoxOf(
        sourceCoordinates: LayoutCoordinates,
        clipBounds: Boolean
    ): Rect = Rect(Offset(0f, 0f), Offset(0f, 0f))

    override fun get(alignmentLine: AlignmentLine): Int = 0
}
