# MyClaw Project Handoff

## Purpose of This Document

This document is intended to start a fresh ChatGPT, Claude, or Codex conversation without losing the important technical and product context developed in the previous long conversation.

It deliberately errs on the side of preserving too much context rather than too little.

The immediate subject is the evolution of `myclaw` from a Java desktop AI workbench into a product family that may include:

- a full native desktop application;
- a lightweight browser-accessible demonstration or teaser;
- a Java HTTP server exposing the existing application services;
- several interchangeable local and cloud AI backends;
- a public website and installer distribution path.

This handoff should be treated as the current semantic state, not as a chronological transcript.

---

# 1. User and Working Preferences

## User

- Name: Ray Tayek
- GitHub: `rtayek`
- Primary repository: `https://github.com/rtayek/myclaw`
- Primary environment:
  - Windows 11
  - Eclipse
  - Git Bash
  - Gradle
  - Java 25
- Also uses:
  - WSL/Ubuntu
  - native Ubuntu
  - Python only when needed for machine learning
  - Ollama
  - Claude CLI
  - ChatGPT/Codex
  - Apache HTTP Server 2

## Accessibility Context

Ray is nearly 80 and has low vision. He also sometimes uses hearing aids.

Accessibility is therefore not a decorative feature or distant goal. It is central to the product and to the development workflow.

Important accessibility preferences:

- large, scalable text;
- readable layouts;
- high contrast;
- minimal clutter;
- keyboard navigation;
- visible status feedback;
- support for screen readers;
- eventual speech input and speech output;
- no important state conveyed only by color;
- browser and desktop interfaces should use consistent terminology.

## Coding Preferences

Ray strongly prefers:

- code as the primary documentation;
- tests as the functional specification;
- expressive names instead of explanatory comments;
- minimal comments;
- package-private visibility unless wider visibility is necessary;
- small, focused abstractions;
- no unnecessary framework architecture;
- paste-safe shell commands;
- Git commits through a message file or here-document, not multi-line `git commit -m`;
- one Gradle project per Eclipse project;
- practical Windows-compatible workflows.

Paste-safe Git commit style:

```sh
cat > commit-message.txt <<'EOF'
Commit title

Commit body.
EOF

git add -A
git commit -F commit-message.txt
rm commit-message.txt
```

Do not push unless Ray explicitly asks.

---

# 2. Product Vision

The current product vision is:

> MyClaw is an accessible native Java desktop workbench for local and command-line AI systems, designed for readable conversations, transparent execution, and conversion of chats into durable personal knowledge.

The expanded web-and-desktop vision is:

> MyClaw will be an accessible AI workbench that users can experience through the web and then install as a full desktop application.

The browser version is not intended initially to replace the desktop application.

The intended product relationship is:

```text
Public website / web demonstration
            ↓
User experiences MyClaw
            ↓
User downloads MyClaw Desktop
            ↓
Full local and cloud AI workbench
```

The desktop application remains the serious product because it can support:

- local Ollama models;
- command-line AI tools;
- cloud AI providers;
- local files;
- private transcripts;
- detachable windows;
- speech input and speech output;
- durable conversations;
- project knowledge;
- later knowledge extraction and chat management.

The browser version should provide a genuine experience of the product without necessarily reproducing every feature.

---

# 3. Current Repository State

Repository:

```text
https://github.com/rtayek/myclaw
```

Recent known pushed commits include:

```text
1a6399cc6988121df495685c418fe0b4fc0c4bd8
added script to remove gradle build folder.

b7265c6c22e740cc7f749a7e207422804e0007d7
added zoom and detach to ui.

89ba53c30166a9468b5fefdf1b55c1ed0a4119e5
Polish desktop transcript presentation

2b0ad51301d9c8d587f326244d828aed5b33180c
Describe myclaw product vision

f118cb...
Organize tests by execution category
```

The exact current `main` branch should be inspected before making changes.

## Source Roots

Current source/test roots:

```text
src/
tst/unit
tst/integration
tst/smoke
tst/slow
```

## Packages

Known packages:

```text
myclaw.application
myclaw.backend
myclaw.cli
myclaw.desktop
myclaw.execution
myclaw.tools
myclaw.transcript
```

## Names That Must Be Preserved

Do not remove or rename without explicit discussion:

```text
Probe
probe
prbe
```

The typo alias `prbe` is intentional compatibility baggage and must remain.

