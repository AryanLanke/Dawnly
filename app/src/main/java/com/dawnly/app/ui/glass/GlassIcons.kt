package com.dawnly.app.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The few line icons Dawnly needs, drawn here so no icon library is required. Tint when drawing. */
object GlassIcons {
    val Check = icon("Check", stroke = "M5.5,12.5 L10,17 L18.5,7.5", strokeWidth = 2.4f)

    val Minus = icon("Minus", stroke = "M6,12 H18")

    val Plus = icon("Plus", stroke = "M12,6 V18 M6,12 H18")

    val Bell = icon(
        "Bell",
        stroke = "M6.5,16.5 V11 a5.5,5.5 0 0 1 11,0 V16.5 L19,18 H5 Z M10,20.5 a2,2 0 0 0 4,0",
    )

    val AlarmClock = icon(
        "AlarmClock",
        stroke = "M12,5.5 a7.5,7.5 0 1 1 0,15 a7.5,7.5 0 1 1 0,-15 Z M12,9.5 V13 L14.5,14.5 " +
            "M4,6.5 L7,4 M20,6.5 L17,4",
    )

    val Steps = icon(
        "Steps",
        stroke = "M8.5,3.5 c1.9,0 2.9,1.9 2.9,4.4 c0,2.1 -0.9,3.6 -2.9,3.6 s-2.9,-1.5 -2.9,-3.6 " +
            "C5.6,5.4 6.6,3.5 8.5,3.5 Z M6.2,14.5 h4.6 v1.4 a2.3,2.3 0 0 1 -4.6,0 Z " +
            "M15.5,7.5 c1.9,0 2.9,1.9 2.9,4.4 c0,2.1 -0.9,3.6 -2.9,3.6 s-2.9,-1.5 -2.9,-3.6 " +
            "C12.6,9.4 13.6,7.5 15.5,7.5 Z M13.2,18.5 h4.6 v0.6 a2.3,2.3 0 0 1 -4.6,0 Z",
    )

    val Battery = icon(
        "Battery",
        stroke = "M4.5,8 h12 a2,2 0 0 1 2,2 v4 a2,2 0 0 1 -2,2 h-12 a2,2 0 0 1 -2,-2 v-4 " +
            "a2,2 0 0 1 2,-2 Z M21,10.5 v3",
        fill = "M5.5,10.5 h7 v3 h-7 Z",
    )

    val Phone = icon(
        "Phone",
        stroke = "M8.5,3 h7 a2,2 0 0 1 2,2 v14 a2,2 0 0 1 -2,2 h-7 a2,2 0 0 1 -2,-2 v-14 " +
            "a2,2 0 0 1 2,-2 Z M11,18 h2",
    )

    private fun icon(name: String, stroke: String, fill: String? = null, strokeWidth: Float = 1.8f) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = addPathNodes(stroke),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            if (fill != null) addPath(pathData = addPathNodes(fill), fill = SolidColor(Color.Black))
        }.build()
}
