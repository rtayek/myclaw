# TASK: Create/Update the 5 Root Documentation Files ONLY

## CRITICAL CONSTRAINTS (READ FIRST)
1. DO NOT write, modify, or generate any Java code, backend scripts, socket protocols, or implementation code.
2. DO NOT delete, move, or modify any existing subfolders or files outside of the 5 target root files.
3. Your ONLY task is to create or edit these exact 5 files at the root of `rtayek/myclaw`:
   - `README.md`
   - `VISION.md`
   - `ARCHITECTURE.md`
   - `HOWTO.md`
   - `ROADMAP.md`

---

## Technical Context
System architecture:
Desktop Java UI (Frontend) <---> Core Broker <---(Sockets)---> Backend Agent Engine

---

## FILE TEMPLATES TO CREATE AT REPO ROOT

### 1. File: `README.md`
```markdown
# MyClaw

> Local-first, desktop-native AI agent runtime.

## Overview
MyClaw is a 3-tier desktop runtime linking a native Java interface to a socket-based backend agent engine.

```text
[ Java Desktop UI ] <---> [ Core Broker ] <---(Sockets)---> [ Backend Engine ]
```
