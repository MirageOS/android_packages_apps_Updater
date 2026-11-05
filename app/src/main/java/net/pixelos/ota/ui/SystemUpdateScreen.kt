/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package net.pixelos.ota.ui

import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsTheme
import net.pixelos.ota.R
import net.pixelos.ota.data.ChangelogState
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import net.pixelos.ota.updates.action.UpdateAction
import net.pixelos.ota.updates.action.UpdateActionType
import net.pixelos.ota.updates.state.ProgressState
import net.pixelos.ota.updates.state.UpdateItemState
import net.pixelos.ota.util.StringUtil
import java.util.Date

private val ContentMaxWidth = 560.dp
private val PagePadding = 16.dp
private val CardCorner = 28.dp
private val CardPadding = 20.dp
private val CardSpacing = 8.dp
private val TileMinHeight = 148.dp
private val ButtonHeight = 56.dp

/**
 * System update screen. Same visual language as the About phone / Personalise screens in
 * Settings: wallpaper banner on top, then rounded cards with a shape chip and a translucent shape
 * in the corner.
 */
@Composable
fun SystemUpdateScreen(
    headline: String,
    supportingText: String?,
    supportingTextIsError: Boolean = false,
    isBusy: Boolean,
    canCheckForUpdates: Boolean,
    showDeviceInfo: Boolean,
    lastCheckedTimestamp: Long,
    onBackClick: () -> Unit,
    onCheckClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
    updateItem: UpdateItemState? = null,
    changelogState: ChangelogState = ChangelogState.Idle,
    onUpdateAction: (UpdateAction) -> Unit = {},
) {
    // The bottom bar only offers a check while no update is displayed.
    val showCheckButton = !isBusy && updateItem == null

    val statusIcon = when {
        supportingTextIsError -> Icons.Outlined.ErrorOutline
        isBusy -> Icons.Outlined.Sync
        updateItem != null -> SystemUpdateIcon
        else -> Icons.Outlined.CheckCircle
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SystemUpdateTopBar(
                onBackClick = onBackClick,
                showCheckAction = !showCheckButton,
                canCheckForUpdates = canCheckForUpdates,
                onCheckClick = onCheckClick,
                onLocalUpdateClick = onLocalUpdateClick,
                onPreferencesClick = onPreferencesClick,
                updateOverflowActions = updateItem?.actions?.overflow ?: emptyList(),
                onUpdateAction = onUpdateAction,
            )
        },
        bottomBar = {
            if (showCheckButton) {
                CheckForUpdateButton(
                    enabled = canCheckForUpdates,
                    onClick = onCheckClick,
                )
            } else if (!isBusy && updateItem != null) {
                UpdateActionButtons(
                    item = updateItem,
                    onAction = onUpdateAction,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ContentMaxWidth)
                    .padding(horizontal = PagePadding, vertical = CardSpacing),
                verticalArrangement = Arrangement.spacedBy(CardSpacing),
            ) {
                UpdateBanner()

                StatusCard(
                    headline = headline,
                    supportingText = supportingText,
                    supportingTextIsError = supportingTextIsError,
                    icon = statusIcon,
                )

                if (isBusy) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = CardSpacing),
                        contentAlignment = Alignment.Center,
                    ) {
                        UpdateCheckAnimation()
                    }
                } else {
                    if (showDeviceInfo) {
                        DeviceInfoTiles(lastCheckedTimestamp = lastCheckedTimestamp)
                    }
                    updateItem?.let { item ->
                        UpdateDetails(item = item, changelogState = changelogState)
                    }
                }

                Spacer(modifier = Modifier.height(CardSpacing))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------------------------

/** Card surface shared by the Personalise and About screens in Settings. */
@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(CardCorner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .clip(shape),
        content = content,
    )
}

/** Big translucent shape that peeks out of the top end corner of a card. */
@Composable
private fun BoxScope.CornerShape() {
    Image(
        painter = painterResource(R.drawable.ic_shape_flower),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondaryContainer),
        alpha = 0.45f,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 30.dp, y = (-30).dp)
            .size(104.dp),
    )
}

