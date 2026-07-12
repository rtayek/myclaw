package com.ray.tvchat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val text: String)          // role: user | assistant | error
data class SessionInfo(val id: String, val label: String)

data class UiState(
    val connected: Boolean = false,
    val status: String = "Connecting…",
    val sessions: List<SessionInfo> = emptyList(),
    val currentSession: String? = null,
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val busy: Set<String> = emptySet(),        // sessions awaiting a reply
    val unread: Set<String> = emptySet(),      // backgrounded sessions with new output
)

class BridgeClient(
    private val url: String,      // e.g. ws://192.168.1.50:8765
    private val token: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // keeps the socket alive through idle
        .build()

    private var ws: WebSocket? = null
    private var wantOpen = true
    private var backoffMs = 1000L

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun start() { wantOpen = true; connect() }

    fun stop() { wantOpen = false; ws?.close(1000, "bye") }

    private fun connect() {
        _state.value = _state.value.copy(status = "Connecting…")
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                backoffMs = 1000L
                webSocket.send(JSONObject().put("type", "auth").put("token", token).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handle(JSONObject(text))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = _state.value.copy(connected = false, status = "Disconnected: ${t.message}")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = _state.value.copy(connected = false, status = "Closed: $reason")
                if (code == 4001) {
                    _state.value = _state.value.copy(status = "Auth failed — check token")
                } else {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (!wantOpen) return
        scope.launch {
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(15_000L)
            if (wantOpen) connect()
        }
    }

    private fun handle(msg: JSONObject) {
        val s = _state.value
        when (msg.optString("type")) {
            "ready" -> {
                val list = mutableListOf<SessionInfo>()
                val arr: JSONArray = msg.optJSONArray("sessions") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(SessionInfo(o.getString("id"), o.getString("label")))
                }
                _state.value = s.copy(
                    connected = true,
                    status = "Connected",
                    sessions = list,
                    currentSession = s.currentSession ?: list.firstOrNull()?.id,
                )
            }

            "token" -> {
                val id = msg.getString("session")
                val text = msg.optString("text")
                val msgs = s.messages[id].orEmpty().toMutableList()
                // append to the streaming assistant message, or start one
                if (msgs.lastOrNull()?.role == "assistant-streaming") {
                    val last = msgs.removeAt(msgs.size - 1)
                    msgs.add(last.copy(text = last.text + text))
                } else {
                    msgs.add(ChatMessage("assistant-streaming", text))
                }
                val unread = if (id != s.currentSession) s.unread + id else s.unread
                _state.value = s.copy(messages = s.messages + (id to msgs), unread = unread)
            }

            "done" -> {
                val id = msg.getString("session")
                val msgs = s.messages[id].orEmpty().toMutableList()
                if (msgs.lastOrNull()?.role == "assistant-streaming") {
                    val last = msgs.removeAt(msgs.size - 1)
                    msgs.add(ChatMessage("assistant", last.text.trim()))
                }
                _state.value = s.copy(messages = s.messages + (id to msgs), busy = s.busy - id)
            }

            "error" -> {
                val id = msg.optString("session")
                val msgs = s.messages[id].orEmpty() + ChatMessage("error", msg.optString("text"))
                _state.value = s.copy(messages = s.messages + (id to msgs), busy = s.busy - id)
            }

            "cleared" -> {
                val id = msg.getString("session")
                _state.value = s.copy(messages = s.messages + (id to emptyList()))
            }
        }
    }

    fun selectSession(id: String) {
        val s = _state.value
        _state.value = s.copy(currentSession = id, unread = s.unread - id)
    }

    fun sendChat(text: String) {
        val s = _state.value
        val id = s.currentSession ?: return
        if (text.isBlank() || id in s.busy) return
        val msgs = s.messages[id].orEmpty() + ChatMessage("user", text)
        _state.value = s.copy(messages = s.messages + (id to msgs), busy = s.busy + id)
        ws?.send(JSONObject().put("type", "chat").put("session", id).put("text", text).toString())
    }

    fun clearSession() {
        val id = _state.value.currentSession ?: return
        ws?.send(JSONObject().put("type", "clear").put("session", id).toString())
    }
}
