/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.presentation.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import plus.yumeyuka.yumebox.common.AppConstants

object NavigationTransitions {
    
    private val enterEasing = LinearOutSlowInEasing
    private val exitEasing = FastOutLinearInEasing
    
    private fun createEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = AppConstants.Animation.ENTER_DURATION_MS,
                delayMillis = AppConstants.Animation.EXIT_DURATION_MS,
                easing = enterEasing,
            ),
        ) + scaleIn(
            initialScale = AppConstants.Animation.SCALE_FRACTION,
            animationSpec = tween(
                durationMillis = AppConstants.Animation.ENTER_DURATION_MS,
                delayMillis = AppConstants.Animation.EXIT_DURATION_MS,
                easing = enterEasing,
            ),
        )
    }
    
    private fun createExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = AppConstants.Animation.EXIT_DURATION_MS,
                easing = exitEasing,
            ),
        ) + scaleOut(
            targetScale = AppConstants.Animation.SCALE_FRACTION,
            animationSpec = tween(
                durationMillis = AppConstants.Animation.EXIT_DURATION_MS,
                easing = exitEasing,
            ),
        )
    }
    
    val defaultStyle = object : NavHostAnimatedDestinationStyle() {
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            createEnterTransition()
        }
        
        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            createExitTransition()
        }
        
        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            createEnterTransition()
        }
        
        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            createExitTransition()
        }
    }
}
