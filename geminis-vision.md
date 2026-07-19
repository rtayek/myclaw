# Gemini's Vision for Switchboard

## Core Premise
**Switchboard is an accessible, multi-window AI cockpit and sovereign agent harness backed by a durable, provider-independent library.** 

It is built for people who require their ears, voice, and keys to accomplish what eyes and hands usually do. Above all, it guarantees that you own every conversation and context record held through it—permanently, locally, and in a format under your complete control.

---

## The Post-Frontier Reality: The Harness is the Product

We have entered the post-frontier era of artificial intelligence. The single monolithic model is no longer the product; **the product is the harness—the orchestration system, context layer, and evaluation loop that surrounds the model**. 

No single model excels at every task or cost profile. The true value lies in embedding AI into an accessible harness that keeps proprietary knowledge local, routes queries intelligently, and pairs models with specialized tools and advisors.

---

## The Core Problem: Fragmentation, Lock-in, and Opaque Cloud Control

Modern AI interfaces assume sharp vision and fast hands: tiny text, mouse-heavy layouts, and fleeting responses. Concurrently, your most valuable intellectual work and tacit knowledge are scattered across closed corporate providers. No API grants access to your complete chat history, and manual exports are lossy, proprietary, and increasingly gated.

Furthermore, cloud-only lock-in forces users to surrender their context data and pay high token markups for tasks better served locally. **What you create with AI today is simply not yours.** Switchboard exists to solve this.

---

## The Promise: Complete Data & Context Sovereignty

Every conversation held through Switchboard is captured at the moment of transaction at full fidelity on your own machine. No exports, no permissions, and no vendor cooperation required.

By capturing data on the way in, you establish **complete data and context sovereignty** before any platform can lock it away. Because switching models is simply a function of rehydrating local context, owning your history ensures complete freedom across the entire AI ecosystem.

---

## Architecture: Hybrid & Deskside Local Compute

Switchboard implements a **local-first, hybrid compute paradigm**:

* **Local Open-Weight Orchestrator (Deskside Execution):** Everyday workloads run locally via open-weight models (e.g., Ollama, quantized open models). Running on local hardware (including unified-memory architectures) delivers zero per-token costs, low latency, and total data privacy.
* **Frontier Models as Sub-Agents / Advisors:** When a task demands complex reasoning beyond local models, the local harness seamlessly escalates to remote frontier models (Claude, Gemini, GPT) as specialized sub-agents.
* **Token Value per Watt:** Optimizes for maximum output value per watt per user, balancing compute economics and local power efficiency with frontier intelligence.

---

## The Product, in Two Stages

### Stage One: The Cockpit
An accessible desktop application acting as your primary terminal to local and cloud AI backends, fully swappable via a single keystroke.

* **Session over Model:** The fundamental unit is the session, not the model. Models and backends can change; the session is the durable record of your work.
* **Local-First Default:** Local models (Ollama/open weights) are the default. If network drops or quotas exhaust, the cockpit falls back to local execution with clear status indicators.
* **Adaptive Interfaces:** Large, scalable text everywhere. Fully navigable via keyboard, touch, voice, mouse, or trackpad. Any single input method must be sufficient to drive the entire system.
* **Independent Sessions:** Open, arrange, detach, restore, and use multiple sessions across one or more monitors without constraints.
* **Speech & Audio Cues:** Fast dictation for prompts and instant text-to-speech (TTS) for responses. Distinct audio feedback for progress, completion, and errors.
* **Failsafe Errors:** Highly visible, copyable, and readable errors. Diagnostic information is never trapped inside an unreachable dialog box.
* **Sovereign Local Storage:** Every conversation is written locally to disk with strict provenance details—capturing exactly which model answered, when, and under what parameters.

### Stage Two: The Library
As you use the cockpit daily, you build a structured local archive. The library makes this data durable and actionable.

* **Cross-Provider Search:** Search across all historical threads, regardless of which backend generated the response.
* **Project Organization:** Group work by projects, sessions, and user goals rather than provider names. A backend is a worker inside the project, not the home of the work.
* **Common Local Schema:** A unified model that preserves the exact metadata of your conversations.
* **Strict Provenance Preservation:** Imported conversations from external providers are structurally distinguished from native captures. The original import artifact is kept unchanged to preserve historic authenticity.
* **Originals vs. Derived Artifacts:** Summaries, tags, indexes, extracted decisions, and project notes remain strictly separate from the original captured or imported record. They must never silently rewrite history.
* **Seamless Context Rehydration:** Generate concise context summaries to transition your active work from one AI model to another effortlessly.

---

## Guiding Architectural Principles

1. **Accessibility is Non-Negotiable:** If an essential function cannot be performed through large readable text, keyboard shortcuts, touch, or voice dictation, the feature is incomplete.
2. **The Harness Belongs to the User:** The orchestration loop, context history, and tool definitions belong to the user's local environment.
3. **Capture Before Presentation:** The original session record is preserved exactly. Conversation, Detail, and Raw views are merely projections of that immutable record.
4. **Archival Integrity:** Switchboard never assumes an imported conversation is 100% complete unless the source explicitly guarantees it. It preserves what it receives and flags known limitations.
5. **Zero-Dependency Portability:** Every saved conversation belongs to the user, on the user's local machine, in standard formats (flat JSON or structured Markdown) that can be read without running the application.
6. **Backend Decoupling:** The client does not care whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.

---

## Success and Milestones

**First Milestone:** 
The developer runs Switchboard daily. A response can be read aloud with a single keystroke, and yesterday's conversation can be seamlessly re-opened and continued today.

**What Success Looks Like:**
* **Short-Term:** Active, daily adoption by the developer and ten real-world users who rely on its accessibility features. We prioritize depth and reliability over broad appeal.
* **Long-Term:** Providing lasting ownership, understanding, and control of AI-assisted work to thousands of developers and creators who refuse to rent their own history.

**Deferred (For Focus):**
Web-based demos, television interfaces, collaborative multi-user services, autonomous AI teams, and specialized tutorial systems. The core abstractions of Switchboard will be built to keep these doors open, but the accessible desktop cockpit remains the absolute MVP priority.