## Gradle Build Directory

Gradle output is redirected to:

```groovy
layout.buildDirectory = layout.projectDirectory.dir('.gradle-build')
```

There is a cleanup script:

```text
clean-gradle.sh
```

It stops Gradle and removes `.gradle-build`.

This exists because Gradle cleanup was unreliable while Eclipse was open on Windows.

The script works reliably while Eclipse remains open.

Do not casually revert the project to `build/`.

## Eclipse

- Eclipse output is `bin/`.
- `.classpath` does not include `.gradle-build`.
- `.project` is a plain Java project rather than a Buildship-managed project.
- Ray uses Eclipse as the primary IDE.

---

# 4. Existing Desktop Application

The desktop client is currently Swing.

The Swing desktop application is the primary product today.

Known current classes reviewed recently include:

```text
MyClawDesktopFrame.java
TranscriptView.java
DesktopErrorFormatter.java
```

## Current Desktop Features

The current Swing client includes:

- backend selection;
- font-family selection;
- font-size selection;
- transcript display;
- prompt editor;
- Send action;
- Clear action;
- status label;
- progress indicator;
- asynchronous prompt execution through `SwingWorker`;
- themes through FlatLaf;
- keyboard shortcuts;
- transcript zoom;
- detachable transcript;
- reattachable transcript;
- help and About dialogs;
- accessibility metadata;
- clipboard support.

## Prompt Sending

Known behavior:

- Ctrl+Enter sends.
- Double Enter may also send.
- Request execution is asynchronous.
- UI state changes during work.
- Responses and errors are appended when the `SwingWorker` completes.

## Zoom

Zoom currently supports:

- Ctrl+mouse wheel over transcript or prompt;
- Ctrl+=;
- Ctrl++;
- Ctrl+-;
- Ctrl+0;
- a font-size combo list, roughly 14 through 48;
- reset size 18.

The transcript-specific key bindings are needed because the transcript can be detached into another window.

## Transcript Detach/Reattach

Current behavior:

- the same live `TranscriptView` and scroll pane are moved;
- no transcript copy is created;
- a nonmodal `JDialog` hosts the detached transcript;
- the main window shows a placeholder with a Reattach button;
- closing the dialog reattaches;
- menu or Ctrl+D toggles detach/reattach.

## Accessibility Direction

The desktop should support both:

### Blind or visually impaired users

- screen-reader support;
- accessible names and descriptions;
- keyboard navigation;
- scalable text;
- strong contrast;
- spoken responses;
- predictable focus.

### Deaf or hard-of-hearing users

- visible state;
- complete transcripts;
- speech-to-text where useful;
- no audio-only information.

Long-term speech goals:

- speech-to-text for dictating prompts;
- editable transcription before sending;
- text-to-speech per assistant response;
- play, pause, stop;
- speech rate and voice controls;
- possible sentence highlighting later.

Core interaction principle:

> Every important interaction should work through typing, speaking, listening, or reading.

---

# 5. Recent Desktop Architecture Work

A recent pass centralized actions using Swing `Action` objects.

Known actions include or were planned to include:

- Send
- Clear Transcript
- Detach/Reattach Transcript
- Copy Transcript
- Focus Prompt
- Show Keyboard Shortcuts
- Show About
- Zoom In
- Zoom Out
- Reset Text Size

Menus were organized around these actions.

Known shortcut direction:

```text
Ctrl+Enter      Send
Ctrl+D          Detach/Reattach Transcript
Ctrl+L          Focus Prompt
Ctrl+Shift+C    Copy Transcript
Ctrl+= / Ctrl++ Zoom In
Ctrl+-          Zoom Out
Ctrl+0          Reset Text Size
F1              Keyboard Shortcuts
```

The action-centric design was considered successful because:

- buttons, menus, and key bindings can share state;
- enabled/disabled state stays synchronized;
- action metadata provides a seam for accessibility and help;
- future speech commands could invoke the same actions.

---

# 6. Claude Desktop Review

Claude performed a review of the current desktop code without changing it.

## Confirmed Strengths

Claude identified these strengths:

- actions are created once and reused;
- Send and Clear enabled state is controlled through the actions;
- transcript roles are not indicated by color alone;
- detach and reattach reuse the live component;
- both dialog close and toggle routes converge on reattach logic;
- keyboard shortcut help matches actual bindings;
- detached transcript zoom works because relevant bindings follow the component;
- `ClipboardWriter` is a useful small injected seam;
- `SwingWorker.done()` is a natural future seam for speech output.

