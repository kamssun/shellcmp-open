package com.example.archshowcase.presentation.chat.room

import androidx.compose.foundation.background
import com.example.archshowcase.core.analytics.appClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.core.compose.exposure.ExposureLazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.example.archshowcase.core.compose.OBOLaunchedEffect
import com.example.archshowcase.core.trace.scroll.ScrollRestoreEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.example.archshowcase.chat.model.ChatMessage
import com.example.archshowcase.chat.model.MessageBody
import com.example.archshowcase.chat.model.WindowAnchor
import com.example.archshowcase.i18n.tr
import com.example.archshowcase.presentation.chat.widget.ChatInputBar
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.chat.widget.EmojiPanel
import com.example.archshowcase.presentation.chat.widget.MessageActionPopup
import com.example.archshowcase.presentation.chat.widget.MessageBubble
import com.example.archshowcase.presentation.chat.widget.PlusPanel
import com.example.archshowcase.presentation.chat.widget.TimeSeparator
import com.example.archshowcase.presentation.chat.widget.shouldShowTimeSeparator
import com.example.archshowcase.presentation.component.AppText
import com.example.archshowcase.presentation.component.AppTopBar
import com.example.archshowcase.presentation.theme.AppTheme
import com.example.archshowcase.resources.Res
import com.example.archshowcase.resources.chat_new_message_overflow
import com.example.archshowcase.resources.chat_no_more_history
import com.example.archshowcase.resources.chat_recall_other
import com.example.archshowcase.resources.chat_recall_self
import com.example.archshowcase.resources.chat_typing

