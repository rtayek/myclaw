# MyClaw

> Local-first, desktop-native AI workbench built for accessibility.

## Overview

MyClaw is a 3-tier desktop runtime linking a native Java interface to a
socket-based backend engine.

```text
[ Java Desktop UI ] <---> [ Core Broker ] <---(Sockets)---> [ Backend Engine ]
```

It is built for a person who is present at the keyboard the whole time and
needs the interaction itself to be survivable — not for an agent working
while you stop watching.

## Core Principles

* **Local-First & Private:** Desktop-native execution using local models
  (Ollama `glm4:9b`) and CLI engines (Claude CLI). Nothing leaves your
  machine except the prompts you choose to send.
* **You Own the Record:** Every exchange is captured on the way in, at full
  fidelity, to append-only files on your disk — `.jsonl` telemetry and
  human-readable `.md` transcripts. No export, no vendor cooperation
  required.
* **Accessible Desktop Control:** Native Java UI for blind, low-vision,
  deaf, and hard-of-hearing developers — screen reader support, scalable
  high-contrast text, keyboard-only navigation, speech in and out.
* **Backends Are Disposable:** The client does not need to know whether a
  backend is a local process, a socket, or a cloud API. The session is the
  durable unit; models change.

## Core Documentation

* [VISION.md](VISION.md) — mission, what this is and is not, scope
  boundaries.
* [ARCHITECTURE.md](ARCHITECTURE.md) — 3-tier model, socket protocol,
  capture pipeline.
* [HOWTO.md](HOWTO.md) — build, test, run, commit.
* [ROADMAP.md](ROADMAP.md) — current capabilities and planned work.
