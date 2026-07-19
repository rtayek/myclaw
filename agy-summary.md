# Summary of MyClaw and Manifold Switchboard Vision Statements

    I have analyzed the three AI-authored vision statement files located in the myclaw workspace:
    1. claudes-vision.md (Claude's perspective)
    2. geminis-vision.md (Gemini's perspective)
    3. openais-vision.md (OpenAI's perspective)

    ---

    ## 1. Manifold Switchboard / Manifold Vision (Claude, Gemini, OpenAI)

    The three AI-specific vision documents outline a single unified product concept under different
  names (**Manifold Switchboard** or **Manifold**).

    ### Core Problem
    * **Accessibility Barriers:** Modern AI interfaces are designed around sharp eyes and fast hands.
  They use tiny, unscalable text, mouse-reliant layouts, and silent/fleeting responses.
    * **Data Lock-in:** User conversation histories are scattered across closed platforms. Exports
  are manual, lossy, structured in proprietary formats, and increasingly gated. What users create
  with AI is not truly owned by them.

    ### Core Value Proposition
    > **An accessible AI cockpit** designed for users who rely on keyboard navigation, voice
  dictation, and spoken output, backed by a **durable, local archive** ensuring complete data
  sovereignty.

    ### Guiding Principles & Modularity
    All three LLM vision documents now align on the core decoupling architectural rule:
    * **Backend Decoupling:** The client should not need to know whether a backend uses a local
  process, local HTTP service, socket, remote server, or cloud API.
    * **Session-Centered Design:** The fundamental unit is the session, not the model. Models and
  backends can change; the session is the durable record of the user's work.
    * **Capture Before Presentation:** The original session record is preserved independently from
  the views that show it. Conversation, detail, and raw views are projections of the stored record.
    * **Originals vs. Derived Artifacts:** Summaries, tags, indexes, extracted decisions, embeddings,
  and project notes remain separate from the original captured or imported record.
    * **Project-First Organization:** Work is organized around projects, sessions, and user goals
  rather than provider names. A backend is a worker inside the project, not the place where the work
  belongs.

    ### Two-Stage Roadmap
    1. **Stage One: The Cockpit (Active Interface)**
       * Single keyboard-centric, voice-capable desktop client talking to multiple AI backends
  (Claude, local Ollama models, Gemini, etc.).
       * Multiple independent sessions and windows that can be opened, arranged, detached,
  restored, and used across one or more monitors.
       * Large, scalable text with support for speech-in (dictation) and speech-out (Text-to-Speech).
       * Transparent, readable, and copyable errors (never trapped in unreachable dialogues).
       * Native capture: All transactions are written locally at full fidelity, with precise metadata
  (which model, parameters, timestamp).
    2. **Stage Two: The Library (The Archive)**
       * Local, cross-provider searchable archive of all historical conversations.
       * Organization of chats by project, session, and user goal rather than by provider.
       * Best-effort, lossless import of historical data (always distinguished from native capture,
  preserving original raw files).
       * Context-aware handoff generation to continue a thread with a different AI model.

    ---

    ## 2. Antigravity's Thoughts & Insights on the Project

    Having analyzed the codebase and the respective vision documents, I see several standout
  architectural strengths and potential areas of opportunity for the **MyClaw / Manifold
  Switchboard** project:

    ### Architectural Strengths
    * **Decoupled Integration Model:** Designing `PromptService` as a transport-neutral boundary is a
  major engineering win. By hiding whether a model is executed via local CLI process, local socket,
  local HTTP, or cloud API, the frontend remains completely insulated from API changes or network
  protocols. This makes upgrading or adding backends exceptionally low-friction.
    * **Accessibility-Driven Architecture:** In UI engineering, treating accessibility as a first-
  class citizen (large text, keyboard-first focus, screen-reader semantics) often forces cleaner
  division between application logic and state representation. For example, ensuring that everything
  is navigable by voice/dictation and text-to-speech inherently prevents deep, nested visual
  components from harboring side-effects.
    * **Data Sovereignty is the Real Moat:** Today's AI providers treat chat history as a proprietary
  retention tool. Logging transactions at full fidelity, with precise metadata (seed, parameters,
  specific sub-model), onto flat files owned directly by the user, creates a highly durable,
  structured dataset. This is a foundational asset for building personal, local knowledge bases
  (RAG/agentic retrieval) in the future.

    ### Strategic Recommendations & Opportunities
    * **Frictionless Handoff Formats:** Since the system captures complete conversation histories
  locally, a killer feature for the "Library" stage is generating structured, markdown-based
  **context snapshots** or **handoff packets** (e.g., standardizing system prompt, variables, and
  recent turn history). This would allow users to transition a coding session from Claude to Gemini
  (or vice-versa) with a single click.
    * **Failsafe Offline Fallback:** Because the system supports local Ollama models, there is a
  strong opportunity to build a local routing fallback. If cloud API limits are reached, or network
  connection is lost, the cockpit could switch seamlessly to a lightweight local model (like
  `glm4:9b` or similar) to handle basic operations without breaking user flow.
    * **Incremental Web Sandbox:** While the web teaser is limited to show the UI and basic chat,
  standardizing the sandbox via a Docker container or local VM run could eventually enable safe,
  remote execution of code. However, starting with the simple reverse-proxied teaser remains the most
  pragmatic way to drive desktop adoption.
