# Gemini's Vision for Switchboard

## Core Premise
**Switchboard is an accessible, multi-window AI cockpit backed by a durable, provider-independent library.** 
It is built for people who require their ears, voice, and keys to accomplish what eyes and hands usually do. Above all, it guarantees that you own every conversation you hold through it—permanently, locally, and in a format you control.

---

## The Core Problem: Fragmentation & Lock-in

Modern AI interfaces assume sharp vision and fast hands: tiny text, mouse-heavy layouts, and fleeting responses. 
Concurrently, your most valuable intellectual work is scattered across corporate providers. No API grants access to your complete chat history, and manual exports are lossy, proprietary, and increasingly gated. 
**What you create with AI today is simply not yours.** Switchboard exists to solve this.

---

## The Promise: Complete Sovereignty

Every conversation held through Switchboard is captured at the moment of transaction at full fidelity on your own machine. No exports, no permissions, and no vendor cooperation required.
By capturing data on the way in, you establish **complete data sovereignty** before any platform can lock it away.

---

## The Product, in Two Stages

### Stage One: The Cockpit
An accessible desktop application acting as your primary terminal to multiple AI backends (e.g., Claude, local Ollama models, and Gemini), fully swappable via a single keystroke.

* **Session over Model:** The fundamental unit is the session, not the model. Models and backends can change; the session is the durable record of your work.
* **Adaptive Interfaces:** Large, scalable text everywhere. Fully navigable via keyboard, touch, voice, mouse, or trackpad. Any single input method must be sufficient to drive the entire system.
* **Independent Sessions:** Open, arrange, detach, restore, and use multiple sessions across one or more monitors without constraints.
* **Speech Integration:** Fast dictation for prompts and instant, high-quality text-to-speech (TTS) for spoken responses.
* **Failsafe Errors:** Highly visible, copyable, and readable errors. Diagnostic information is never trapped inside an unreachable dialog box.
* **Sovereign Local Storage:** Every conversation is written locally to disk with strict provenance details—capturing exactly which model answered, when, and under what parameters.

### Stage Two: The Library
As you use the cockpit daily, you build a structured local archive. The library makes this data durable and actionable.

* **Cross-Provider Search:** Search across all historical threads, regardless of which backend generated the response.
* **Project Organization:** Group work by projects, sessions, and user goals rather than provider names. A backend is a worker inside the project, not the home of the work.
* **Common Local Schema:** A unified model that preserves the exact metadata of your conversations.
* **Strict Provenance Preservation:** Imported conversations from external providers are structurally distinguished from native captures. The original import artifact is kept unchanged to preserve historic authenticity, logging the source and any known fidelity limitations.
* **Originals vs. Derived Artifacts:** Summaries, tags, indexes, extracted decisions, and project notes remain strictly separate from the original captured or imported record. They must never silently rewrite history.
* **Seamless Handoffs:** Generate concise context summaries to transition your active work from one AI model to another effortlessly.

---

## Guiding Architectural Principles

1. **Accessibility is Non-Negotiable:** If an essential function cannot be performed through large readable text, keyboard shortcuts, touch, or voice dictation, the feature is incomplete.
2. **Capture Before Presentation:** The original session record is preserved exactly. Conversation, Detail, and Raw views are merely projections of that immutable record.
3. **Archival Integrity:** Switchboard never assumes an imported conversation is 100% complete unless the source explicitly guarantees it. It preserves what it receives, records what is known, and flags what is missing.
4. **Zero-Dependency Portability:** Every saved conversation belongs to the user, on the user's local machine, in a standard format (such as flat JSON or structured Markdown) that can be read without running the application.
5. **Backend Decoupling:** The client should not need to know whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.

---

## Success and Milestones

**First Milestone:** 
The developer runs Switchboard daily. A response can be read aloud with a single keystroke, and yesterday's conversation can be seamlessly re-opened and continued today.

**What Success Looks Like:**
* **Short-Term:** Active, daily adoption by the developer and ten real-world users who rely on its accessibility features. We prioritize depth and reliability over broad appeal.
* **Long-Term:** Providing lasting ownership, understanding, and control of AI-assisted work to thousands of developers and creators who refuse to rent their own history.

**Deferred (For Focus):**
Web-based demos, television interfaces, collaborative multi-user services, autonomous AI teams, and specialized tutorial systems. The core abstractions of Switchboard will be built to keep these doors open, but the accessible desktop cockpit remains the absolute MVP priority.
