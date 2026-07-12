# TV Chat — Google TV frontend + Node bridge to your home server

Two pieces:

```
bridge/    Node WebSocket server. Routes each "session" to a backend:
           your jar (java -jar ... claude "<prompt>") or Ollama.
tv-app/    Android TV app (Kotlin + Compose for TV). Session rail on the
           left, chat pane, D-pad friendly, mic dictation via system keyboard.
```

Protocol between them (JSON over one WebSocket):

```
TV -> bridge   {"type":"auth","token":"..."}
bridge -> TV   {"type":"ready","sessions":[{"id":"claude","label":"Claude (jar)"}, ...]}
TV -> bridge   {"type":"chat","session":"claude","text":"hello"}
bridge -> TV   {"type":"token","session":"claude","text":"partial output"}   (repeats)
bridge -> TV   {"type":"done","session":"claude","exitCode":0}
TV -> bridge   {"type":"clear","session":"claude"}
```

---

## 1. Bridge (run in WSL or Windows — wherever java + your jar live)

```bash
cd bridge
npm install
BRIDGE_TOKEN=pick-a-long-random-string node bridge.js
```

Edit the `SESSIONS` block at the top of `bridge.js`:

- Set `jar:` to your actual jar path.
- The `claude` session runs: `java -jar <jar> claude "<full prompt>"`.
  The prompt is passed as a single argv entry — no shell involved, so any
  characters in the prompt are safe.
- `history: "bridge"` makes the bridge prepend recent turns into the prompt
  (dialogue works even though each jar run is one-shot). If your jar keeps
  its own conversation state, change to `history: "none"`.
- The `qwen-local` session streams from Ollama at `localhost:11434`.
  If the bridge runs in WSL and Ollama on Windows, your mirrored networking
  mode makes `localhost` work as-is.

Note on WSL: if the bridge runs inside WSL, the TV must be able to reach it.
Mirrored networking mode (which you already enabled) exposes WSL ports on the
Windows host's LAN address, but Windows Firewall may still need an inbound
rule for TCP 8765.

## 2. TV app

Edit the two constants at the top of
`tv-app/app/src/main/java/com/ray/tvchat/MainActivity.kt`:

```kotlin
private const val BRIDGE_URL = "ws://<your-pc-lan-ip>:8765"
private const val BRIDGE_TOKEN = "same-string-as-the-bridge"
```

Build (Android Studio: open `tv-app/`, or CLI with the Android SDK installed):

```bash
cd tv-app
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/app-debug.apk
```

Deploy over Wi-Fi from your desk:

1. On the Google TV: Settings > System > About > click "Android TV OS build"
   7 times to unlock Developer options, then enable USB debugging
   (covers network debugging on most models; some have a separate
   "Wireless debugging" toggle).
2. Find the TV's IP: Settings > Network & Internet.
3. From your PC:

```bash
adb connect <tv-ip>:5555     # accept the prompt on the TV the first time
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app appears on the TV home row as "TV Chat".

## 3. Using it

- D-pad left/right moves between the session rail and the chat.
- Up/down on the rail picks a session; OK selects.
- Focus the input box, press OK — the system keyboard appears; use the
  remote's mic button / keyboard mic for dictation.
- Amber dot on a session = thinking; blue dot = new output in a
  backgrounded session.
- "Clear this chat" wipes that session's history on the bridge.

## Notes / next steps

- Transcripts live in bridge memory only; restart clears them. Persist to a
  JSON file in `remember()` if you want them to survive restarts.
- Traffic is plain `ws://` on your LAN (the manifest allows cleartext for
  that reason). Don't port-forward 8765 to the internet as-is.
- To add another AI: add an entry to `SESSIONS` in bridge.js. The TV rail
  builds itself from the server's `ready` frame — no app changes.
- v2 idea: move the WebSocket server inside your jar (Java-WebSocket or
  Javalin) to eliminate per-prompt JVM startup; the TV app won't need to
  change as long as the protocol stays the same.