@OptIn(FlowPreview::class)
@Composable
fun ChatRoomContent(
    component: ChatRoomComponent,
    modifier: Modifier = Modifier
) {
    val state = component.state.rememberFields()
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Top bar
        ChatTopBar(
            title = state.conversationName,
            memberCount = state.memberCount,
            typingUser = state.typingUser,
            onBack = component::onBack
        )

        // Message list — reverseLayout naturally positions at bottom, no scroll needed
        val listState = rememberLazyListState()
        val messages = state.messages
        // Scroll restore for time-travel replay
        ScrollRestoreEffect(
            listState = listState,
            scrollRestoreEvent = component.scrollRestoreEvent
        )

        // ── Stick-to-bottom：窗口化后由 WindowAnchor 控制 ──
        var stickToBottom by remember { mutableStateOf(true) }

        val scrollDetector = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (consumed.y != 0f && stickToBottom && listState.firstVisibleItemIndex > 0) {
                        // 用户手动滚动且已离开最新消息 → 退出 stickToBottom
                        // anchor 变更统一由下方 snapshotFlow 边缘检测处理
                        stickToBottom = false
                    }
                    return Offset.Zero
                }
            }
        }

        // Track scroll position for time-travel recording + 双向边缘检测
        var lastDispatchedAnchorId by remember { mutableStateOf("") }
        OBOLaunchedEffect(listState) {
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }
                .debounce(100)
                // 不用 distinctUntilChanged：快速来回滚动时 NestedScrollConnection 同步设
                // stickToBottom=false，但 debounce 后值可能回到 (0,0)，需要重新 emit 以恢复
                .collect { (first, offset) ->
                    component.updateScrollPosition(first, offset)
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    // 读最新 State 值（state.messages 通过委托读取，不依赖捕获的局部变量）
                    val currentMessages = state.messages
                    // 离开底部但未发过 anchor → 切换 At 模式启用 newMessageCount
                    if (!stickToBottom && first > 0 && lastDispatchedAnchorId.isEmpty()) {
                        val newest = currentMessages.firstOrNull()
                        if (newest != null) {
                            lastDispatchedAnchorId = newest.id
                            component.onMoveWindow(WindowAnchor.At(newest.timestamp, newest.id))
                        }
                    }
                    // 向旧端：接近列表尾部 → 窗口向历史方向滑动
                    if (lastVisible >= totalItems - EDGE_THRESHOLD && totalItems > 0 && state.hasMoreBefore) {
                        val oldest = currentMessages.lastOrNull()
                        if (oldest != null && oldest.id != lastDispatchedAnchorId) {
                            lastDispatchedAnchorId = oldest.id
                            component.onMoveWindow(WindowAnchor.At(oldest.timestamp, oldest.id))
                        }
                    }
                    // 向新端：接近列表头部且非 Latest → 窗口向最新方向滑动
                    if (first <= EDGE_THRESHOLD && !stickToBottom && state.hasMoreAfter) {
                        val newest = currentMessages.firstOrNull()
                        if (newest != null && newest.id != lastDispatchedAnchorId) {
                            lastDispatchedAnchorId = newest.id
                            component.onMoveWindow(WindowAnchor.At(newest.timestamp, newest.id))
                        }
                    }
                    // 滚回 index 0 → 恢复 stickToBottom + 同步 anchor
                    if (first == 0 && !stickToBottom) {
                        stickToBottom = true
                        lastDispatchedAnchorId = ""
                        component.onMoveWindow(WindowAnchor.Latest)
                    }
                }
        }

        // Auto-scroll: 仅 stickToBottom 时，瞬时跳转
        val latestMsgId = messages.firstOrNull()?.id
        var prevLatestMsgId by remember { mutableStateOf(latestMsgId) }
        OBOLaunchedEffect(latestMsgId) {
            if (latestMsgId != null && latestMsgId != prevLatestMsgId && prevLatestMsgId != null) {
                if (stickToBottom) {
                    listState.scrollToItem(0)
                }
            }
            prevLatestMsgId = latestMsgId
        }

        // 发消息时 Store 通过 Label 通知 UI 回到底部
        OBOLaunchedEffect(Unit) {
            component.scrollToLatestEvent.collect {
                stickToBottom = true
                lastDispatchedAnchorId = ""
                listState.scrollToItem(0)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            ExposureLazyColumn(
                listId = "chat_messages",
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxSize().nestedScroll(scrollDetector)
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, msg -> msg.id },
                    contentType = { _, message ->
                        when {
                            message.isRecalled || message.body is MessageBody.System -> "system"
                            else -> "bubble"
                        }
                    }
                ) { index, message ->
                    val olderTimestamp = messages.getOrNull(index + 1)?.timestamp
                    // 最旧消息且还有更多历史时，跳过 separator 避免加载后闪烁
                    val isLoadingBoundary = olderTimestamp == null && state.hasMoreBefore
                    if (!isLoadingBoundary && shouldShowTimeSeparator(message.timestamp, olderTimestamp)) {
                        TimeSeparator(timestamp = message.timestamp)
                    }

                    if (message.isRecalled || message.body is MessageBody.System) {
                        SystemMessageItem(message)
                    } else {
                        MessageBubble(
                            message = message,
                            onLongClick = { selectedMessage = message },
                            onResendClick = { component.onResend(message.id) }
                        )
                    }
                }
                if (!state.hasMoreBefore) {
                    item(key = "no_more_history") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = tr(Res.string.chat_no_more_history),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 到顶时滚动露出提示（仅用户主动翻历史时触发，初次加载不滚动）
            OBOLaunchedEffect(state.hasMoreBefore) {
                if (!state.hasMoreBefore && !stickToBottom) {
                    val last = listState.layoutInfo.totalItemsCount - 1
                    if (last >= 0) listState.animateScrollToItem(last)
                }
            }

            // Action popup
            selectedMessage?.let { msg ->
                MessageActionPopup(
                    message = msg,
                    onDismiss = { selectedMessage = null },
                    onCopy = { component.onCopy(msg.id) },
                    onRecall = { component.onRecall(msg.id) },
                    onDelete = { component.onDelete(msg.id) }
                )
            }

            // 「回到底部」按钮：MoveWindow(Latest) + newMessageCount 徽章
            if (!stickToBottom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, CircleShape)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surface)
                            .appClickable(component = "ScrollToBottom") {
                                scope.launch {
                                    component.onMoveWindow(WindowAnchor.Latest)
                                    stickToBottom = true
                                    lastDispatchedAnchorId = ""
                                    listState.scrollToItem(0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "↓",
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.primary
                        )
                    }
                    if (state.newMessageCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AppTheme.colors.error),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = if (state.newMessageCount > MAX_BADGE_COUNT)
                                    tr(Res.string.chat_new_message_overflow, MAX_BADGE_COUNT)
                                else state.newMessageCount.toString(),
                                style = AppTheme.typography.labelSmall,
                                color = AppTheme.colors.onError
                            )
                        }
                    }
                }
            }
        }

        // Input bar
        ChatInputBar(
            inputMode = state.inputMode,
            text = inputText,
            onTextChange = { inputText = it },
            onSendText = { text ->
                component.onSendText(text)
                inputText = ""
            },
            onSendVoice = component::onSendVoice,
            onToggleInputMode = component::onToggleInputMode,
            onToggleEmoji = component::onToggleEmojiPanel,
            onTogglePlus = component::onTogglePlusPanel
        )

        // Emoji panel — emoji appends to input, sticker sends directly
        if (state.showEmojiPanel) {
            EmojiPanel(
                onEmojiClick = { emoji -> inputText += emoji },
                onStickerClick = { id, url -> component.onSendSticker(id, url) }
            )
        }

        // Plus panel
        if (state.showPlusPanel) {
            PlusPanel(
                onPhotoClick = { component.onSendImage("", 400, 300, false) },
                onCameraClick = { component.onSendImage("", 300, 400, false) }
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    memberCount: Int,
    typingUser: String?,
    onBack: () -> Unit
) {
    AppTopBar(
        navigationIcon = {
            AppText(
                text = "←",
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.onSurface,
                modifier = Modifier.padding(8.dp).appClickable(component = "ChatTopBar:Back", onClick = onBack)
            )
        },
        title = {
            Column {
                AppText(
                    text = "$title($memberCount)",
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface
                )
                if (typingUser != null) {
                    AppText(
                        text = tr(Res.string.chat_typing, typingUser),
                        style = AppTheme.typography.labelSmall,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun SystemMessageItem(message: ChatMessage) {
    val text = when {
        message.isRecalled -> if (message.isMine) tr(Res.string.chat_recall_self) else tr(Res.string.chat_recall_other, message.senderName)
        message.body is MessageBody.System -> (message.body as MessageBody.System).text
        else -> ""
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            style = AppTheme.typography.labelSmall,
            color = AppTheme.colors.onSurfaceVariant
        )
    }
}

private const val EDGE_THRESHOLD = 10
private const val MAX_BADGE_COUNT = 99

@Preview
@Composable
fun ChatRoomContentPreview() = PreviewWrapper { componentContext ->
    val component = remember { DefaultChatRoomComponent(componentContext, "preview_conv") }
    ChatRoomContent(component)
}
