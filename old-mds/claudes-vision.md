# Manifold Switchboard Vision

## One sentence

Manifold Switchboard is an AI cockpit you can use without seeing well
or typing well, that preserves every conversation you hold through it
— completely, permanently, in a format you own.

## The problem

AI interfaces assume sharp eyes and fast hands: small text,
mouse-driven layouts, silent responses, and relentless visual
tracking. And the work itself is scattered across providers who guard
it: no API reaches your chat history, and exports are manual, lossy,
and getting harder to come by. What you made with AI is not, today,
yours.

## The promise

Every conversation held through Manifold Switchboard is captured the
moment it happens, at full fidelity, on your own machine — no export,
no permission, no vendor cooperation required. Own your conversations
on the way in, because getting them back out later is only going to
get harder.

## What makes it different

Harnesses are becoming abundant, and nearly all of them are built for
a task: their ideal end state is the agent working while you stop
watching. This one is built for a person who is present the whole
time and needs the interaction itself to be survivable. That pulls
the opposite way on almost every design decision — they add panes and
status to scan, this removes anything that must be scanned.

The second difference is that the archive is the product, not
exhaust. A tool sold by the seat has no reason to make your history
easy to take elsewhere. That is a conflict of interest, not a
capability gap, which is why it lasts.

Multi-backend support, local models, search, and handoffs are table
stakes, not the pitch. Any funded team could build them in a quarter.
None of them will build for people who cannot watch the screen.

## The product, in two stages

### Stage one: the cockpit

One accessible desktop app that talks to many AIs:

- Local models first. An open model on your own machine is the
  everyday default: no account, no metering, no network, nothing that
  can be shut off. Hosted models like Claude are one keystroke away
  when a task needs them.
- Never dead in the water. If the network drops or a quota runs out,
  the cockpit falls back to a local model and says so, rather than
  failing.
- Multiple independent sessions that can be opened, arranged,
  detached, restored, and used across one or more monitors.
- Large, scalable text everywhere.
- Keyboard, voice, touch, and mouse — any one of them alone is enough.
- Reachable from wherever you already are: a global hotkey sends the
  text in front of you to a session without hunting for a window.
- Speech in: prompt by dictation. Speech out: responses read aloud.
- Sound carries status. Waiting, finished, and failed are distinct
  audible cues, so progress does not depend on watching a screen.
- Truthful, plain-language errors that can be read aloud, with full
  technical detail available on request. No information trapped in a
  dialog.
- Every conversation saved locally with provenance — which AI, which
  model, when, and under what settings — and reopenable the next day.
  Nothing leaves your computer except the prompts you send.

### Stage two: the library

Daily use of the cockpit builds a collection. The library makes it
durable and useful:

- Search across every conversation, regardless of which AI answered.
- Organize conversations by project.
- Generate concise handoffs to continue work with another AI.
- Let the user see, for their own work, which model handled which
  session and how it went — not a benchmark, just their own history,
  because the only ranking that predicts value is the one built from
  tasks you actually ran.
- Keep summaries, tags, indexes, extracted decisions, and other
  interpretations separate from the original captured record.
- Import old conversations, best-effort and last. Native capture and
  imported material stay clearly distinct; the original import file is
  kept unchanged so a better parser can re-read it later; known gaps
  are recorded. Forward capture is the foundation; import is a
  courtesy to history.

The transcript format carries provenance from day one, so the library
never needs a migration to exist.

## Who it is for

Anyone who needs their ears and voice to do what eyes and hands
usually do — whether from low vision, limited typing, or simply eyes
worn out by a long day — and anyone who wants to own their work with
AI instead of renting access to it. The developer is the first user:
this is built from lived experience, not a compliance checklist.

## First milestone

The developer runs Manifold Switchboard daily; a response can be read
aloud with one keystroke; yesterday's conversation can be reopened
today.

## What success looks like

Daily use by the developer and ten real users. Then hundreds to a few
thousand devoted users who depend on it. Depth over breadth.

## Deferred

Web demo, TV client, teaching features, multi-user service, and
automatic routing between local and hosted models. The core
abstractions keep those doors open; the accessible desktop comes
first.

Capture, persistence, and integrity are built as a self-contained
layer with no interface dependencies, documented schema, and stable
record identity — so it could be extracted for others later. It is
not published or promised as a library until someone other than the
developer needs it. The discipline is the point; the packaging can
wait.

Manifold Switchboard is an accessible interface that happens to speak
to many AIs — not an orchestration platform, not a compliance product,
not a code-auditing tool. That distinction is what keeps it buildable
by one person and useful to people no one else is building for.

## Guiding principles

- The fundamental unit is the session, not the model. Models and
  backends can change; the session is the durable record of the
  user's work.
- If it cannot be done by reading large text, pressing keys, and
  speaking, it is not done.
- The stored record is the source of truth, not a log written
  alongside it. It is append-only and content-hashed, so a session can
  be shown to be complete and unaltered rather than merely claimed to
  be.
- Capture is independent of presentation: the original session record
  is preserved, and conversation, detail, and raw views are
  projections of that record.
- Never claim an imported conversation is complete unless the source
  proves it: preserve what arrives, record what's known, flag what's
  missing.
- Work is organized around projects, sessions, and user goals, not
  provider names. A backend is a worker inside the project, not the
  place where the work belongs.
- The client should not need to know whether a backend uses a local
  process, local HTTP service, socket, remote server, or cloud API.
- Backends are disposable. The best model for a task will change
  several times a year, so adding, replacing, or retiring one should
  be a small, routine act that leaves saved sessions untouched.
- Everything the app stores about you, you can inspect, correct,
  export, or delete. Owning your data means the right to erase it,
  not only to keep it.
- Every saved conversation belongs to the user, on the user's
  machine, in a format they can read without us.
- The library is only as complete as the cockpit is pleasant. Making
  people want to live here is a data-integrity feature.