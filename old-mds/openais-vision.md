# Manifold Vision

## Vision

Manifold will begin as an accessible AI cockpit and grow into a user-owned AI workspace and continuity layer backed by a durable, provider-independent record core.

Its first purpose is simple: make it easier to use different AI systems through one clear desktop interface designed for large text, keyboard control, touch, dictation, and spoken responses.

The cockpit is how the user interacts with AI. The harness is how Manifold connects models, tools, and context. The library is how the work survives.

The model is not the whole product. Manifold should control the relationship among the user, their context, their tools, and whichever local or cloud models are useful for the task.

The fundamental unit is the session, not the model. Models and backends can change; the session is the durable record of the user's work.

Every conversation handled through Manifold will be preserved locally as completely as Manifold can observe it. Imported conversations will be preserved as completely as their source permits, with their origin, acquisition method, and known limitations recorded.

As Manifold grows, these conversations will become part of a common library that can organize work by project, search across conversations, preserve provenance, support model evaluation, and export information in readable or structured forms.

Models will change. The user’s conversations, projects, preferences, evaluations, and accumulated context should remain stable and portable across them.

AI systems come and go. Manifold keeps the user’s work.

## Distinction

Agent and AI harnesses will be abundant. Manifold should not try to distinguish itself by offering the largest model catalog, the most elaborate autonomous agents, or the fastest router.

Its distinction should be the combination of:

* accessibility designed into the product architecture rather than added as decoration;
* durable continuity across models, providers, tools, and time;
* local ownership of conversations, project context, evaluations, and source artifacts;
* explicit, understandable control over context disclosure;
* honest provenance and preservation-fidelity claims;
* personal evaluation based on the user’s actual work rather than public benchmark rankings;
* a reusable record core that can serve Manifold and other AI clients;
* bounded, inspectable customization that can turn repeated natural-language instructions into reusable local behavior.

Most harnesses will optimize execution. Manifold should optimize continuity: what happened, what context was used, what was derived, what changed, what the user preferred, and how the work can be carried elsewhere.

Manifold should preserve work, not merely messages.

## Product Direction

Manifold should provide:

* one accessible interface for several AI backends;
* multiple independent sessions and windows that can be opened, arranged, detached, restored, and used across one or more monitors;
* keyboard, touch, voice, mouse, and trackpad interaction;
* large scalable text, reliable speech output, and truthful plain-language errors that can be read aloud, copied, and expanded to show technical detail;
* local preservation of conversations and source artifacts;
* explicit provenance, including provider, model, time, relevant settings when known, acquisition method, preservation fidelity, and whether execution was local or remote;
* explicit user control over which conversations, files, project materials, preferences, and other context are disclosed to each backend;
* traceable context bundles showing which records, memories, retrieved data, skills, and instructions were supplied to each invocation and why;
* project organization and cross-provider search;
* best-effort import of existing conversations in whatever forms providers make available;
* provider-independent storage and export;
* concise handoffs for continuing work with another AI;
* local reusable skills that can add selected instructions and procedures to a request;
* curated shared, project, and tool-specific memory stored in readable standard files;
* optional prompt-authored extensions that implement narrow application-owned contracts and can be inspected, tested, approved, stored, and executed locally;
* scheduled prompts and summaries when explicitly configured;
* supervised agent loops whose prompts, tool calls, changes, results, and continuation decisions remain visible;
* records of proposed actions, effective identities, approvals, executions, failures, and resulting artifacts without retaining reusable secrets;
* support for comparing models on the user’s own work rather than treating provider claims or public benchmarks as authoritative;
* preservation of useful execution evidence, such as latency, cost when known, local compute used, failures, retries, selected model, and user judgment of the result.

Local, open-weight, and cloud models should be treated as peers with different capabilities, costs, latency, privacy properties, and resource requirements. A local model should not be considered merely an inferior fallback, and an expensive frontier model should not be assumed to be the best choice for every task.

Manifold should help the user see AI as deployed work, not as a leaderboard. The relevant question is not only which model is strongest in isolation, but which combination of model, context, tool access, compute location, latency, cost, privacy, and reliability produces useful results under the user's constraints.

The accessible cockpit is the first product. Daily use of that cockpit builds the collection.

Reliable forward capture is foundational. Importing older conversations is valuable but secondary and necessarily limited by what their sources expose.

The conversation library is the stable context layer that makes the collection durable, searchable, portable, and increasingly useful as models and providers change. It should allow the user to reuse accumulated knowledge without surrendering that knowledge to any one model provider.

Over time, Manifold should support a personal evaluation loop grounded in the user’s actual tasks and outcomes. The purpose is not to maximize token volume or favor a fashionable model, but to help the user obtain the best useful result under their chosen constraints of quality, cost, latency, privacy, reliability, and local compute.