## Defects Found

### Zoom Discoverability

Zoom actions existed and had shortcuts but were not in a menu.

A mouse user who did not know the shortcut could only use the font-size combo.

Recommended correction:

```text
View
└── Zoom
    ├── Zoom In
    ├── Zoom Out
    └── Reset Text Size
```

Reuse the existing actions.

### Send Accessibility State

When working:

```text
Send → Working...
```

The visible button text changed, but the accessible name remained `Send`.

A screen-reader user might not hear that a request was in progress.

Recommended correction:

- update accessible name or description while working;
- restore it when ready or failed.

### Duplicate Tooltips

Send and Clear tooltips were assigned in more than one place.

Recommended correction:

- keep the string in one location.

### Label Association

Visible labels for:

```text
Backend:
Font:
Size:
```

were not programmatically linked with `setLabelFor(...)`.

Accessible names partly compensated, but proper label relationships were missing.

### Detach/Reattach Status

Clear reported status:

```text
Transcript cleared
```

Detach and reattach did not report status.

Recommended messages:

```text
Transcript detached
Transcript reattached
```

### About Dialog Font

Keyboard Shortcuts used the selected text font.

About used the look-and-feel default font.

This is inconsistent and potentially unreadable at large selected sizes.

### Fixed Pixel Sizes

Potential issue, not confirmed:

```text
setMinimumSize(900, 650)
promptScroll.setPreferredSize(900, 210)
```

These should be manually checked at:

```text
18
28
36
48
```

### Shortcut Binding Duplication

Zoom shortcuts were registered through multiple mechanisms:

- `Action.ACCELERATOR_KEY`;
- per-component bindings;
- root-pane bindings.

The prompt-specific zoom binding was probably redundant because the prompt never leaves the main frame.

The transcript-specific binding is needed for detached operation.

### Button Text Fighting Action Text

Some buttons use an action and immediately override the button text.

Examples conceptually:

```java
clearButton.setAction(clearTranscriptAction);
clearButton.setText("Clear");
```

and:

```java
new JButton(detachTranscriptAction);
setText("Reattach");
```

This works but can become fragile.

### `dispose()` Observation

There is cleanup in an overridden `dispose()` method.

Because the frame uses `EXIT_ON_CLOSE`, real application shutdown may not follow the same path as tests that explicitly call `dispose()`.

This is an architectural observation, not an urgent bug, since process exit cleans up the detached dialog anyway.

## Missing Tests Identified

Claude suggested tests for:

- Ctrl+wheel zoom versus ordinary wheel scrolling;
- Zoom menu discoverability;
- detach/reattach status;
- accessible descriptions;
- focus traversal;
- About dialog scaling;
- detached transcript during active request;
- `setLabelFor(...)`.

## Current Corrective-Pass Handoff

A focused handoff was prepared for Claude to implement:

1. add Zoom submenu;
2. add detach and reattach status;
3. add `setLabelFor(...)`;
4. scale About dialog;
5. keep Send accessibility state accurate;
6. remove duplicate tooltip assignments;
7. remove redundant prompt zoom binding;
8. add focused tests, especially wheel regression tests.

This pass should remain narrow.

Do not mix it with web architecture work.

---

# 7. Backend Concepts

The term “backend” is overloaded and must be used carefully.

In MyClaw there are at least three layers:

## AI Provider Adapter

An implementation that knows how to talk to one model provider or runtime.

Examples:

```text
ClaudeCliBackend
OllamaCliBackend
OllamaHttpBackend
OpenAiHttpBackend
SocketBackend
RemoteMyClawBackend
```

## Application Routing Layer

The existing or evolving MyClaw application layer chooses a configured backend and normalizes requests, responses, and failures.

Likely central concept:

```text
PromptService
```

The application layer should decide which `AiBackend` implementation to use.

## Public HTTP Server

A server-facing adapter exposes MyClaw application services to a browser or remote client.

This is not itself an AI backend.

It translates:

```text
HTTP + JSON
```

into calls to:

```text
PromptService
```

and returns normalized responses.

---

# 8. Backend Abstraction Direction

A stable abstraction should hide transport details.

Conceptually:

```java
interface AiBackend {
    AiResponse generate(AiRequest request);
}
```

Exact method names and current interfaces must be inspected in the repository before changing anything.

