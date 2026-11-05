/*
 * SPDX-FileCopyrightText: 2026 The MirageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package net.pixelos.ota.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pixelos.ota.R
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val TAG = "UpdateBanner"

private val BannerHeight = 200.dp
private val BannerCorner = 28.dp
private val WallpaperBlur = 28.dp

/** Page padding the banner sits in; only used to pick the aspect ratio of the wallpaper crop. */
private const val PAGE_PADDING_DP = 16
private const val BANNER_HEIGHT_DP = 200

private const val SAMPLE_WIDTH_PX = 720

/** Share of the banner, from the left and from the bottom, that holds the text. */
private const val TEXT_AREA_WIDTH = 0.75f
private const val TEXT_AREA_HEIGHT = 0.45f
private const val PERCENTILE = 0.85f

/** Relative luminance a background can have for white text to reach 4.5:1. */
private const val MAX_BACKGROUND_LUMINANCE = 1.05f / 4.5f - 0.05f
private const val MIN_SCRIM_ALPHA = 0.35f
private const val MAX_SCRIM_ALPHA = 0.85f

private val TextShadow = Shadow(
    color = Color(0x99000000),
    offset = Offset(0f, 4f),
    blurRadius = 16f,
)

@Immutable
private class BannerWallpaper(val image: ImageBitmap, val scrimAlpha: Float)

/**
 * Banner at the top of the updater: the current wallpaper, blurred, under a soft scrim that is
 * darkest at the bottom, with the ROM name and, in a thinner weight, the maintainer next to the
 * official / unofficial badge.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val wallpaper by rememberBannerWallpaper()
    val colors = MaterialTheme.colorScheme

    // Used until the wallpaper is loaded, or when it cannot be read. Darkened so the white text
    // keeps its contrast in light and dark themes.
    val fallback = Brush.linearGradient(
        listOf(
            lerp(colors.primary, Color.Black, 0.55f),
            lerp(colors.secondary, Color.Black, 0.55f),
        ),
    )
    val scrimAlpha = wallpaper?.scrimAlpha ?: MIN_SCRIM_ALPHA
    val scrim = remember(scrimAlpha) {
        val bottom = min(MAX_SCRIM_ALPHA, scrimAlpha * 1.1f)
        // Soft: almost clear at the top, easing into the dark area behind the text.
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = bottom * 0.15f),
                Color.Black.copy(alpha = bottom * 0.8f),
                Color.Black.copy(alpha = bottom),
            ),
        )
    }

    val officialLabel = stringResource(
        if (DeviceInfoUtils.isOfficial) R.string.build_official else R.string.build_unofficial,
    )
    val maintainer = DeviceInfoUtils.maintainer.ifEmpty { stringResource(R.string.maintainer_unknown) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BannerHeight)
            .clip(RoundedCornerShape(BannerCorner))
            .background(fallback),
    ) {
        wallpaper?.let {
            Image(
                bitmap = it.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(WallpaperBlur, BlurredEdgeTreatment.Unbounded),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim),
        )

        // Same translucent shapes as the Settings banner.
        Image(
            painter = painterResource(R.drawable.ic_shape_flower),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            alpha = 0.16f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = (-36).dp)
                .size(120.dp),
        )
        Image(
            painter = painterResource(R.drawable.ic_shape_cookie),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            alpha = 0.12f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 64.dp)
                .size(56.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
        ) {
            Text(
                text = stringResource(R.string.brand_name),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = TextShadow,
                ),
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = maintainer,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        shadow = TextShadow,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = if (DeviceInfoUtils.isOfficial) VerifiedIcon else UnofficialIcon,
                    contentDescription = officialLabel,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun rememberBannerWallpaper(): State<BannerWallpaper?> {
    val context = LocalContext.current.applicationContext
    return produceState<BannerWallpaper?>(null, context) {
        value = withContext(Dispatchers.IO) { loadBannerWallpaper(context) }
    }
}

/**
 * Loads the current wallpaper, center-cropped to the banner's aspect ratio and scaled down (it is
 * blurred anyway), and measures how strong the scrim has to be. Returns null if the wallpaper
 * cannot be read (missing permission, live wallpaper, ...); the gradient stays in that case.
 */
private fun loadBannerWallpaper(context: Context): BannerWallpaper? {
    return try {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported) return null
        val drawable = manager.drawable ?: return null
        val dw = drawable.intrinsicWidth
        val dh = drawable.intrinsicHeight
        if (dw <= 0 || dh <= 0) return null

        val metrics = context.resources.displayMetrics
        val bannerWidth = max(1f, metrics.widthPixels - 2 * PAGE_PADDING_DP * metrics.density)
        val bannerHeight = BANNER_HEIGHT_DP * metrics.density
        val sw = SAMPLE_WIDTH_PX
        val sh = max(1, (sw * (bannerHeight / bannerWidth)).roundToInt())

        // Center crop, like ContentScale.Crop. Drawn straight into the small bitmap so the
        // full-size wallpaper is never copied.
        val bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val scale = max(sw / dw.toFloat(), sh / dh.toFloat())
        val canvas = Canvas(bitmap)
        canvas.translate((sw - dw * scale) / 2f, (sh - dh * scale) / 2f)
        canvas.scale(scale, scale)
        drawable.setBounds(0, 0, dw, dh)
        drawable.draw(canvas)

        BannerWallpaper(bitmap.asImageBitmap(), scrimAlpha(bitmap))
    } catch (e: RuntimeException) {
        Log.w(TAG, "Unable to load wallpaper", e)
        null
    } catch (e: OutOfMemoryError) {
        Log.w(TAG, "Unable to load wallpaper", e)
        null
    }
}

/** Smallest scrim alpha that keeps white text readable over the text area of the bitmap. */
private fun scrimAlpha(bitmap: Bitmap): Float {
    val w = bitmap.width
    val h = bitmap.height
    val x1 = (w * TEXT_AREA_WIDTH).roundToInt()
    val y0 = (h * (1f - TEXT_AREA_HEIGHT)).roundToInt()
    val step = 4
    val values = ArrayList<Float>()
    var y = y0
    while (y < h) {
        var x = 0
        while (x < x1) {
            values.add(luminance(bitmap.getPixel(x, y)))
            x += step
        }
        y += step
    }
    if (values.isEmpty()) return MAX_SCRIM_ALPHA
    values.sort()
    val bright = values[min(values.size - 1, (values.size * PERCENTILE).toInt())]
    if (bright <= MAX_BACKGROUND_LUMINANCE) return MIN_SCRIM_ALPHA
    val needed = 1f - MAX_BACKGROUND_LUMINANCE / bright
    return max(MIN_SCRIM_ALPHA, min(MAX_SCRIM_ALPHA, needed))
}

/** Relative luminance (WCAG) of an sRGB pixel. */
private fun luminance(color: Int): Float =
    0.2126f * linear(AndroidColor.red(color)) +
            0.7152f * linear(AndroidColor.green(color)) +
            0.0722f * linear(AndroidColor.blue(color))

private fun linear(channel: Int): Float {
    val c = channel / 255f
    return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
}
