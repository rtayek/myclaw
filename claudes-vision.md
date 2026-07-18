# Manifold Switchboard Vision

## One sentence

Manifold Switchboard is an AI cockpit you can use without seeing well
or typing well, that preserves every conversation you hold through it
— completely, permanently, in a format you own.

## The problem

AI interfaces assume sharp eyes and fast hands: small text,
mouse-driven layouts, silent responses. And the work itself is
scattered across providers who guard it: no API reaches your chat
history, and exports are manual, lossy, and getting harder to come by.
What you made with AI is not, today, yours.

## The promise

Every conversation held through Manifold Switchboard is captured the
moment it happens, at full fidelity, on your own machine — no export,
no permission, no vendor cooperation required. Own your conversations
on the way in, because getting them back out later is only going to
get harder.

## The product, in two stages

### Stage one: the cockpit

One accessible desktop app that talks to many AIs:

- Claude, local Ollama models, and other backends in one interface,
  switchable by keystroke.
- Large, scalable text everywhere.
- Keyboard, voice, touch, and mouse — any one of them alone is enough.
- Speech in: prompt by dictation. Speech out: responses read aloud.
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
- Import old conversations, best-effort and last. Native capture and
  imported material stay clearly distinct; the original import file is
  kept unchanged so a better parser can re-read it later; known gaps
  are recorded. Forward capture is the foundation; import is a
  courtesy to history.

The transcript format carries provenance from day one, so the library
never needs a migration to exist.

## Who it is for

Anyone who needs their ears and voice to do what eyes and hands
usually do, and anyone who wants to own their work with AI instead of
renting access to it. The developer is the first user: this is built
from lived experience, not a compliance checklist.

## First milestone

The developer runs Manifold Switchboard daily; a response can be read
aloud with one keystroke; yesterday's conversation can be reopened
today.

## What success looks like

Daily use by the developer and ten real users. Then hundreds to a few
thousand devoted users who depend on it. Depth over breadth.

## Deferred

Web demo, TV client, teaching features, multi-user service. The core
abstractions keep those doors open; the accessible desktop comes first.

## Guiding principles

- If it cannot be done by reading large text, pressing keys, and
  speaking, it is not done.
- Never claim an imported conversation is complete unless the source
  proves it: preserve what arrives, record what's known, flag what's
  missing.
- The client should not need to know whether a backend uses a local process, local HTTP service, socket, remote server, or cloud API.
- Everything the app stores about you, you can inspect, correct,
  export, or delete. Owning your data means the right to erase it, not
  only to keep it.
- Every saved conversation belongs to the user, on the user's machine,
  in a format they can read without us.
- The library is only as complete as the cockpit is pleasant. Making
  people want to live here is a data-integrity feature.