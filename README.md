# MyClaw

> Local-first, desktop-native AI agent runtime.

## Overview
MyClaw is a 3-tier desktop runtime linking a native Java interface to a socket-based backend agent engine.

```text
[ Java Desktop UI ] <---> [ Core Broker ] <---(Sockets)---> [ Backend Engine ]
```

## Core Principles
* **Local-First & Private:** Desktop-native execution leveraging local models (Ollama `glm4:9b`) and CLI engines (Claude CLI) with full user data sovereignty.
* **Accessible Desktop Control:** Native Java desktop UI tailored for low-vision and blind developers (screen reader support, scalable high contrast, keyboard-only navigation, speech I/O).
* **24/7 Persistence & Telemetry:** Append-only transaction logging (`.jsonl` telemetry and `.md` human-readable transcripts) supporting continuous background agent operation.
* **Modular Skill Engine:** Execution of modular `SKILL.md` workflows and hybrid agent loops.

## Core Documentation
* [VISION.md](VISION.md) - Accessibility vision, design philosophy, and spatial serenity layout.
* [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture, 3-tier model, and thread isolation.
* [HOWTO.md](HOWTO.md) - Build, test, execution, and commit instructions.
* [ROADMAP.md](ROADMAP.md) - Project milestones, current capabilities, and future vision.