Transport-specific behavior should live in implementations.

Examples:

```text
claude-local  → exec Claude CLI
glm-local     → HTTP to Ollama
openai-cloud  → HTTPS to OpenAI
office-model  → socket or remote HTTP
```

The browser should not know whether a selected backend uses:

- `ProcessBuilder`;
- local HTTP;
- a socket;
- a remote HTTP service;
- a cloud API;
- another MyClaw server.

Avoid one giant backend that “guesses” the protocol.

Prefer explicit configured backend implementations and polymorphism.

---

# 9. Ollama Direction

Ollama currently or previously has been invoked through CLI integration.

A useful future improvement is:

```text
OllamaHttpBackend
```

Ollama exposes a local HTTP API, normally on:

```text
http://localhost:11434
```

Benefits of HTTP integration:

- avoids launching a process for every prompt;
- supports cleaner request/response handling;
- prepares for streaming;
- prepares for model listing and management;
- prepares for remote Ollama servers;
- fits naturally behind `AiBackend`.

Do not build JSON through raw string concatenation.

Use a JSON library because escaping only quotation marks is incorrect for:

- newlines;
- backslashes;
- tabs;
- control characters.

Also, the Ollama API response body is JSON.

The assistant text must be parsed from the response object rather than returning the entire JSON document.

Existing known local model:

```text
glm4:9b
```

Ray established earlier that this model fits entirely in the RTX 4060 Ti 8 GB GPU and runs fully on GPU.

Machine:

- Intel i7-13700F;
- 48 GB RAM;
- RTX 4060 Ti 8 GB.

A smaller model such as `llama3.2:1b` may be useful for a fast teaser or test, but the exact available model list should be checked at implementation time.

---

# 10. Desktop-to-Web Product Idea

Ray’s idea evolved from applets to a browser-accessible product teaser.

Applets are no longer viable.

The current idea is:

- keep the full product as a native desktop application;
- provide a browser experience that demonstrates MyClaw;
- let users try a simple model or AI connection;
- provide an installer for the desktop product;
- use the web experience partly for promotion and discovery.

The browser experience should help a user understand:

- the transcript;
- the prompt workflow;
- backend selection;
- zoom;
- keyboard navigation;
- accessibility;
- visible status;
- the relationship between web demo and desktop product.

The web teaser does not initially need:

- arbitrary local file access;
- full command execution;
- large model installation;
- durable knowledge;
- native detachable windows;
- every desktop feature.

---

# 11. Two Different Web Strategies Considered

## Browser-Local Model Strategy

A static site could download a small quantized model and run it in the visitor’s browser using technologies such as:

- Transformers.js;
- WebLLM;
- WebAssembly;
- WebGPU.

Advantages:

- no public inference server;
- no static home IP;
- no per-prompt cloud bill;
- prompts can remain in the visitor’s browser.

Disadvantages:

- model download size;
- browser compatibility;
- memory and GPU limitations;
- weaker devices may be slow;
- browser model may be too toy-like.

This remains a possible public teaser design.

## MyClaw Server Strategy

A browser talks to a MyClaw HTTP server.

That server talks to:

- local Ollama;
- Claude CLI;
- cloud APIs;
- socket services;
- remote AI servers.

This is the direction that currently appears most aligned with Ray’s thinking.

For an early test, the entire server stack can run on Ray’s PC.

---

# 12. Webswing Discussion

Claude proposed using Webswing to display the existing Swing application in a browser.

Webswing can be useful for a local or private experiment.

Architecture:

```text
Browser
   ↓
Webswing server
   ↓
existing Swing MyClaw process
   ↓
PromptService
   ↓
AiBackend
```

Important clarification:

The Java Swing process runs on the Webswing server.

It does not run inside the visitor’s browser.

Therefore, on a public Webswing deployment:

```text
localhost
```

means the hosting server, not the visitor’s computer.

Webswing does not eliminate:

- Java server hosting;
- model hosting;
- inference cost;
- server administration;
- authentication;
- abuse controls.

Potential Webswing value:

- quick proof of concept;
- reuse current Swing client;
- test browser presentation;
- test remote interaction;
- evaluate keyboard behavior;
- evaluate transcript zoom;
- evaluate clipboard;
- evaluate accessibility.

Do not assume Swing accessibility metadata automatically becomes excellent browser accessibility.

Test with NVDA and real keyboard navigation.

