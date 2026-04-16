package com.example.archshowcase.presentation.timetravel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.timetravel.TimeTravelState
import com.example.archshowcase.core.compose.selectAsState
import com.example.archshowcase.core.isAndroidPlatform
import com.example.archshowcase.core.scheduler.oboLaunch
import com.example.archshowcase.core.trace.export.ExportResult
import com.example.archshowcase.core.trace.export.ImportResult
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTextButton
import com.example.archshowcase.presentation.theme.AppTheme
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock

private const val OBO_TAG = "TimeTravel"

@Composable
fun TimeTravelFloatingPanel(
    component: TimeTravelComponent,
    modifier: Modifier = Modifier,
) {
    val isInPlaybackMode by component.timeTravelState.selectAsState { mode == TimeTravelState.Mode.STOPPED }
    val selectedEventIndex by component.timeTravelState.selectAsState { selectedEventIndex }
    val eventsCount by component.timeTravelState.selectAsState { events.size }
    val currentIntentInfo by component.timeTravelState.selectAsState {
        val idx = selectedEventIndex
        if (idx < 0) return@selectAsState null
        // 往前找最近的 INTENT 事件
        val intentEvent = (idx downTo 0).firstNotNullOfOrNull { i ->
            events.getOrNull(i)?.takeIf { it.type == com.arkivanov.mvikotlin.core.store.StoreEventType.INTENT }
        } ?: return@selectAsState null
        val store = intentEvent.storeName.substringBefore(':')
        val intent = intentEvent.value?.let { it::class.simpleName } ?: ""
        "$store · $intent"
    }
    val vfState by component.vfExportState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingExportData by remember { mutableStateOf<ByteArray?>(null) }
    var pendingVfFiles by remember { mutableStateOf<Map<String, ByteArray>?>(null) }
    val scope = rememberCoroutineScope()

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var dragMinX by remember { mutableStateOf(0f) }
    var dragMaxX by remember { mutableStateOf(0f) }
    var dragMinY by remember { mutableStateOf(0f) }
    var dragMaxY by remember { mutableStateOf(0f) }

    val bookmarkMsg by component.bookmarkMessage.collectAsState()
    LaunchedEffect(bookmarkMsg) {
        bookmarkMsg?.let { msg ->
            errorMessage = msg
            component.onBookmarkMessageConsumed()
        }
    }

    val vfSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { file ->
        file?.let {
            val files = pendingVfFiles ?: return@let
            scope.oboLaunch(OBO_TAG) {
                // VF 打包为 zip：包含 manifest.json + start.tte + end.tte
                val zipBytes = packVfToZip(files)
                it.write(zipBytes)
            }
            pendingVfFiles = null
        }
    }

    val fileSaverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault()
    ) { file ->
        file?.let {
            val data = pendingExportData ?: return@let
            scope.oboLaunch(OBO_TAG) { it.write(data) }
            pendingExportData = null
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = null),
        mode = FileKitMode.Single
    ) { file ->
        file?.let {
            scope.oboLaunch(OBO_TAG) {
                val bytes = it.readBytes()
                when (val result = component.onImport(bytes)) {
                    is ImportResult.Success -> {
                        errorMessage = null
                        expanded = true
                    }
                    is ImportResult.Error -> {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                var root: LayoutCoordinates? = coords
                while (root?.parentLayoutCoordinates != null) root = root.parentLayoutCoordinates
                val rs = root?.size ?: return@onGloballyPositioned
                val baseX = pos.x - offsetX
                val baseY = pos.y - offsetY
                dragMinX = -baseX
                dragMaxX = (rs.width - coords.size.width).coerceAtLeast(0).toFloat() - baseX
                dragMinY = -baseY
                dragMaxY = (rs.height - coords.size.height).coerceAtLeast(0).toFloat() - baseY
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppTheme.colors.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppText(
                        error,
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colors.onErrorContainer,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    AppTextButton(onClick = { errorMessage = null }) {
                        AppText("×", color = AppTheme.colors.onErrorContainer)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.surface)
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isInPlaybackMode) {
                        currentIntentInfo?.let { info ->
                            AppText(
                                info,
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppText(
                            if (isInPlaybackMode) "回放 (${selectedEventIndex + 1}/${eventsCount})"
                            else "TimeTravel",
                            style = AppTheme.typography.labelMedium
                        )
                        if (isInPlaybackMode) {
                            AppTextButton(onClick = component::onCancel) {
                                AppText("退出", color = AppTheme.colors.error)
                            }
                        }
                    }

                    if (isInPlaybackMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                        ) {
                            AppTextButton(
                                onClick = component::onMoveToStart,
                                enabled = selectedEventIndex >= 0
                            ) {
                                AppText("|<")
                            }
                            AppTextButton(
                                onClick = component::onStepBackward,
                                enabled = selectedEventIndex >= 0
                            ) {
                                AppText("<")
                            }
                            AppTextButton(
                                onClick = component::onStepForward,
                                enabled = selectedEventIndex < eventsCount - 1
                            ) {
                                AppText(">")
                            }
                            AppTextButton(
                                onClick = component::onMoveToEnd,
                                enabled = selectedEventIndex < eventsCount - 1
                            ) {
                                AppText(">|")
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextButton(
                                onClick = {
                                    when (val result = component.onExport()) {
                                        is ExportResult.Success -> {
                                            val fileName = "timetravel_${formatExportTime()}"
                                            pendingExportData = result.data
                                            fileSaverLauncher.launch(
                                                fileName,
                                                result.extension
                                            )
                                            errorMessage = null
                                        }
                                        is ExportResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                }
                            ) {
                                AppText("导出")
                            }
                            AppTextButton(
                                onClick = { filePickerLauncher.launch() }
                            ) {
                                AppText("导入")
                            }
                            if (isAndroidPlatform) {
                                val hasBookmark by component.bookmarkExists.collectAsState()
                                AppTextButton(
                                    onClick = { component.onBookmarkToggle() }
                                ) {
                                    AppText(
                                        if (hasBookmark) "清除书签" else "保存书签",
                                        color = if (hasBookmark) AppTheme.colors.error
                                                else AppTheme.colors.primary
                                    )
                                }
                            }
                        }

                        // VF 录制区域（仅 Android）
                        if (isAndroidPlatform) {
                            when (vfState) {
                                is VfExportState.Idle -> {
                                    AppTextButton(onClick = {
                                        // 收起面板 → 等一帧渲染 → 截图 → 开始录制
                                        expanded = false
                                        scope.oboLaunch(OBO_TAG) {
                                            kotlinx.coroutines.delay(300)
                                            val screenshot = com.example.archshowcase.core.util.ScreenshotCapture.capture()
                                            component.onExportVfStart(screenshot)
                                        }
                                    }) {
                                        AppText("录制开始")
                                    }
                                }
                                is VfExportState.Recording -> {
                                    AppTextButton(
                                        onClick = {
                                            // 收起面板 → 等一帧渲染 → 截图 → 保存 VF
                                            expanded = false
                                            scope.oboLaunch(OBO_TAG) {
                                                kotlinx.coroutines.delay(300)
                                                val endScreenshot = com.example.archshowcase.core.util.ScreenshotCapture.capture()
                                                val files = component.onExportVfEnd("", endScreenshot)
                                                if (files != null) {
                                                    pendingVfFiles = files
                                                    vfSaverLauncher.launch(
                                                        "vf_${formatExportTime()}",
                                                        "zip"
                                                    )
                                                } else {
                                                    errorMessage = "录制导出失败"
                                                }
                                            }
                                        }
                                    ) {
                                        AppText("录制结束")
                                    }
                                }
                                is VfExportState.WaitingForText -> Unit
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isInPlaybackMode) AppTheme.colors.tertiaryContainer
                    else AppTheme.colors.primaryContainer
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(dragMinX, dragMaxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(dragMinY, dragMaxY)
                    }
                }
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = if (expanded) "×" else if (isInPlaybackMode) "▶" else "TT",
                style = AppTheme.typography.titleMedium,
                color = if (isInPlaybackMode) AppTheme.colors.onTertiaryContainer
                else AppTheme.colors.onPrimaryContainer,
            )
        }
    }
}

/**
 * 纯 Kotlin 实现的 ZIP 打包（Stored 模式，不压缩）
 *
 * 生成标准 ZIP 格式，兼容所有解压工具。
 * 仅用于 VF 导出（manifest.json + .tte + .png 等小文件），无需压缩。
 */
internal fun packVfToZip(files: Map<String, ByteArray>): ByteArray {
    val buf = MutableZipBuffer()
    val centralEntries = mutableListOf<ByteArray>()
    var offset = 0

    files.forEach { (name, data) ->
        val nameBytes = name.encodeToByteArray()
        val crc = crc32(data)

        // Local file header
        val local = buildLocalHeader(nameBytes, data.size, crc)
        buf.write(local)
        buf.write(data)

        // 记录 central directory entry
        centralEntries.add(buildCentralEntry(nameBytes, data.size, crc, offset))
        offset += local.size + data.size
    }

    // Central directory
    val centralStart = offset
    var centralSize = 0
    centralEntries.forEach { entry ->
        buf.write(entry)
        centralSize += entry.size
    }

    // End of central directory
    buf.write(buildEndRecord(files.size, centralSize, centralStart))

    return buf.toByteArray()
}

// ── ZIP 格式辅助 ──────────────────────────────────────────

private class MutableZipBuffer {
    private val chunks = mutableListOf<ByteArray>()
    private var totalSize = 0

    fun write(data: ByteArray) { chunks.add(data); totalSize += data.size }

    fun toByteArray(): ByteArray {
        val result = ByteArray(totalSize)
        var pos = 0
        chunks.forEach { chunk -> chunk.copyInto(result, pos); pos += chunk.size }
        return result
    }
}

private fun buildLocalHeader(name: ByteArray, size: Int, crc: Int): ByteArray {
    val header = ByteArray(30 + name.size)
    // Signature: PK\x03\x04
    header.putInt32LE(0, 0x04034b50.toInt())
    // Version needed: 20
    header.putInt16LE(4, 20)
    // Flags: 0, Method: 0 (stored)
    // CRC-32
    header.putInt32LE(14, crc)
    // Compressed size = Uncompressed size (stored)
    header.putInt32LE(18, size)
    header.putInt32LE(22, size)
    // File name length
    header.putInt16LE(26, name.size)
    // Extra field length: 0
    // File name
    name.copyInto(header, 30)
    return header
}

private fun buildCentralEntry(name: ByteArray, size: Int, crc: Int, localOffset: Int): ByteArray {
    val entry = ByteArray(46 + name.size)
    // Signature: PK\x01\x02
    entry.putInt32LE(0, 0x02014b50.toInt())
    // Version made by: 20, Version needed: 20
    entry.putInt16LE(4, 20)
    entry.putInt16LE(6, 20)
    // CRC-32
    entry.putInt32LE(16, crc)
    // Sizes
    entry.putInt32LE(20, size)
    entry.putInt32LE(24, size)
    // File name length
    entry.putInt16LE(28, name.size)
    // Relative offset of local header
    entry.putInt32LE(42, localOffset)
    // File name
    name.copyInto(entry, 46)
    return entry
}

private fun buildEndRecord(entryCount: Int, centralSize: Int, centralOffset: Int): ByteArray {
    val record = ByteArray(22)
    // Signature: PK\x05\x06
    record.putInt32LE(0, 0x06054b50.toInt())
    // Entries on this disk / total entries
    record.putInt16LE(8, entryCount)
    record.putInt16LE(10, entryCount)
    // Central directory size / offset
    record.putInt32LE(12, centralSize)
    record.putInt32LE(16, centralOffset)
    return record
}

private fun ByteArray.putInt16LE(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = (value shr 8 and 0xFF).toByte()
}

private fun ByteArray.putInt32LE(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = (value shr 8 and 0xFF).toByte()
    this[offset + 2] = (value shr 16 and 0xFF).toByte()
    this[offset + 3] = (value shr 24 and 0xFF).toByte()
}

/** 标准 CRC-32（ISO 3309） */
private fun crc32(data: ByteArray): Int {
    var crc = -1 // 0xFFFFFFFF
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        repeat(8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
        }
    }
    return crc xor -1
}

private fun formatExportTime(): String {
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val instant = Instant.fromEpochMilliseconds(nowMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.year}${localDateTime.monthNumber.toString().padStart(2, '0')}${localDateTime.dayOfMonth.toString().padStart(2, '0')}_" +
            "${localDateTime.hour.toString().padStart(2, '0')}${localDateTime.minute.toString().padStart(2, '0')}${localDateTime.second.toString().padStart(2, '0')}"
}
