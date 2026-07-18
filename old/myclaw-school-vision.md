# MyClaw School Vision

## Vision

MyClaw School will be an accessible learning environment in which one learner works with a coordinated team of AI teachers.

The learner may be a child, teenager, adult, expert entering a new field, or anyone who benefits from explanations, practice, feedback, speech, large text, keyboard access, or a slower and more deliberate pace.

The system will not merely answer questions. It will help the learner understand, practice, question, correct mistakes, and build durable knowledge.

## Product Goal

A learner should be able to:

1. State a learning goal or ask a question.
2. Receive an explanation matched to prior knowledge and preferred depth.
3. Ask for a simpler, deeper, more mathematical, more visual, or more practical explanation.
4. Work through examples and exercises.
5. Receive hints before complete answers when appropriate.
6. Have misconceptions identified and corrected respectfully.
7. Review progress across a durable learning session.
8. Use reading, typing, keyboard navigation, speech input, and spoken output.
9. Understand when the system is uncertain or when a human teacher should be consulted.
10. Learn through local or cloud AI models without changing the learning experience.

## Core Idea: A Coordinated Teaching Team

MyClaw School will present one coherent learning experience backed by several specialized teaching roles.

```text
Learner
   ↓
LearningSessionService
   ↓
Teaching Coordinator
   ├── Lead Teacher
   ├── Socratic Tutor
   ├── Practice Coach
   ├── Reviewer
   ├── Subject Specialists
   └── Safety and Capability Policy
```

The roles will not normally speak independently or compete for the learner's attention.

Instead:

1. The coordinator determines the next useful teaching action.
2. One or more roles prepare or evaluate a response.
3. The reviewer checks accuracy, clarity, level, and policy.
4. The learner receives one clear response.

The system should resemble a well-coordinated faculty, not a committee meeting with six chatbots interrupting one another.

## Teaching Roles

### Teaching Coordinator

The coordinator manages the learning process.

It decides whether the next step should be:

- an explanation;
- a guiding question;
- a hint;
- an example;
- an exercise;
- a correction;
- a review;
- a recommendation to seek human help.

### Lead Teacher

The lead teacher gives structured explanations and keeps the lesson focused on the learner's goal.

### Socratic Tutor

The tutor asks questions that reveal what the learner understands and helps the learner reach answers rather than merely supplying them.

### Practice Coach

The practice coach creates exercises, gives graduated hints, and adjusts difficulty based on performance.

### Reviewer

The reviewer checks:

- factual accuracy;
- logical consistency;
- clarity;
- appropriate depth;
- unexplained terminology;
- unsupported confidence;
- whether the response actually advances the learning goal.

### Subject Specialists

Specialist roles may provide domain-specific instruction in areas such as:

- mathematics;
- science;
- programming;
- history;
- writing;
- language learning;
- music;
- practical skills.

Specialists should be added only when they provide a clear teaching benefit.

## Learner Model

MyClaw School should maintain a transparent, editable learner profile.

Possible elements include:

- current learning goal;
- prior knowledge;
- preferred explanation depth;
- preferred pace;
- preferred use of mathematics, examples, diagrams, or code;
- accessibility preferences;
- mastered topics;
- unresolved misconceptions;
- recent exercises and results;
- whether hints or full solutions are preferred;
- capability and privacy policies.

The learner model should be limited to information that improves instruction.

It should not become a hidden psychological profile. The learner should be able to inspect, correct, export, or delete it.

## Learning Modes

MyClaw School may support explicit learning modes such as:

- Teach from first principles;
- Give the compressed technical explanation;
- Use mathematics;
- Use code examples;
- Use concrete examples;
- Ask me questions;
- Give me a hint;
- Quiz me;
- Review my solution;
- Find gaps in my understanding;
- Challenge my assumptions;
- Build a study plan;
- Read this aloud.

These modes should represent teaching behavior, not merely prompt decorations.

## Child, Teen, and Adult Use

MyClaw School will not be divided into a serious adult product and a decorative children's product.

Instead, it will support guided profiles with different interface complexity and capability policies.

```text
Guided
Standard
Advanced
```

A guided profile may be appropriate for a child, a new computer user, a person with cognitive or visual limitations, or an adult learning an unfamiliar subject.

Age may influence policy, but age alone should not determine intellectual depth.

A technically advanced child may need sophisticated material with restricted system access. An accomplished adult may want a simple interface and slow explanations. Human beings continue to resist convenient classification.

## Accessibility Goal

Accessibility is a primary design requirement.

Every important interaction should support:

- readable text;
- scalable text;
- high contrast;
- keyboard navigation;
- visible focus;
- speech input;
- spoken output;
- repeatable audio;
- concise status messages;
- operation without color alone;
- truthful simplified errors with optional technical detail.

The web and desktop versions should use consistent terminology and interaction patterns.

Optional visual characters, sound cues, or message shapes may reinforce state, but they must never be the only way state is communicated.

## Safety and Capability Goal

A simplified interface is not a security boundary.

MyClaw School will enforce capabilities in the application layer or server, not merely hide buttons.

A workspace policy may control:

- permitted AI backends;
- cloud access;
- internet tools;
- local file access;
- command execution;
- transcript storage;
- microphone retention;
- external links;
- installation and model management;
- adult or teacher approval requirements.

For guided child profiles, arbitrary command execution, unrestricted file access, credentials, and unreviewed internet tools should normally be unavailable.

Local-first operation should be supported so that learning sessions can remain on the user's computer when desired.

## Architectural Goal

MyClaw School will build on the existing MyClaw application core rather than duplicate AI integration logic.

```text
Desktop client ──────────────┐
                             │
Web client ──────────────────┼── LearningSessionService
                             │           ↓
Future clients ──────────────┘     TeachingTeam
                                         ↓
                                   PromptService
                                         ↓
                                     AiBackend
                              ├── Ollama over HTTP
                              ├── command-line tools through exec
                              ├── cloud AI services over HTTPS
                              ├── socket-based model services
                              └── remote MyClaw servers
```

The major boundaries should be:

### `PromptService`

Responsible for executing a prompt through a selected backend.

### `LearningSessionService`

Responsible for learning goals, lesson continuity, learner state, exercises, teaching actions, and progress.

### `TeachingTeam`

Responsible for coordinating teaching roles and producing a coherent instructional response.

### `WorkspacePolicy`

Responsible for enforcing capability, privacy, and approval rules.

### Client Adapters

Responsible for presenting the learning experience through desktop, web, speech, or other interfaces.

Sockets, HTTP, servlets, Swing, JavaFX, and browser code are adapters. They must not contain teaching logic.

## Initial Socket Architecture

The first implementation may use a localhost TCP socket with newline-delimited UTF-8 JSON.

```text
Desktop learning client
        ↓ TCP socket
MyClaw socket server
        ↓
LearningSessionService
        ↓
TeachingTeam
        ↓
PromptService
        ↓
AiBackend
```

The initial protocol should remain small and explicit.

Possible operations:

```text
health
listBackends
startLearningSession
sendLearnerMessage
getLearningSession
```

The protocol should include request identifiers, explicit operation names, bounded message sizes, and structured errors.

Java object serialization and guessed protocols should not be used.

## First Teaching-Team Version

The first meaningful teaching team should contain only three logical roles:

```text
Lead Teacher
Socratic Tutor
Reviewer
```

The execution sequence should be:

1. The learner sends a question or learning goal.
2. The lead teacher drafts a response.
3. The tutor decides whether an answer, hint, question, or exercise is most useful.
4. The reviewer checks accuracy, clarity, learner level, and policy.
5. The coordinator returns one final response.

These roles may initially be implemented by one model using structured prompts. Separate models or parallel agents are not required for the first version.

## Development Strategy

### Stage 1: Learning domain skeleton

- Define transport-neutral learning request and response models.
- Define `LearningSessionService`.
- Define `TeachingTeam`.
- Define `WorkspacePolicy`.
- Implement one deterministic fake teaching team for tests.
- Keep all code independent of sockets and user interfaces.

### Stage 2: Local socket proof of concept

- Add a localhost-only socket server.
- Use newline-delimited JSON.
- Expose health, backend listing, session creation, and learner-message operations.
- Test the complete protocol using a Java socket client.

### Stage 3: Single-model teaching team

- Implement lead teacher, tutor, and reviewer behavior through one AI backend.
- Produce one coherent response.
- Record the teaching action selected for each turn.
- Add tests using fake backends and fixed responses.

### Stage 4: Minimal accessible desktop client

- Provide one readable transcript.
- Provide one prompt editor.
- Add large actions for Explain, Hint, Quiz, Simpler, Deeper, and Read Aloud.
- Add scalable text and complete keyboard operation.

### Stage 5: Learner profiles and durable sessions

- Store learning goals and progress.
- Let learners inspect and edit their profile.
- Add export and deletion.
- Add guided, standard, and advanced profiles.

### Stage 6: Web demonstration

- Add an HTTP or WebSocket adapter.
- Reuse `LearningSessionService`, `TeachingTeam`, and `PromptService`.
- Present a limited accessible demonstration through Apache.

## Design Principles

- Teach rather than merely answer.
- Prefer one coherent response over visible agent chatter.
- Match instruction to the learner's actual knowledge, not assumed age.
- Make learner state transparent and editable.
- Keep safety policy separate from visual presentation.
- Enforce restrictions on the server or application side.
- Keep transports and user interfaces as adapters.
- Use explicit roles and explicit backend implementations.
- Let tests define behavior.
- Prefer meaningful names over explanatory comments.
- Begin with one model and simple orchestration.
- Add complexity only when evaluation shows a teaching benefit.

## Success Criteria

The first meaningful success will be:

> A learner starts a local MyClaw School session, states a learning goal, receives an explanation selected and reviewed by a three-role teaching team, asks for a hint or a deeper explanation, and continues through an accessible socket-based desktop client without the client knowing which AI backend was used.

The larger success will be:

> MyClaw School becomes an accessible, local-first learning environment in which coordinated AI teachers help children and adults understand, practice, question, and create while learners, parents, and human teachers retain control over privacy, capabilities, and educational goals.