Webswing should be treated as a spike or optional deployment mechanism, not yet the permanent architecture.

---

# 13. Current Preferred Web Architecture

The architecture Ray most recently endorsed is:

```text
Browser
   ↓ HTTPS
Apache HTTP Server 2
   ↓ reverse proxy
MyClaw Java server
   ↓
PromptService
   ↓
AiBackend implementations
   ├── Ollama over HTTP
   ├── Claude or other tools through exec
   ├── cloud AI services over HTTPS
   ├── socket-based services
   └── remote MyClaw or model servers
```

## Apache’s Role

Ray already has Apache 2 running.

Apache can provide:

- HTTPS termination;
- public hostname;
- static HTML, CSS, and JavaScript;
- reverse proxying;
- Basic authentication for early tests;
- request logging;
- access control;
- possibly WebSocket proxying for streaming later.

Conceptual proxy configuration:

```apache
ProxyPass        /myclaw/api/ http://127.0.0.1:8081/api/
ProxyPassReverse /myclaw/api/ http://127.0.0.1:8081/api/
```

Exact configuration must be adapted to Ray’s actual Apache installation.

## Private Services

The MyClaw Java server should initially bind only to:

```text
127.0.0.1:8081
```

Ollama should remain private on:

```text
127.0.0.1:11434
```

Only Apache should be exposed publicly.

Do not expose Ollama directly.

Do not put the development machine in a router DMZ.

---

# 14. Static IP and Early External Testing

A static IP is not required for an initial test.

Possible progression:

1. test everything on localhost;
2. test from another machine on the LAN;
3. expose Apache through router port forwarding or a secure tunnel;
4. give access only to trusted testers;
5. stop external access when the test ends.

Ray already has experience with:

- Apache on Windows;
- port forwarding;
- public access;
- Basic authentication;
- a GoDaddy-hosted domain;
- local networking.

For a brief test, Ray can use the current public IP address.

If it changes, the tester can be given the new address.

Dynamic DNS could provide a stable hostname later.

A temporary tunnel such as Cloudflare Tunnel or Tailscale Funnel was also discussed, but Ray’s existing Apache and router setup may make direct testing reasonable.

Security principles:

- expose only Apache;
- protect the test with authentication;
- use HTTPS if possible;
- limit access;
- do not expose local provider credentials;
- limit prompt and response size;
- shut the route down after the test;
- monitor CPU, GPU, and memory.

---

# 15. Java Servlet Discussion

A Java servlet is a reasonable implementation for the HTTP boundary around `PromptService`.

The servlet should remain thin.

Responsibilities:

- accept HTTP requests;
- parse JSON;
- validate input;
- call `PromptService`;
- map application failures to HTTP status and JSON;
- return normalized results;
- later support streaming or cancellation if justified.

The servlet should not contain:

- provider selection logic;
- Ollama logic;
- Claude CLI logic;
- cloud-specific routing;
- command-building details.

Those belong behind the existing application and backend abstractions.

Possible endpoints:

```text
GET  /api/health
GET  /api/backends
POST /api/chat
```

Possible conceptual request:

```json
{
  "backend": "glm-local",
  "prompt": "Explain recursion."
}
```

Possible conceptual response:

```json
{
  "backend": "glm-local",
  "text": "..."
}
```

The exact DTO shape should derive from existing application request and response models rather than inventing an unrelated duplicate model.

---

# 16. Tomcat, Jetty, and Server Choices

Tomcat remains a valid choice.

Nothing “happened” to Tomcat.

The current distinction is operational style.

## Traditional Tomcat

```text
Apache HTTP Server
   ↓ reverse proxy
Tomcat
   ↓
myclaw.war
   ↓
Servlets
   ↓
PromptService
```

Advantages:

- conventional servlet container;
- WAR deployment;
- mature administration;
- good if hosting several Java web applications;
- familiar Java web model.

Costs:

- separate server installation;
- deployment and restart cycle;
- more configuration;
- more moving parts for a small proof of concept.

## Embedded Jetty

```text
java -jar myclaw-server.jar
```

The process contains:

- embedded Jetty;
- servlets;
- application services;
- backend implementations.

Advantages:

- simple Gradle application target;
- one Java process;
- direct constructor injection;
- easy integration testing;
- easy start and stop;
- no separate WAR deployment.

## Embedded Tomcat

Also possible.