Future versions may route work among models, use a local or open-weight model for ordinary tasks, keep sensitive work close to the user's machine when practical, or escalate difficult work to a specialized or frontier model. That orchestration should be evidence-based, visible to the user, reversible when practical, and supported by contribution-level provenance when several models participate in one result.

Manifold should support a user who remains present and directs work step by step, but it should not require the user to watch every operation. Tasks and agent loops may run unattended when the user explicitly chooses a policy that permits it. Loops should be bounded by default through limits such as steps, time, cost, tools, files, or required approvals; those defaults may be relaxed deliberately for experiments or trusted workflows. Whether supervised or unattended, the resulting record should make the work understandable afterward.

## Record Core

The capture, persistence, provenance, hashing, import, and projection machinery should form a coherent record core beneath the cockpit and harness.

That core should own:

* sessions, turns, tool invocations, and attachments;
* native capture and imported material;
* immutable source artifacts whenever practical;
* content identity, hashes, and integrity checks;
* provenance and preservation-fidelity claims;
* context selections and disclosure records;
* proposed actions, approvals, executions, and results;
* identities and authorization references needed to explain an action, without storing reusable credentials;
* skill identities, versions, digests, sources, and dependencies when skills affect an invocation;
* generated-extension source, contract identity, schema fingerprint, tests, approval state, version, and execution history;
* relationships among original records, derived artifacts, evaluations, and exports;
* provider-independent storage and verification.

Original records should remain authoritative. Summaries, tags, indexes, embeddings, handoffs, extracted decisions, memories, evaluations, generated extensions, and other interpretations should remain derived projections linked to their exact sources.

Hashes can establish byte identity, detect accidental change, support deduplication, and link derived records to exact source artifacts. They cannot prove that a source was complete, accurate, or trustworthy. Content integrity and capture provenance must therefore remain distinct concepts.

The record core should use provider-neutral concepts such as sessions, messages, artifacts, activities, agents, tool invocations, execution metadata, context bundles, derivations, imports, verification, and export. Provider adapters should translate external events into that common model rather than allowing provider-specific assumptions to define the foundation.

Manifold should use the record core first as an embedded component. Its boundaries should nevertheless permit later use as a local service, portable repository format, or backend for other AI cockpits, harnesses, command-line tools, editor integrations, and import utilities.

The cockpit attracts daily use. The record core makes the resulting work survive.

## Context, Memory, and Evaluation

Agent memory should not be treated as the permanent record. It is a task-specific projection over preserved history.

Short-term conversational windows, extracted facts, summaries, retrieved project material, skills, and context selected for a particular model call should all remain distinguishable. Manifold should preserve the evidence from which those projections were derived so they can be inspected, rebuilt, corrected, replaced, or deleted.

A context bundle should record not only what was sent to a model, but also how each item entered the bundle. Context may have been selected directly by the user, retrieved by the application, recalled from memory, supplied by a skill, returned by a tool, or inherited from a project policy. Those mechanisms have different meanings and should not be collapsed into one opaque prompt.

Memory services may provide recent messages, confirmed facts, prior decisions, relevant project material, or context constrained by a token budget. Their output should remain linked to the durable records from which it was selected or derived.

Tools and MCP integrations represent actions or access to external systems. Their use should be recorded as activities with inputs, outputs, effective identity, authorization or human approval when applicable, latency, failures, retries, and resulting artifacts. Secret values should be filtered or referenced, not preserved in reusable form.

Human approval should remain an explicit event. A record should distinguish what an agent proposed, what the user approved, modified, or rejected, and what the system ultimately executed.

Automated evaluations should be preserved as derived assertions, not promoted to truth. Each evaluation should identify its subject, criteria, evaluator, model or software version, result, and sources. User judgment should remain distinguishable from model judgment.

These services may later be exposed to other harnesses. An external agent framework could use Manifold to retrieve recent context, relevant project history, confirmed decisions, or bounded context while the record core preserves exactly what evidence supported the returned bundle.

## Bounded Extensions

Repeated instructions may sometimes be better represented as reusable local behavior than as prompt text interpreted again on every invocation.

Manifold may therefore support prompt-authored extensions for narrow operations such as filtering sessions, classifying records, ranking candidate context, validating imports, formatting reports, selecting notifications, or applying handoff inclusion rules.

The application should define each extension point as a small, stable, application-owned contract. In Java, interfaces should express behavior, records should represent immutable structured data, enums should represent finite choices, and sealed hierarchies should represent closed alternatives. Broad service facades, persistence entities, transport objects, framework types, arbitrary maps, and unrestricted host access should not be exposed merely because reflection makes that technically convenient.

