package com.ray.tvchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*

// ---------------------------------------------------------------------------
// EDIT THESE for your network. For anything beyond LAN hobby use, move the
// token out of source (BuildConfig field or first-run entry screen).
// ---------------------------------------------------------------------------
private const val BRIDGE_URL = "ws://192.168.1.50:8765"
private const val BRIDGE_TOKEN = "CHANGE-ME"

class MainActivity : ComponentActivity() {

    private lateinit var bridge: BridgeClient

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bridge = BridgeClient(BRIDGE_URL, BRIDGE_TOKEN)
        bridge.start()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val state by bridge.state.collectAsStateWithLifecycle()
                AppScreen(
                    state = state,
                    onSelectSession = bridge::selectSession,
                    onSend = bridge::sendChat,
                    onClear = bridge::clearSession,
                )
            }
        }
    }

    override fun onDestroy() {
        bridge.stop()
        super.onDestroy()
    }
}

@Composable
fun AppScreen(
    state: UiState,
    onSelectSession: (String) -> Unit,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
            .padding(16.dp)
    ) {
        SessionRail(state, onSelectSession, onClear, Modifier.width(230.dp).fillMaxHeight())
        Spacer(Modifier.width(16.dp))
        ChatPane(state, onSend, Modifier.weight(1f).fillMaxHeight())
    }
}

// ---------------------------------------------------------------------------
// Left rail: one focusable card per session + status footer
// ---------------------------------------------------------------------------
@Composable
fun SessionRail(
    state: UiState,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("Sessions", fontSize = 20.sp, color = Color(0xFF9AA4AF),
            modifier = Modifier.padding(bottom = 12.dp))

        state.sessions.forEach { session ->
            val selected = session.id == state.currentSession
            val unread = session.id in state.unread
            val busy = session.id in state.busy

            Card(
                onClick = { onSelect(session.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.colors(
                    containerColor = if (selected) Color(0xFF1E2A38) else Color(0xFF161B22),
                ),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.label,
                        fontSize = 18.sp,
                        color = if (selected) Color.White else Color(0xFFB8C2CC),
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        busy -> Dot(Color(0xFFF2C14E))    // amber: thinking
                        unread -> Dot(Color(0xFF4EA1F2))  // blue: new output
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("Clear this chat")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            state.status,
            fontSize = 13.sp,
            color = if (state.connected) Color(0xFF6BCB77) else Color(0xFFE07A5F),
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color)
    )
}

// ---------------------------------------------------------------------------
// Chat pane: message list + input field
// ---------------------------------------------------------------------------
@Composable
fun ChatPane(state: UiState, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    val sessionId = state.currentSession
    val messages = state.messages[sessionId].orEmpty()
    val listState = rememberLazyListState()

    // keep view pinned to the latest message while streaming
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161B22))
                .padding(16.dp),
        ) {
            items(messages) { msg -> MessageBubble(msg) }
        }

        Spacer(Modifier.height(12.dp))

        InputBar(
            enabled = sessionId != null && sessionId !in state.busy,
            hint = when {
                sessionId == null -> "Waiting for server…"
                sessionId in state.busy -> "Thinking…"
                else -> "Press OK to type or dictate"
            },
            onSend = onSend,
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val bg = when (msg.role) {
        "user" -> Color(0xFF1E3A5F)
        "error" -> Color(0xFF5F1E1E)
        else -> Color(0xFF21262D)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            msg.text.ifEmpty { "…" },
            fontSize = 18.sp,          // readable from the couch
            lineHeight = 26.sp,
            color = Color(0xFFE6EDF3),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Input: a focusable text field. Focus it with the D-pad, press OK, and the
// system TV keyboard (with mic dictation) appears. Send button beside it.
// ---------------------------------------------------------------------------
@Composable
fun InputBar(enabled: Boolean, hint: String, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF161B22))
                .border(
                    width = 2.dp,
                    color = if (focused) Color(0xFF4EA1F2) else Color(0xFF30363D),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text(hint, color = Color(0xFF6E7781), fontSize = 18.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                enabled = enabled,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color(0xFF4EA1F2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        Spacer(Modifier.width(10.dp))
        Button(
            enabled = enabled && text.isNotBlank(),
            onClick = {
                onSend(text.trim())
                text = ""
            },
        ) { Text("Send") }
    }
}