This gives a self-contained Java application while using Tomcat as the embedded container.

## Recommendation So Far

For the smallest proof of concept:

- embedded Jetty or embedded Tomcat is likely simplest;
- traditional Tomcat is still entirely reasonable if Ray prefers it.

No final server-container decision has been made.

Do not choose casually without first examining:

- current Gradle structure;
- Ray’s familiarity;
- desired packaging;
- whether a WAR is useful;
- whether multiple Java web apps are likely;
- testing ergonomics;
- future streaming needs.

---

# 17. Possible Initial HTTP Server Components

A minimal server might contain:

```text
MyClawServerMain
HealthServlet
BackendsServlet
ChatServlet
ChatRequest
ChatResponse
ErrorResponse
```

Potential responsibilities:

## `MyClawServerMain`

- create application services;
- configure backend registry;
- start embedded server;
- bind localhost;
- configure shutdown;
- log the local URL.

## `HealthServlet`

- return basic health;
- possibly report application version;
- avoid exposing secrets or detailed internal state.

## `BackendsServlet`

- list configured backends;
- include user-facing names;
- possibly include availability state;
- avoid exposing credentials or command paths.

## `ChatServlet`

- parse request;
- validate backend and prompt;
- call `PromptService`;
- return normalized response;
- handle expected and unexpected failures distinctly.

## JSON Library

Use a real JSON library.

Candidates might include:

- Jackson;
- Gson;
- JSON-B.

No choice has been made.

Prefer one that:

- works cleanly with records or DTOs;
- has straightforward Gradle support;
- is easy to test;
- does not drag in unnecessary framework infrastructure.

---

# 18. Web Client Direction

A real web client would eventually use:

```text
HTML
CSS
JavaScript or TypeScript
```

It would call the MyClaw server over HTTP or WebSocket.

The web client should be independent of provider details.

It should reuse product language from the desktop:

- Transcript
- Prompt
- Backend
- Model
- Send
- Clear
- Zoom
- Keyboard Shortcuts
- Accessibility Help
- Read Aloud
- Dictate

The first browser client can be very small.

Suggested initial controls:

- backend selector;
- transcript;
- prompt editor;
- Send;
- Clear;
- text-size control;
- status;
- download desktop link.

The web client could be served by Apache as static files.

The MyClaw Java server would provide only the API.

---

# 19. Accessibility Requirements for the Web Client

The web client must use semantic browser-native accessibility.

Important requirements:

- real HTML controls;
- programmatic labels;
- visible focus;
- logical tab order;
- keyboard operation;
- screen-reader-friendly status updates;
- scalable text;
- high contrast;
- no color-only meaning;
- transcript roles exposed as text;
- loading and error status announced;
- large controls;
- no canvas-only application rendering unless proven accessible.

Test with:

- NVDA;
- keyboard only;
- browser zoom;
- 200% or higher text scaling;
- high-contrast settings;
- large response transcripts;
- slow response behavior;
- errors;
- focus restoration after sending.

This is one reason a native HTML web client may ultimately be preferable to remote Swing rendering through Webswing.

---

# 20. Security Model

The public browser should never directly receive:

- cloud API keys;
- command paths;
- local filesystem access;
- raw execution capabilities;
- Ollama’s unrestricted local port;
- shell access.

The Java server should mediate all provider access.

Suggested early safeguards:

- Apache Basic authentication;
- HTTPS;
- allowed backend list;
- prompt-size limit;
- response-size or token limit;
- request timeout;
- one-request-at-a-time or small concurrency limit;
- server-side logging without storing secrets;
- rate limiting later;
- explicit denial of arbitrary commands;
- localhost binding for Java and Ollama.

Cloud credentials should remain on the server.

Do not allow the browser to supply arbitrary executables, commands, URLs, or model server addresses during the first test.

---

# 21. Relationship Between Desktop and Web

Desired structure:

```text
Swing or JavaFX desktop ─┐
                         ├── PromptService → AiBackend
Web HTTP adapter ────────┘
```

More fully:

```text
Desktop UI
   ↓
PromptService
   ↓
AiBackend

Browser
   ↓
HTTP/JSON
   ↓
Servlet or HTTP controller
   ↓
PromptService
   ↓
AiBackend
```

The desktop and web clients should share:

- application services;
- backend configuration concepts;
- request models where practical;
- response models where practical;
- error categories;
- product terminology;
- backend registry;
- tests for provider behavior.

