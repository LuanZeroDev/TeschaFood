package com.tescha.food.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 40.dp,
    val xxl: Dp = 64.dp,
    val gutter: Dp = 24.dp,
    val margin: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