Extension contracts should carry clear domain vocabulary, explicit limits, and independent tests. Each incompatible contract version should have a distinct identity or schema fingerprint so an old persisted extension cannot silently execute against a changed capability surface.

Authoring and execution should remain separate phases. During authoring, a model may receive the contract, prompt, selected examples, limits, and tests and produce a script or other executable artifact. The result should then be inspectable, testable, approvable, versioned, and revocable. During ordinary execution, the approved extension should run locally through the same bounded contract without requiring another model call.

A generated extension is not an authority merely because it compiles or passes representative tests. It remains a derived artifact whose prompt, schema, examples, generating model, source digest, tests, approval, and later executions should be preserved. Execution should be sandboxed or capability-limited, resource-bounded, observable, and subject to rollback or deactivation.

This mechanism belongs above the record core, within derived services and customization. The record core itself should remain deterministic and independent of generated guest-language behavior.

The first experiment should use a deliberately small contract, such as a rule over a read-only session summary, and compare the result with a plain Java implementation and a Markdown skill before any broader adoption. Graal Script Agent is one possible implementation technology, not a foundational dependency or product requirement.

## First Goal

The first useful version should be practical for daily use.

A user should be able to choose an AI, enter a prompt by typing or speaking, read or hear the response, and reopen yesterday’s preserved conversation today.

The system should store the complete observable conversation through a common local model. Imported material should remain distinguishable from native capture, with the original artifact preserved unchanged whenever possible.

The first version does not require automatic routing, fine-tuning, multi-agent orchestration, prompt-authored extensions, or a separately deployed record service. It should establish clean record-core boundaries and preserve enough information to support later evaluation of model quality, latency, cost, privacy, and reliability on the user’s actual work.

Skills, curated memory, scheduling, agent loops, and bounded extensions are later capabilities built on the same session and event record. Their first implementations should be small and inspectable: manually selected local skills, readable Markdown memory, explicitly configured schedules, a coding loop in which an external coding tool edits files and the user reviews the resulting diff, and a read-only generated rule over a narrow application-owned contract.

## Guiding Principles

If an essential function cannot be performed through large readable text, keyboard, touch, or voice, it is incomplete.

Every saved conversation should belong to the user, remain on the user’s machine, and be readable in a standard form without requiring Manifold.

The client should not need to know whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.

Capture should be independent of presentation. The original session record should be preserved, while conversation, detail, and raw views remain projections of that record.

Manifold should never claim that an imported conversation is complete unless the source establishes that. It should preserve what it receives, record what is known, and identify what may be missing.

Personal state and AI-generated interpretations should be visible, distinguishable from source records, editable, exportable, and deletable by the user. Owning data includes the right to erase it, not merely to keep it.

Summaries, tags, indexes, extracted decisions, embeddings, project notes, memories, generated extensions, and other interpretations should remain derived artifacts. They should never silently rewrite the original captured or imported record.

Work should be organized around projects, sessions, and user goals, not provider names. A backend is a worker inside the project, not the place where the work belongs.

The user’s context is part of the user’s durable intellectual work. Manifold should not silently expose, duplicate, or bind that context to a provider. Context disclosure should be deliberate, inspectable, and limited to what the task requires.

Privacy and capability controls should be explicit and enforced by the system, not merely suggested by the interface.

AI-authored behavior should operate only through explicit application-owned capabilities. Prompt convenience must not silently become unrestricted access to the application, filesystem, network, credentials, or host runtime.

Model choice should follow the user’s task, constraints, and evidence from real use. Manifold should not privilege a provider, model family, or deployment method merely because it is fashionable, expensive, or currently at the top of a public benchmark.

Evaluation should measure useful outcomes rather than activity alone. More tokens, more agents, more energy, or more remote compute do not by themselves mean more value.

Present-user control and bounded execution are useful defaults, not permanent prohibitions. The user should be able to choose broader execution policies, including unattended work, while retaining clear records, understandable limits, stop controls where practical, and reviewable results.

The cockpit must be pleasant and reliable enough for daily use. A library is only as complete as the conversations people choose to conduct through it.

## Success

Manifold succeeds first when it becomes a genuinely useful accessible AI cockpit for its developer and a small number of real users.

It succeeds in the longer term when it becomes the user-owned workspace and continuity layer through which people can use, compare, and change AI systems without surrendering ownership of their work.

The record core succeeds when Manifold depends on it cleanly and other clients could use it without inheriting Manifold’s user interface or provider-specific assumptions.

Bounded extensions succeed when users can convert repeated instructions into understandable, testable, reusable local behavior without granting the generated code broader authority than the task requires.