They should not necessarily share UI code.

The Swing or JavaFX desktop UI should not be compiled into the browser.

---

# 22. Swing Versus JavaFX

A possible move from Swing to JavaFX was discussed.

There is no fundamental reason MyClaw could not move to JavaFX.

Potential benefits:

- CSS styling;
- scalable layouts;
- property binding;
- richer message presentation;
- cleaner dialogs and popovers;
- future Markdown and code rendering;
- better animation or transitions.

Migration costs:

- rewrite current desktop UI;
- retest transcript;
- retest zoom;
- retest detach;
- retest keyboard shortcuts;
- retest focus;
- retest dialogs;
- retest accessibility;
- rebuild desktop tests.

The primary concern is real accessibility behavior, not API availability.

Before switching, a small JavaFX feasibility spike was recommended:

- keep current Swing app;
- build a minimal JavaFX client;
- reuse `PromptService`;
- test backend selector, transcript, prompt, Send, zoom;
- test NVDA;
- test 18/28/36/48 sizes;
- test selection and copy;
- test long replies;
- test detached window behavior;
- test Gradle packaging.

No decision has been made to switch.

Do not mix a JavaFX rewrite with the web-server proof of concept.

---

# 23. Current Recommended Development Sequence

The next work should be split into distinct changes.

## Change A: Finish Desktop Corrective Pass

Keep this narrow.

Expected items:

- View → Zoom submenu;
- detach/reattach status;
- `setLabelFor(...)`;
- About font scaling;
- accurate Send accessibility state;
- duplicate tooltip cleanup;
- redundant binding cleanup;
- wheel regression tests.

Do not mix this with server work.

## Change B: Inspect Existing Application and Backend Boundaries

Before coding the server:

- inspect `PromptService`;
- inspect existing `AiBackend`;
- inspect backend selection;
- inspect request/response types;
- inspect error handling;
- inspect CLI integration;
- inspect tests.

Determine how to expose existing behavior without duplication.

## Change C: Add Ollama HTTP Adapter

Potentially add:

```text
OllamaHttpBackend
```

Requirements:

- use Java `HttpClient`;
- use real JSON serialization;
- parse normalized response text;
- timeouts;
- availability/error handling;
- integration tests against a fake local HTTP server;
- no dependence on a real Ollama process in normal tests;
- real Ollama smoke test only when explicitly requested.

## Change D: Add Local MyClaw HTTP Server

Initial endpoints:

```text
GET /api/health
GET /api/backends
POST /api/chat
```

Requirements:

- localhost binding;
- thin HTTP adapter;
- reuse `PromptService`;
- integration tests with Java `HttpClient`;
- no provider logic in servlet;
- no authentication inside Java initially if Apache handles the private test;
- clear error JSON;
- clean startup and shutdown.

## Change E: Minimal Browser Client

Create a small accessible page:

- backend selector;
- transcript;
- prompt;
- Send;
- Clear;
- status;
- text size;
- desktop download link.

Serve static files through Apache.

## Change F: Apache Reverse Proxy

Proxy:

```text
/myclaw/api/
```

to the local Java server.

Protect with Basic authentication for early tests.

## Change G: LAN and External Test

Test in this order:

1. localhost;
2. another machine on LAN;
3. trusted external tester;
4. accessibility testing;
5. resource monitoring.

---

# 24. Suggested First Proof-of-Concept Success Criterion

The first meaningful success is:

> A remote user opens a browser, reaches MyClaw through Apache, selects an available backend, sends a prompt, receives a response from either a local Ollama model or a cloud-backed provider, and experiences an accessible interface that clearly leads to the full desktop application.

A smaller technical success is:

> A Java `HttpClient` integration test starts the local MyClaw server, calls `/api/backends`, calls `/api/chat` through a fake backend, verifies normalized JSON, and shuts the server down cleanly.

---

# 25. Open Questions

These questions remain unresolved.

## Server Container

Choose among:

- traditional Tomcat + WAR;
- embedded Tomcat;
- embedded Jetty;
- another small Java HTTP server.

Do not choose merely from fashion.

Evaluate operational simplicity and testing.

## JSON Library

Choose:

- Jackson;
- Gson;
- JSON-B;
- another small option.

## Streaming

Initial chat can be non-streaming.

Later choices:

- Server-Sent Events;
- WebSocket;
- chunked HTTP;
- polling.

