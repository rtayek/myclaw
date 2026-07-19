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
* a reusable record core that can serve Manifold and other AI clients.

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
* project organization and cross-provider search;
* best-effort import of existing conversations in whatever forms providers make available;
* provider-independent storage and export;
* concise handoffs for continuing work with another AI;
* support for comparing models on the user’s own work rather than treating provider claims or public benchmarks as authoritative;
* preservation of useful execution evidence, such as latency, cost when known, local compute used, failures, retries, selected model, and user judgment of the result.

Local, open-weight, and cloud models should be treated as peers with different capabilities, costs, latency, privacy properties, and resource requirements. A local model should not be considered merely an inferior fallback, and an expensive frontier model should not be assumed to be the best choice for every task.

Manifold should help the user see AI as deployed work, not as a leaderboard. The relevant question is not only which model is strongest in isolation, but which combination of model, context, tool access, compute location, latency, cost, privacy, and reliability produces useful results under the user's constraints.

The accessible cockpit is the first product. Daily use of that cockpit builds the collection.

Reliable forward capture is foundational. Importing older conversations is valuable but secondary and necessarily limited by what their sources expose.

The conversation library is the stable context layer that makes the collection durable, searchable, portable, and increasingly useful as models and providers change. It should allow the user to reuse accumulated knowledge without surrendering that knowledge to any one model provider.

Over time, Manifold should support a personal evaluation loop grounded in the user’s actual tasks and outcomes. The purpose is not to maximize token volume or favor a fashionable model, but to help the user obtain the best useful result under their chosen constraints of quality, cost, latency, privacy, reliability, and local compute.

Future versions may route work among models, use a local or open-weight model for ordinary tasks, keep sensitive work close to the user's machine when practical, or escalate difficult work to a specialized or frontier model. That orchestration should be evidence-based, visible to the user, reversible when practical, and supported by contribution-level provenance when several models participate in one result.

## Record Core

The capture, persistence, provenance, hashing, import, and projection machinery should form a coherent record core beneath the cockpit and harness.

That core should own:

* sessions, turns, tool invocations, and attachments;
* native capture and imported material;
* immutable source artifacts whenever practical;
* content identity, hashes, and integrity checks;
* provenance and preservation-fidelity claims;
* relationships among original records, derived artifacts, and exports;
* provider-independent storage and verification.

Original records should remain authoritative. Summaries, tags, indexes, embeddings, handoffs, extracted decisions, and other interpretations should remain derived projections linked to their exact sources.

Hashes can establish byte identity, detect accidental change, support deduplication, and link derived records to exact source artifacts. They cannot prove that a source was complete, accurate, or trustworthy. Content integrity and capture provenance must therefore remain distinct concepts.

The record core should use provider-neutral concepts such as sessions, messages, artifacts, tool invocations, execution metadata, derivations, imports, verification, and export. Provider adapters should translate external events into that common model rather than allowing provider-specific assumptions to define the foundation.

Manifold should use the record core first as an embedded component. Its boundaries should nevertheless permit later use as a local service, portable repository format, or backend for other AI cockpits, harnesses, command-line tools, editor integrations, and import utilities.

The cockpit attracts daily use. The record core makes the resulting work survive.

## First Goal

The first useful version should be practical for daily use.

A user should be able to choose an AI, enter a prompt by typing or speaking, read or hear the response, and reopen yesterday’s preserved conversation today.

The system should store the complete observable conversation through a common local model. Imported material should remain distinguishable from native capture, with the original artifact preserved unchanged whenever possible.

The first version does not require automatic routing, fine-tuning, multi-agent orchestration, or a separately deployed record service. It should establish clean record-core boundaries and preserve enough information to support later evaluation of model quality, latency, cost, privacy, and reliability on the user’s actual work.

## Guiding Principles

If an essential function cannot be performed through large readable text, keyboard, touch, or voice, it is incomplete.

Every saved conversation should belong to the user, remain on the user’s machine, and be readable in a standard form without requiring Manifold.

The client should not need to know whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.

Capture should be independent of presentation. The original session record should be preserved, while conversation, detail, and raw views remain projections of that record.

Manifold should never claim that an imported conversation is complete unless the source establishes that. It should preserve what it receives, record what is known, and identify what may be missing.

Personal state and AI-generated interpretations should be visible, distinguishable from source records, editable, exportable, and deletable by the user. Owning data includes the right to erase it, not merely to keep it.

Summaries, tags, indexes, extracted decisions, embeddings, project notes, and other interpretations should remain derived artifacts. They should never silently rewrite the original captured or imported record.

Work should be organized around projects, sessions, and user goals, not provider names. A backend is a worker inside the project, not the place where the work belongs.

The user’s context is part of the user’s durable intellectual work. Manifold should not silently expose, duplicate, or bind that context to a provider. Context disclosure should be deliberate, inspectable, and limited to what the task requires.

Privacy and capability controls should be explicit and enforced by the system, not merely suggested by the interface.

Model choice should follow the user’s task, constraints, and evidence from real use. Manifold should not privilege a provider, model family, or deployment method merely because it is fashionable, expensive, or currently at the top of a public benchmark.

Evaluation should measure useful outcomes rather than activity alone. More tokens, more agents, more energy, or more remote compute do not by themselves mean more value.

The cockpit must be pleasant and reliable enough for daily use. A library is only as complete as the conversations people choose to conduct through it.

## Success

Manifold succeeds first when it becomes a genuinely useful accessible AI cockpit for its developer and a small number of real users.

It succeeds in the longer term when it becomes the user-owned workspace and continuity layer through which people can use, compare, and change AI systems without surrendering ownership of their work.

The record core succeeds when Manifold depends on it cleanly and other clients could use it without inheriting Manifold’s user interface or provider-specific assumptions.