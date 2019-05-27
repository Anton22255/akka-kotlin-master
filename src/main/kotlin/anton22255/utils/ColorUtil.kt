package anton22255.utils

import java.awt.Color


fun transitionOfHueRange(percentage: Double, startHue: Int, endHue: Int): Color {
    // From 'startHue' 'percentage'-many to 'endHue'
    // Finally map from [0°, 360°] -> [0, 1.0] by dividing
    val hue = (percentage * (endHue - startHue) + startHue) / 360

    val saturation = 1.0
    val lightness = 0.5

    // Get the color
    return hslColorToRgb(hue, saturation, lightness)
}

fun hslColorToRgb(hue: Double, saturation: Double, lightness: Double): Color {
    if (saturation == 0.0) {
        // The color is achromatic (has no color)
        // Thus use its lightness for a grey-scale color
        val grey = percToColor(lightness)
        return Color(grey, grey, grey)
    }

    val q: Double
    if (lightness < 0.5) {
        q = lightness * (1 + saturation)
    } else {
        q = lightness + saturation - lightness * saturation
    }
    val p = 2 * lightness - q

    val oneThird = 1.0 / 3
    val red = percToColor(hueToRgb(p, q, hue + oneThird))
    val green = percToColor(hueToRgb(p, q, hue))
    val blue = percToColor(hueToRgb(p, q, hue - oneThird))

    return Color(red, green, blue)
}

fun percToColor(percentage: Double): Int {
    return Math.round(percentage * 255).toInt()
}

fun hueToRgb(p: Double, q: Double, t: Double): Double {
    var t = t
    if (t < 0) {
        t += 1.0
    }
    if (t > 1) {
        t -= 1.0
    }

    if (t < 1.0 / 6) {
        return p + (q - p) * 6.0 * t
    }
    if (t < 1.0 / 2) {
        return q
    }
    return if (t < 2.0 / 3) {
        p + (q - p) * (2.0 / 3 - t) * 6.0
    } else p
}