/** Icon inside a cookie-shaped chip. */
@Composable
private fun ShapeChip(icon: ImageVector) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_shape_cookie),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun StatusCard(
    headline: String,
    supportingText: String?,
    supportingTextIsError: Boolean,
    icon: ImageVector,
) {
    SurfaceCard {
        CornerShape()
        Column(modifier = Modifier.padding(CardPadding)) {
            ShapeChip(icon)
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (supportingTextIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Square-ish card: chip on top, title and value at the bottom. */
@Composable
private fun InfoTile(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        CornerShape()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .heightIn(min = TileMinHeight)
                .padding(CardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ShapeChip(icon)
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoTiles(lastCheckedTimestamp: Long) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(context, configuration.locales) { StringUtil.getCurrentLocale(context) }
    val securityPatch = remember(locale) {
        runCatching {
            StringUtil.formatSecurityPatchFullDate(context, DeviceInfoUtils.buildSecurityPatch)
        }.getOrDefault(DeviceInfoUtils.buildSecurityPatch)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(CardSpacing),
    ) {
        InfoTile(
            icon = Icons.Outlined.Android,
            title = stringResource(R.string.android_version_title),
            summary = DeviceInfoUtils.androidVersion,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        InfoTile(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.security_update),
            summary = securityPatch,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }

    if (lastCheckedTimestamp > 0) {
        LastCheckedCard(timestampMillis = lastCheckedTimestamp)
    }
}

@Composable
private fun LastCheckedCard(timestampMillis: Long) {
    val context = LocalContext.current
    val dateText = remember(timestampMillis) {
        val date = DateUtils.formatDateTime(
            context,
            timestampMillis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_NO_YEAR,
        )
        val time = DateFormat.getTimeFormat(context).format(Date(timestampMillis))
        "$date ($time)"
    }

    SurfaceCard {
        CornerShape()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShapeChip(Icons.Outlined.Schedule)
            Column {
                Text(
                    text = stringResource(R.string.last_checked_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Update details
// ---------------------------------------------------------------------------------------------

@Composable
private fun UpdateDetails(
    item: UpdateItemState,
    changelogState: ChangelogState,
) {
    val showsInstallInfo = item.progress == null &&
            item.actions.primary.type in setOf(
        UpdateActionType.START_DOWNLOAD,
        UpdateActionType.START_INSTALL,
    )

    SurfaceCard {
        CornerShape()
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                text = "${item.buildVersion} - ${item.buildDate}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showsInstallInfo && item.fileSize.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.list_update_size, item.fileSize),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item.progress?.let { progress ->
                UpdateProgress(progress)
            }
        }
    }

    SurfaceCard {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                text = stringResource(R.string.changelog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (changelogState) {
                ChangelogState.Idle -> Unit
                ChangelogState.Loading -> Text(
                    text = stringResource(R.string.changelog_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                ChangelogState.Error -> Text(
                    text = stringResource(R.string.changelog_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
                is ChangelogState.Loaded -> if (changelogState.markdown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.changelog_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    MarkdownText(
                        markdown = changelogState.markdown,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }

    if (showsInstallInfo) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = CardSpacing)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.metered_warning_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.UpdateProgress(progress: ProgressState) {
    when (progress) {
        is ProgressState.Determinate -> {
            LinearWavyProgressIndicator(
                progress = { progress.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
            val caption = listOf(progress.downloadedSize, progress.eta)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
            if (caption.isNotEmpty()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                )
            }
        }

        ProgressState.Indeterminate -> {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
            Text(
                text = stringResource(R.string.downloading_installing_caption),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun UpdateActionButtons(
    item: UpdateItemState,
    onAction: (UpdateAction) -> Unit,
) {
    val context = LocalContext.current
    val primary = item.actions.primary
    val primaryLabel = when (primary.type) {
        UpdateActionType.START_DOWNLOAD -> stringResource(R.string.action_download_install)
        else -> primary.type.title(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item.actions.secondary?.let { secondary ->
            TextButton(
                onClick = { onAction(secondary) },
                enabled = secondary.enabled,
            ) {
                Text(secondary.type.title(context))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(
            onClick = { onAction(primary) },
            enabled = primary.enabled,
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .height(ButtonHeight),
            shape = RoundedCornerShape(ButtonHeight / 2),
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SystemUpdateTopBar(
    onBackClick: () -> Unit,
    showCheckAction: Boolean,
    canCheckForUpdates: Boolean,
    onCheckClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    updateOverflowActions: List<UpdateAction> = emptyList(),
    onUpdateAction: (UpdateAction) -> Unit = {},
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PagePadding, vertical = 8.dp),
    ) {
        FilledTonalIconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            FilledTonalIconButton(
                onClick = { menuExpanded = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.menu_more_options),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (showCheckAction) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.check_for_update)) },
                        enabled = canCheckForUpdates,
                        onClick = {
                            menuExpanded = false
                            onCheckClick()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.local_update_import)) },
                    onClick = {
                        menuExpanded = false
                        onLocalUpdateClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_preferences)) },
                    onClick = {
                        menuExpanded = false
                        onPreferencesClick()
                    },
                )
                val reportIssueUrl = stringResource(R.string.report_issue_url)
                if (reportIssueUrl.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_issues)) },
                        onClick = {
                            menuExpanded = false
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, reportIssueUrl.toUri())
                            )
                        },
                    )
                }
                updateOverflowActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.type.title(context)) },
                        enabled = action.enabled,
                        onClick = {
                            menuExpanded = false
                            onUpdateAction(action)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckForUpdateButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .height(ButtonHeight),
            shape = RoundedCornerShape(ButtonHeight / 2),
        ) {
            Text(
                text = stringResource(R.string.check_for_update),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenUpToDatePreview() {
    SettingsTheme {
        SystemUpdateScreen(
            headline = "Your system is up to date",
            supportingText = null,
            isBusy = false,
            canCheckForUpdates = true,
            showDeviceInfo = false,
            lastCheckedTimestamp = 1_754_000_000_000L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenCheckingPreview() {
    SettingsTheme {
        SystemUpdateScreen(
            headline = "Checking for update…",
            supportingText = null,
            isBusy = true,
            canCheckForUpdates = false,
            showDeviceInfo = false,
            lastCheckedTimestamp = 0L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}
