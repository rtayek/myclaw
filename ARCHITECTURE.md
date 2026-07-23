# System Architecture

## 3-Tier Model
MyClaw operates as a local, 3-tier desktop runtime:

```text
[ Java Desktop UI ] <---> [ Core Broker ] <---(Sockets)---> [ Backend Agent Engine ]
```

* **Java Desktop UI (Frontend):** Accessible Swing desktop application providing high-contrast layouts, screen reader compatibility, and keyboard-first navigation.
* **Core Broker:** Native Java event-driven core managing session state, prompt routing, thread management, and transcript persistence.
* **Backend Agent Engine:** Flexible agent execution layer (Ollama, Claude CLI) communicating over loopback TCP sockets.

## Capture-Hash-Persist Pipeline
Data moves through a strict, thread-isolated service pipeline to preserve UI responsiveness:

$$\text{[CaptureService]} \longrightarrow \text{[HashService]} \longrightarrow \text{[PersistenceService]} \longrightarrow \text{[Spatial Routing Engine]}$$

1. **`CaptureService`:** Non-blocking async ingestion of keyboard events, system hotkeys (`Win+V`), and voice input.
2. **`HashService`:** SHA-256 cryptographic indexing of text blocks and error signatures to eliminate duplicate model runs.
3. **`PersistenceService`:** Dual-stream append-only journal creating transactional `.jsonl` telemetry logs and high-contrast `.md` transcripts.
4. **`Spatial Routing Engine`:** Routes output streams to designated display zones and screen reader accessibility buffers.

## Modular Skill Execution
* Executes structured workflows defined in `SKILL.md` files located in project directories (`.agents/skills/`).
* Supports both interactive user-guided sessions and 24/7 background agent execution.
