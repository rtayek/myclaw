// bridge.js — WebSocket <-> CLI/HTTP bridge for the TV chat client
// Run:  BRIDGE_TOKEN=yoursecret node bridge.js
// Deps: npm install ws

const { WebSocketServer } = require("ws");
const { spawn } = require("child_process");
const http = require("http");

// ---------------------------------------------------------------------------
// Config — edit this section
// ---------------------------------------------------------------------------

const PORT = 8765;
const AUTH_TOKEN = process.env.BRIDGE_TOKEN; // never hardcode; set in env

// Each session the TV can select. Two backend kinds:
//   kind: "jar"    -> spawns: java -jar <jar> <arg> "<prompt>"
//   kind: "ollama" -> POSTs to local Ollama /api/chat (streams tokens)
//
// history: "bridge" = bridge stores the transcript and prepends recent turns
//                     into the prompt (for stateless one-shot CLIs).
//          "none"   = send only the raw prompt (use if your jar keeps its
//                     own conversation state, or for one-shot commands).
const SESSIONS = {
  "claude": {
    label: "Claude (jar)",
    kind: "jar",
    jar: "/home/ray/yourapp/yourapp.jar",
    arg: "claude",
    history: "bridge",
    historyTurns: 6, // how many past user/assistant turns to prepend
  },
  "qwen-local": {
    label: "Qwen (Ollama)",
    kind: "ollama",
    model: "qwen2.5",
    url: "http://localhost:11434/api/chat",
    history: "bridge", // Ollama /api/chat takes full message arrays anyway
    historyTurns: 12,
  },
};

if (!AUTH_TOKEN) {
  console.error("Refusing to start: set BRIDGE_TOKEN in the environment.");
  process.exit(1);
}

// ---------------------------------------------------------------------------
// Per-session transcripts (in memory; restart clears them)
// ---------------------------------------------------------------------------

const transcripts = {}; // sessionId -> [{role, text}]

function remember(sessionId, role, text) {
  (transcripts[sessionId] ||= []).push({ role, text });
}

function buildJarPrompt(cfg, sessionId, prompt) {
  if (cfg.history !== "bridge") return prompt;
  const past = (transcripts[sessionId] || []).slice(-cfg.historyTurns);
  if (past.length === 0) return prompt;
  const lines = past.map((t) => `${t.role === "user" ? "User" : "Assistant"}: ${t.text}`);
  return (
    "Previous conversation:\n" +
    lines.join("\n") +
    "\n\nContinue the conversation. User: " +
    prompt
  );
}

// ---------------------------------------------------------------------------
// Backends
// ---------------------------------------------------------------------------

function runJar(cfg, sessionId, prompt, send) {
  const fullPrompt = buildJarPrompt(cfg, sessionId, prompt);
  // prompt is its own argv entry — no shell, no quoting/injection issues
  const proc = spawn("java", ["-jar", cfg.jar, cfg.arg, fullPrompt]);

  let reply = "";
  proc.stdout.on("data", (chunk) => {
    const text = chunk.toString();
    reply += text;
    send({ type: "token", session: sessionId, text });
  });
  proc.stderr.on("data", (chunk) => {
    send({ type: "token", session: sessionId, text: chunk.toString() });
  });
  proc.on("close", (code) => {
    if (cfg.history === "bridge") {
      remember(sessionId, "user", prompt);
      remember(sessionId, "assistant", reply.trim());
    }
    send({ type: "done", session: sessionId, exitCode: code });
  });
  proc.on("error", (err) => {
    send({ type: "error", session: sessionId, text: String(err) });
    send({ type: "done", session: sessionId, exitCode: -1 });
  });
}

function runOllama(cfg, sessionId, prompt, send) {
  const past = (transcripts[sessionId] || []).slice(-cfg.historyTurns);
  const messages = past
    .map((t) => ({ role: t.role, content: t.text }))
    .concat([{ role: "user", content: prompt }]);

  const body = JSON.stringify({ model: cfg.model, messages, stream: true });
  const url = new URL(cfg.url);

  const req = http.request(
    { hostname: url.hostname, port: url.port, path: url.pathname, method: "POST",
      headers: { "Content-Type": "application/json" } },
    (res) => {
      let reply = "";
      let buf = "";
      res.on("data", (chunk) => {
        buf += chunk.toString();
        let nl;
        while ((nl = buf.indexOf("\n")) >= 0) {
          const line = buf.slice(0, nl).trim();
          buf = buf.slice(nl + 1);
          if (!line) continue;
          try {
            const obj = JSON.parse(line);
            const text = obj.message?.content || "";
            if (text) {
              reply += text;
              send({ type: "token", session: sessionId, text });
            }
            if (obj.done) {
              remember(sessionId, "user", prompt);
              remember(sessionId, "assistant", reply);
              send({ type: "done", session: sessionId, exitCode: 0 });
            }
          } catch { /* partial line; wait for more */ }
        }
      });
      res.on("error", (err) => {
        send({ type: "error", session: sessionId, text: String(err) });
        send({ type: "done", session: sessionId, exitCode: -1 });
      });
    }
  );
  req.on("error", (err) => {
    send({ type: "error", session: sessionId, text: "Ollama unreachable: " + err.message });
    send({ type: "done", session: sessionId, exitCode: -1 });
  });
  req.end(body);
}

// ---------------------------------------------------------------------------
// WebSocket server
// ---------------------------------------------------------------------------

const wss = new WebSocketServer({ port: PORT });

wss.on("connection", (ws, req) => {
  let authed = false;
  console.log("connection from", req.socket.remoteAddress);

  const send = (obj) => {
    if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(obj));
  };

  ws.on("message", (raw) => {
    let msg;
    try { msg = JSON.parse(raw); } catch { return; }

    if (!authed) {
      if (msg.type === "auth" && msg.token === AUTH_TOKEN) {
        authed = true;
        // tell the TV which sessions exist so the rail builds itself
        send({
          type: "ready",
          sessions: Object.entries(SESSIONS).map(([id, s]) => ({ id, label: s.label })),
        });
      } else {
        ws.close(4001, "bad token");
      }
      return;
    }

    if (msg.type === "chat") {
      const cfg = SESSIONS[msg.session];
      if (!cfg) return send({ type: "error", session: msg.session, text: "unknown session" });
      if (typeof msg.text !== "string" || !msg.text.trim()) return;

      if (cfg.kind === "jar") runJar(cfg, msg.session, msg.text, send);
      else if (cfg.kind === "ollama") runOllama(cfg, msg.session, msg.text, send);
    }

    if (msg.type === "clear") {
      delete transcripts[msg.session];
      send({ type: "cleared", session: msg.session });
    }
  });
});

console.log(`bridge listening on ws://0.0.0.0:${PORT}`);
console.log("sessions:", Object.keys(SESSIONS).join(", "));
