# Product Vision

## Core Mission

MyClaw is built for blind, low-vision, deaf, and hard-of-hearing developers.
Accessibility is the foundational architecture, not an add-on.

If it cannot be done by reading large text, pressing keys, and speaking, it
is not done.

## Why This Exists

**The interfaces exclude people.** Mainstream AI tools assume sharp eyes and
fast hands: dense multi-pane layouts, mouse precision, tiny controls, silent
and fleeting responses, endless panning across a magnified canvas. For an
engineer with low vision or visual fatigue, this is a structural barrier to
daily work.

**The work is not yours.** Conversation history sits on providers who guard
it. No API reaches it; exports are manual, lossy, proprietary, and
increasingly gated. So MyClaw captures on the way in — full fidelity, local
disk, no permission needed. Getting your work back out later will only get
harder. The archive is the product, not exhaust.

A tool sold by the seat has no reason to make your history easy to take
elsewhere. That is a conflict of interest, not a capability gap, which is
why it persists.

## What Makes It Different

Agent harnesses are abundant, and nearly all are built for a task: their
ideal end state is the agent running while you stop watching. MyClaw is
built for someone present the whole time. That pulls the opposite way on
almost every design decision — harnesses add panes and status to supervise;
this removes anything that must be scanned.

Multi-backend support, local models, search, and handoffs are table stakes.
Any funded team could ship them in a quarter. None of them will build for
people who cannot watch the screen.

## Core Pillars

### Multi-Sensory Collaboration

* **Speech in and out:** dictation for hands-free prompting, synthesis for
  spoken answers.
* **Audio sonification:** background events, build and test results, and
  stream states rendered as distinct tonal cues rather than visual
  notifications.

### Zero-Mouse Keyboard Navigation

* **Keyboard-first flow:** no mouse precision, no drag-and-drop, no hunting
  for small controls.
* **Global context capture:** hotkey text-buffer ingestion from the editor
  straight into the runtime, without breaking focus.

### Visual Serenity and High Contrast

* **Readable consoles:** scalable typography, high-contrast themes,
  high-legibility Markdown streams.
* **Screen reader optimization:** clean text streams parsed from the first
  character. Errors are readable, copyable, and speakable — never trapped
  in an unreachable dialog.

### Predictable Spatial Layout

Output streams are bound to fixed, predictable regions rather than
scattered across a canvas, eliminating the disorienting panning that
magnification otherwise forces. On a multi-monitor setup this maps to
dedicated screens — execution, workspace, transcripts — but a single
screen remains fully supported. Multiple monitors are an accommodation,
never a requirement.

## Guiding Principles

* The fundamental unit is the session, not the model.
* The stored record is the source of truth: append-only and content-hashed.
* Capture is independent of presentation. Views are projections of the
  record.
* Interpretations — summaries, tags, indexes — stay separate from the
  original and never silently rewrite it.
* Work is organized around projects and goals, not provider names. A
  backend is a worker inside the project, not where the work belongs.
* Everything stored about you, you can inspect, correct, export, or delete.
* Never claim an import is complete unless the source proves it.
* The archive is only as complete as the cockpit is pleasant. Making people
  want to work here is a data-integrity feature.

## Scope: What This Is Not

These are ruled out because they contradict a principle above, or because
one person cannot maintain them:

* **A public skill registry.** Community moderation plus a demonstrated
  malware vector. Local `SKILL.md` loading captures the value without the
  ecosystem.
* **Cloud sandboxes or hosted multi-tenant services.** Contradicts
  local-first and data sovereignty; requires infrastructure and on-call.
* **Prompt-to-hosted-web-app generation.** A different product category,
  orthogonal to the mission.
* **Messaging-first operation as the core frame.** MyClaw is a desktop
  workbench. A 24/7 chat assistant is a different product wearing the same
  name.
* **Unbounded autonomous agent loops.** Bounded, policy-governed background
  work is a roadmap item; hands-off autonomy as the default premise is not.

## Test for New Features

1. Does it serve a person who is present, or one who has stopped watching?
2. Can it be operated by reading large text, pressing keys, and speaking?
3. Does the data stay on the user's machine?
4. Can one person maintain it — no ecosystem, fleet, or on-call?
5. Is it the moat, or table stakes any funded team ships in a quarter?

A "no" on 1–4 is a reject. Table stakes are deferred until the first
milestone is met.

## First Milestone

The developer runs it daily. A response can be read aloud with one
keystroke. Yesterday's conversation can be reopened today.

## Success

Daily use by the developer and ten people who depend on it. Then hundreds
to a few thousand devoted users. Depth over breadth.
