# Manifold Vision

## Vision

Manifold will begin as an accessible AI cockpit and grow into a user-controlled AI harness backed by a durable, provider-independent library for a person’s work with AI.

Its first purpose is simple: make it easier to use different AI systems through one clear desktop interface designed for large text, keyboard control, touch, dictation, and spoken responses.

The model is not the whole product. Manifold should control the relationship among the user, their context, their tools, and whichever local or cloud models are useful for the task.

The fundamental unit is the session, not the model. Models and backends can change; the session is the durable record of the user's work.

Every conversation handled through Manifold will be preserved locally as completely as Manifold can observe it. Imported conversations will be preserved as completely as their source permits, with their origin, acquisition method, and known limitations recorded.

As Manifold grows, these conversations will become part of a common library that can organize work by project, search across conversations, preserve provenance, support model evaluation, and export information in readable or structured forms.

Models will change. The user’s conversations, projects, preferences, evaluations, and accumulated context should remain stable and portable across them.

## Product Direction

Manifold should provide:

* one accessible interface for several AI backends;
* multiple independent sessions and windows that can be opened, arranged, detached, restored, and used across one or more monitors;
* keyboard, touch, voice, mouse, and trackpad interaction;
* large scalable text, reliable speech output, and truthful plain-language errors that can be read aloud, copied, and expanded to show technical detail;
* local preservation of conversations and source artifacts;
* explicit provenance, including provider, model, time, relevant settings when known, acquisition method, preservation fidelity, and whether execution was local or remote;
* project organization and cross-provider search;
* best-effort import of existing conversations in whatever forms providers make available;
* provider-independent storage and export;
* concise handoffs for continuing work with another AI;
* support for comparing models on the user’s own work rather than treating provider claims or public benchmarks as authoritative.

Local, open-weight, and cloud models should be treated as peers with different capabilities, costs, latency, privacy properties, and resource requirements. A local model should not be considered merely an inferior fallback, and an expensive frontier model should not be assumed to be the best choice for every task.

The accessible cockpit is the first product. Daily use of that cockpit builds the collection.

Reliable forward capture is foundational. Importing older conversations is valuable but secondary and necessarily limited by what their sources expose.

The conversation library is the stable context layer that makes the collection durable, searchable, portable, and increasingly useful as models and providers change.

Future versions may route work among models, use a local model for ordinary tasks, or escalate difficult work to a specialized or frontier model. That orchestration should be evidence-based, visible to the user, and supported by contribution-level provenance when several models participate in one result.

## First Goal

The first useful version should be practical for daily use.

A user should be able to choose an AI, enter a prompt by typing or speaking, read or hear the response, and reopen yesterday’s preserved conversation today.

The system should store the complete observable conversation through a common local model. Imported material should remain distinguishable from native capture, with the original artifact preserved unchanged whenever possible.

The first version does not require automatic routing, fine-tuning, or multi-agent orchestration. It should preserve enough information to support later evaluation of model quality, latency, cost, privacy, and reliability on the user’s actual work.

## Guiding Principles

If an essential function cannot be performed through large readable text, keyboard, touch, or voice, it is incomplete.

Every saved conversation should belong to the user, remain on the user’s machine, and be readable in a standard form without requiring Manifold.

The client should not need to know whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.

Capture should be independent of presentation. The original session record should be preserved, while conversation, detail, and raw views remain projections of that record.

Manifold should never claim that an imported conversation is complete unless the source establishes that. It should preserve what it receives, record what is known, and identify what may be missing.

Personal state and AI-generated interpretations should be visible, distinguishable from source records, editable, exportable, and deletable by the user. Owning data includes the right to erase it, not merely to keep it.

Summaries, tags, indexes, extracted decisions, embeddings, project notes, and other interpretations should remain derived artifacts. They should never silently rewrite the original captured or imported record.

Work should be organized around projects, sessions, and user goals, not provider names. A backend is a worker inside the project, not the place where the work belongs.

Privacy and capability controls should be explicit and enforced by the system, not merely suggested by the interface.

Model choice should follow the user’s task, constraints, and evidence from real use. Manifold should not privilege a provider, model family, or deployment method merely because it is fashionable, expensive, or currently at the top of a public benchmark.

The cockpit must be pleasant and reliable enough for daily use. A library is only as complete as the conversations people choose to conduct through it.

## Success

Manifold succeeds first when it becomes a genuinely useful accessible AI cockpit for its developer and a small number of real users.

It succeeds in the longer term when it becomes the user-controlled harness and stable context layer through which people can use, compare, and change AI systems without surrendering ownership of their work.