Do not add streaming to the first proof of concept unless it is nearly free.

## Authentication

For early tests, Apache Basic authentication may be sufficient.

Later options:

- accounts;
- session authentication;
- OAuth;
- invitation tokens.

No need to solve public authentication yet.

## Conversation State

Initial API can be stateless:

```text
prompt in → response out
```

Later it may need:

- conversation ID;
- prior messages;
- backend-specific session;
- cancellation;
- retry;
- persistent transcript.

## Public Teaser Backend

Possible public choices:

- Ray’s home machine for short demos;
- hosted small model;
- browser-local model;
- simulated responses;
- limited cloud API;
- Webswing;
- native HTML client.

The near-term private test can run entirely on Ray’s PC.

## Licensing

If using Webswing or hosted model systems, check licensing before committing.

## Installer

A public website should eventually provide a desktop installer.

Installer technology has not yet been selected.

Possible future paths include:

- `jpackage`;
- MSI;
- platform-specific packaging.

---

# 26. Things Not to Do Yet

Avoid these until the basic web boundary works:

- full public launch;
- multi-user account system;
- unrestricted anonymous model use;
- arbitrary command execution from browser;
- direct browser-to-Ollama exposure;
- full conversation persistence;
- model installation UI;
- RAG;
- training or fine-tuning;
- speech implementation;
- Markdown renderer;
- code-block renderer;
- JavaFX rewrite;
- generalized docking;
- distributed agent framework;
- large plugin architecture;
- remote filesystem access;
- complex authorization;
- premature microservices.

The project has enough possible futures already. It does not require additional ornamental futures at this stage.

---

# 27. Commands and Workflow Expectations

Use paste-safe commands.

Likely project commands:

```sh
./clean-gradle.sh
./gradlew test
./gradlew integrationTest
./gradlew smokeTestClasses
./gradlew slowTestClasses
./gradlew runDesktop
```

Future server commands might become:

```sh
./gradlew runServer
```

Do not assume such a task exists until inspecting the repository.

Do not run real Claude or Ollama smoke tests unless Ray explicitly asks.

Do not modify GitHub directly unless Ray explicitly asks.

Do not push unless Ray explicitly asks.

---

# 28. Existing Durable Vision File

A separate vision document was created:

```text
myclaw-web-desktop-vision.md
```

It contains the broad product and architectural direction:

- web demonstration;
- desktop full product;
- shared `PromptService`;
- backend adapters;
- Apache front door;
- thin Java HTTP layer;
- accessibility;
- security;
- staged development;
- success criteria.

This handoff expands that document with repository state, recent desktop work, unresolved choices, and operational context.

---

# 29. Recommended Opening Prompt for the New Chat

Paste or upload this document, then use a prompt like:

> Please read this MyClaw handoff carefully. Start by inspecting the current GitHub repository and comparing the actual code to the handoff. Do not make changes yet. Identify the current application and backend abstractions, especially `PromptService`, `AiBackend`, backend selection, request/response types, and error handling. Then propose the smallest clean first step toward a localhost MyClaw HTTP server that can later sit behind Apache 2. Preserve the existing Swing desktop behavior and do not mix the server proof of concept with a JavaFX rewrite.

A narrower alternative:

> Please read this handoff and help me decide between traditional Tomcat, embedded Tomcat, and embedded Jetty for the first MyClaw HTTP proof of concept. Base the recommendation on the actual repository structure and Ray’s Windows/Eclipse/Gradle workflow. Do not change code yet.

---

# 30. Current Bottom Line

The current architectural direction is:

```text
Native desktop client
        │
        ├──────────────┐
        ↓              │
PromptService          │
        ↓              │
AiBackend              │
        ↓              │
Local and cloud AI     │
                       │
Browser                │
   ↓                   │
Apache 2               │
   ↓                   │
MyClaw HTTP adapter ───┘
```

The essential design decision is:

> The web client and desktop client should share the same MyClaw application and backend layer. The HTTP or servlet code should be only another adapter around that core.

The immediate practical path is:

1. finish the small desktop correction pass;
2. inspect the actual current application boundaries;
3. add or refine Ollama HTTP support;
4. add a minimal localhost Java HTTP server;
5. call it with integration tests;
6. place Apache in front;
7. add a tiny accessible browser client;
8. test with one trusted remote user.

That path preserves the existing desktop work while opening a credible route to a public web teaser and installer-driven product.
