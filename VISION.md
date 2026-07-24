# Product Vision

MyClaw is an accessible, local-first Java desktop cockpit for working with
interchangeable AI systems while preserving conversations, context, and
artifacts in durable, user-owned records.

Accessibility is foundational. The first product is a desktop-first Java
workbench that can be operated by reading large text, pressing keys, and
speaking. Screen-reader friendliness, keyboard flow, high-contrast readable
views, speech output, and clear errors are not polish; they are part of the
core product definition.

## Why This Exists

Mainstream AI tools assume sharp eyes, fast hands, and constant visual
scanning across dense panes, small controls, and transient responses. MyClaw
starts from a different premise: the interaction should remain inspectable,
recoverable, and comfortable for people who depend on accessible desktop
software.

Mainstream AI tools also tend to trap work inside provider-controlled
history. MyClaw treats the session record as the product. Conversations,
context, decisions, and artifacts should belong to the user, live on local
storage by default, and remain inspectable, correctable, exportable, and
deletable.

## Principles

- The fundamental unit is the session, not the model or provider.
- Sessions outlive models, accounts, APIs, and backends.
- Capture is independent of presentation; views are projections of durable
  records.
- Local-first does not mean local-only: local and cloud backends can both be
  useful when the user chooses them.
- Backends are replaceable workers inside the user's project.
- Accessibility, transparency, and user ownership are design constraints.
- Optional skills, memory, scheduling, and agent loops should extend the
  workbench without making it an opaque autonomous system.
- MyClaw prioritizes accessible, inspectable interaction. It may also perform
  supervised or unattended work under explicit user-selected policies.
- Agent loops are policy-governed. Conservative limits are the default, while
  longer or unattended execution may be explicitly enabled.
- One-person maintainability matters. The project should avoid product shapes
  that require a marketplace, hosted fleet, or on-call operation.

## Scope Boundaries

These are outside the project because they conflict with local ownership or
with one-person maintainability:

- Public skill marketplace.
- Hosted multi-tenant service.
- Prompt-to-hosted-app product.
- Cloud sandbox fleet maintained by this project.
- Messaging-first assistant as the core product frame.

## First Milestone

The developer runs MyClaw daily. A response can be read aloud with one
keystroke. Yesterday's conversation can be reopened today. The archive grows
because the workbench is useful enough to use.
