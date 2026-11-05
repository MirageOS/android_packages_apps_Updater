/*
 * SPDX-FileCopyrightText: Material Design Authors / Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package net.pixelos.ota.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private val SystemUpdatePathData = addPathNodes(
    "M17,1.01L7,1C5.9,1 5,1.9 5,3v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3C19,1.9 " +
        "18.1,1.01 17,1.01zM17,21H7v-1h10V21zM17,18H7V6h10V18zM7,4V3h10v1H7zM16,12l-4,4l-4," +
        "-4l1.41,-1.41L11,12.17V8h2v4.17l1.59,-1.59L16,12z",
)

internal val SystemUpdateIcon: ImageVector = ImageVector.Builder(
    name = "SystemUpdate",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = SystemUpdatePathData,
    fill = SolidColor(Color.Black),
).build()

// Same paths as ic_about_verified / ic_about_unofficial in Settings.
private const val SealPath =
    "M23,12l-2.44,-2.79 0.34,-3.69 -3.61,-0.82 -1.89,-3.2L12,2.96 8.6,1.5 6.71,4.69 3.1,5.5 " +
        "3.44,9.2 1,12l2.44,2.79 -0.34,3.7 3.61,0.82L8.6,22.5l3.4,-1.47 3.4,1.46 1.89,-3.19 " +
        "3.61,-0.82 -0.34,-3.69L23,12z"

/** Seal with a check mark: official build. */
internal val VerifiedIcon: ImageVector = ImageVector.Builder(
    name = "Verified",
    defaultWidth = 18.dp,
    defaultHeight = 18.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        SealPath + "M10.09,16.72l-3.8,-3.81 1.48,-1.48 2.32,2.33 5.85,-5.87 1.48,1.48 -7.33,7.35z",
    ),
    fill = SolidColor(Color.Black),
).build()

/** Same seal with an exclamation mark cut out: unofficial build. */
internal val UnofficialIcon: ImageVector = ImageVector.Builder(
    name = "Unofficial",
    defaultWidth = 18.dp,
    defaultHeight = 18.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(SealPath + "M11,6.5h2v7h-2z" + "M12,15a1.25,1.25 0 1,0 0.001,0z"),
    fill = SolidColor(Color.Black),
    pathFillType = PathFillType.EvenOdd,
).build()
