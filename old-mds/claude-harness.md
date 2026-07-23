# Harnesses

Notes on the agent-harness landscape and what, if anything, Manifold
Switchboard (`myclaw`) should take from it.

This is a decision record, not a roadmap. It exists because harness
features are attractive, plentiful, and mostly built on premises that
contradict this project's. Writing down which premises differ is
cheaper than relitigating each feature as it appears in a video.

## The distinction that governs everything here

A harness is built for a task. Its ideal end state is the agent
working while the user stops watching.

This project is built for a person who is present the whole time and
needs the interaction itself to be survivable.

That difference pulls the opposite way on nearly every design
decision. Harnesses add panes, status, and autonomy to scan and
supervise; this removes anything that must be scanned. A feature that
is excellent in a harness is therefore not automatically good here,
and the burden of proof runs against it.

Two further filters, both from `claudes-vision.md`:

* Multi-backend support, local models, search, and handoffs are table
  stakes. Any funded team can build them in a quarter. None of them
  will build for people who cannot watch the screen. Effort spent on
  table stakes is effort not spent on the moat.
* One person has to maintain it. Anything requiring an ecosystem, a
  sandbox fleet, a domain, or an on-call rotation is out on those
  grounds alone.

## The landscape

Summarized from public material and one tool-hopping video. Included
so the comparisons below have a referent; not independently verified.

**OpenClaw** — open-source local agent, messaging-first (Telegram,
WhatsApp, Discord, Signal). Skills as `SKILL.md` directories, cron-like
heartbeat scheduling, broad local permissions. Strong on local freedom
and reachability; reported to be brittle in long multi-step workflows.
Security posture is its known weakness: malicious skills on the public
registry, tens of thousands of gateways exposed to the internet.
Installed locally for reference at `~/.npm-global/bin/openclaw` under
WSL Ubuntu.

**Hermes Agent** — persistent memory, procedural skill learning,
background task scheduling. The "nightly consolidation" idea comes
from here.

**Claude Cowork** — polished low-friction desktop execution inside
sandboxed directories.

**Perplexity Computer** — cloud-orchestrated, multi-model,
asynchronous execution in microVM sandboxes.

**Lovable** — natural language to full-stack web app; dashboards and
internal tools generated and hosted.

"Tool hopping" is the practice of moving between these per task,
because no single one is reliable across the whole workflow. The pain
of tool hopping is the handoff: context is lost between tools and
nothing keeps a unified record. That pain is adjacent to this
project's Stage Two (the library), which is worth noting but is not
by itself a reason to build toward the harnesses.

## Adopt

Small, local, additive, and compatible with the stated principles.

### 1. Nightly consolidation of the archive

From Hermes, reframed. Not "the agent works while you sleep" — that is
the harness premise. Rather: **the archive gets more useful over
time.** A scheduled job reads session records and produces summaries,
tags, indexes, and extracted decisions.

Fits directly:

* `SessionRuntime` already records immutable session events around
  `PromptService.submit(...)`. Those events are the input; no new
  capture path is needed.
* The principle that interpretations stay separate from the original
  captured record already dictates the output shape — consolidation
  writes derived artifacts alongside, never mutating the append-only
  record.

Implementation is a `ScheduledExecutorService` plus a prompt; no
server, no network dependency.

### 2. Local skills with progressive disclosure

From OpenClaw and Claude Code; the mechanics are the salvageable part
of the outside strategy document.

* A skill is a directory containing `SKILL.md`: YAML frontmatter
  (name, description, triggers) plus a natural-language body of
  instructions.
* At startup, index **only** the frontmatter — roughly twenty tokens
  per skill.
* Match the incoming prompt against those descriptions.
* Load the full body into context **only on a match**, preserving
  context-window space.

Attaches to `PromptService`, which already owns backend selection and
request creation; skill injection is one more step in request
assembly. The format is portable, so skills written for Claude Code or
OpenClaw can be reused as-is.

Convention: `.myclaw/skills/<skill-name>/SKILL.md`.

**Local loading only.** See Reject, below, regarding registries.

### 3. Curated memory file

A single markdown file reread at session start, appendable by the
system. The smallest of the three and the natural first step: it is a
curated peer of the transcripts already written under `runs/`, and it
serves "yesterday's conversation can be reopened today" directly.

Suggested order: memory file, then skills, then consolidation.

## Consider, with conditions

### Constrained tool execution

Letting a backend act — run a command, edit a file — is what makes
harnesses feel powerful. It is also where all of OpenClaw's danger
lives, and doing it safely (sandboxing, approval flows) is a large
engineering surface.

If pursued, not arbitrary execution: a small fixed menu of whitelisted
actions, each requiring explicit confirmation. The confirmation must
be operable by reading large text, pressing keys, and speaking — an
approval dialog that can only be handled visually is a regression
against the core principle, not a feature.

### One messaging channel

Reachability from a phone has genuine accessibility value: speaking to
the cockpit while away from the desk. But each channel is a
maintenance treadmill, and messaging-first framing is exactly what
pulled the outside strategy document away from the desktop.

If pursued: one channel only, loopback-first, allowlist of a single
user ID, never exposed to the public internet. Deferred until the
desktop first milestone is met.

## Reject

Each of these contradicts a stated principle, not merely a preference.

**Public skill registry (ClawHub equivalent).** Community moderation
plus a demonstrated malware vector. Not maintainable by one person.
Local skill loading captures the value without the ecosystem.

**Cloud sandbox sub-agents.** Contradicts "nothing leaves your
computer except the prompts you send" and local-models-first. Requires
microVM orchestration and cost management.

**Hosted micro-apps and dashboards** (`myinstance.myclaw.ai`).
Multi-tenant hosted infrastructure: domain, uptime, per-user
isolation, security. A different company. Also note the irony: a
hosted product acquires precisely the conflict of interest this
project's data-ownership thesis is aimed at.

**Full autonomous agent loops.** The "ran for six hours unattended"
behavior. Over-autonomy, unpredictable cost, and the crowded funded
space. Directly opposed to the present-user premise.

**Messaging-first as core philosophy.** Not a feature but a frame, and
the one that does the most damage. The repo is a native accessible
desktop workbench; adopting a 24/7-assistant-over-chat philosophy
would not evolve it, it would replace it.

## On the outside strategy document

A handoff document produced from tool-hopping source material was
reviewed (`[cite: N]` markers throughout; sources are the videos, not
this repo). Its Section 2 on progressive disclosure is accurate and
useful, and is the basis of Adopt item 2 above.

Its Section 3, however, states the core philosophy as a frictionless
24/7 messaging assistant, explicitly not an IDE — the opposite of what
this repo builds. Nothing in it derives from this project: not the
accessibility mission, not the desktop client, not local-models-first,
not `SessionRuntime`, not the capture-on-the-way-in thesis. It is a
good summary of the videos, mislabeled as a summary of this project.

Filed for reference; not adopted as strategy.

## Test for future harness features

When the next tool or video presents a feature worth wanting:

1. Does it serve a person who is present, or one who has stopped
   watching?
2. Can it be operated by reading large text, pressing keys, and
   speaking?
3. Does it keep data on the user's machine?
4. Can one person maintain it, with no ecosystem, fleet, or
   on-call?
5. Is it the moat, or table stakes any funded team ships in a quarter?

A no on any of 1–4 is a reject. A yes on 5's second clause means defer
until the first milestone is met.
