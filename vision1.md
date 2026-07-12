# MyClaw Web and Desktop Vision

## Vision

MyClaw will be an accessible AI workbench that users can experience through the web and then install as a full desktop application.

MyClaw will support coordinated AI teaching teams that adapt explanations, questions, exercises, and feedback to an individual learner’s knowledge, goals, pace, and accessibility needs.

The web version will provide a simple, approachable demonstration of the product. The desktop application will provide the complete experience, including local models, cloud AI services, command-line tools, private files, and richer accessibility features.

The web and desktop clients will share the same application concepts and backend abstractions rather than duplicating AI integration logic.

## Product Goal

A user should be able to:

1. Visit the MyClaw website.
2. Try a real or limited AI conversation in a browser.
3. Experience the interface, accessibility features, zoom, keyboard navigation, and general workflow.
4. Understand the difference between the web demonstration and the full desktop product.
5. Download and install MyClaw Desktop.
6. Use local Ollama models, command-line AI tools, and cloud AI providers through one consistent interface.

## Architectural Goal

MyClaw will separate user interfaces from AI execution.

```text
Desktop client ───────────────┐
                              │
Web client → HTTP API ────────┼── PromptService
                              │       ↓
Other future clients ─────────┘   AiBackend
                                      ├── Ollama over HTTP
                                      ├── Claude or other tools through exec
                                      ├── Cloud AI services over HTTPS
                                      ├── Socket-based model services
                                      └── Remote MyClaw servers
```

The client should not need to know whether a backend uses:

- a local process;
- a local HTTP service;
- a socket;
- a remote server;
- a cloud API.

Each backend implementation will hide its transport and provider-specific behavior behind a stable MyClaw abstraction.

## Web Deployment Goal

For an initial public or private test:

```text
Browser
   ↓ HTTPS
Apache HTTP Server
   ↓ reverse proxy
MyClaw web server
   ↓
PromptService
   ↓
AiBackend implementations
```

Apache will act as the public front door.

Apache may provide:

- HTTPS;
- authentication;
- static web content;
- reverse proxying;
- request logging;
- access control.

The MyClaw Java server will listen on a private local address and expose a small HTTP API.

Possible initial endpoints:

```text
GET  /api/health
GET  /api/backends
POST /api/chat
```

The Java server may use:

- embedded Jetty;
- embedded Tomcat;
- a traditional Tomcat deployment;
- another small Java HTTP or servlet container.

The servlet or HTTP layer should remain thin. It will translate HTTP and JSON into calls to the existing application layer.

## Web Experience

The web version should demonstrate the real MyClaw interaction model.

It should include:

- a readable transcript;
- a prompt editor;
- backend or model selection;
- Send and Clear actions;
- scalable text;
- keyboard navigation;
- visible status information;
- accessible labels and descriptions;
- concise help;
- an obvious path to download the desktop application.

The first web version does not need every desktop feature.

It may initially omit:

- local file access;
- arbitrary local command execution;
- large model downloads;
- detachable native windows;
- persistent personal knowledge;
- advanced project management;
- unrestricted usage.

The purpose of the first web version is to let users understand and experience MyClaw, not to reproduce the entire desktop system inside a browser.

## Desktop Experience

MyClaw Desktop will remain the complete product.

It may support:

- local Ollama models;
- command-line AI tools;
- cloud AI providers;
- private local files;
- durable conversations;
- local knowledge retrieval;
- speech input;
- speech output;
- advanced accessibility;
- multiple windows;
- richer project and transcript management.

The desktop application should use the same `PromptService`, request models, response models, and `AiBackend` abstractions as the web server.

## Accessibility Goal

Accessibility is a primary product requirement, not a later compatibility pass.

Every important interaction should be usable through:

- reading;
- typing;
- keyboard navigation;
- speech input;
- spoken output.

Important state must not be conveyed by color alone.

Web and desktop versions should use the same terminology, interaction patterns, and accessibility principles, even when their UI implementations differ.

## Security Goal

Only the public web entry point should be exposed to the internet.

The following should remain private unless deliberately configured otherwise:

- Ollama;
- local model ports;
- command execution services;
- MyClaw internal server ports;
- provider credentials;
- local files.

For early testing:

- Apache should expose only the required MyClaw path;
- the MyClaw server should bind to localhost;
- Ollama should remain on localhost;
- authentication should protect the test;
- external access should be limited to trusted users;
- usage limits should prevent accidental or abusive resource consumption.

## Development Strategy

The project should grow in small, testable stages.

### Stage 1: Local HTTP proof of concept

- Add an HTTP-facing MyClaw server.
- Reuse the existing `PromptService`.
- Expose health, backend-listing, and chat endpoints.
- Connect through Java `HttpClient` integration tests.
- Keep the server bound to localhost.

### Stage 2: Minimal browser client

- Serve a small HTML, CSS, and JavaScript interface.
- Connect it to the MyClaw HTTP API.
- Preserve product terminology and accessibility behavior.
- Test locally through Apache.

### Stage 3: Limited external test

- Reverse-proxy through Apache.
- Add HTTPS and authentication.
- Test with one or more trusted remote users.
- Observe latency, accessibility, reliability, and resource usage.

### Stage 4: Public teaser

- Add product information and download links.
- Limit backend usage.
- Present the web version as a demonstration.
- Direct serious users to MyClaw Desktop.

## Design Principles

- Keep the application core independent of Swing, JavaFX, servlets, and browser code.
- Treat transports as adapters, not application logic.
- Prefer explicit backend implementations over protocol guessing.
- Keep the HTTP layer thin.
- Reuse request, response, and error models where practical.
- Let tests define behavior.
- Prefer meaningful names over explanatory comments.
- Avoid infrastructure that is not yet needed.
- Preserve the option to use Swing, JavaFX, Webswing, Tomcat, Jetty, or a native web client without coupling the core to any one of them.

## Success Criteria

The first meaningful success will be:

> A remote user opens a browser, reaches MyClaw through Apache, selects an available backend, sends a prompt, receives a response from either a local Ollama model or a cloud-backed provider, and experiences an accessible interface that clearly leads to the full desktop application.

The larger success will be:

> MyClaw becomes one coherent, accessible interface for local models, command-line AI tools, and cloud AI services across desktop and web clients.
