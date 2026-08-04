# MyClaw_CONSOLIDATED.md

Consolidated project summary — chat-transcript ingestion feature.

## 1. Target Objective & High-level Overview

Ingest chat transcript files (`.md` or `.json`), pass their conversational
content to an LLM through MyClaw's existing prompt pipeline, and persist the
model's structured project summary to `<Project>_CONSOLIDATED.md`.

The flow is: **read transcript → extract messages → wrap in a summarization
prompt → submit to a backend via `PromptService` → write the response next to
the input file.**

## 2. Key Architectural Decisions & Components

- **`TranscriptIngestionService`** (`myclaw.application`) — orchestrates the
  end-to-end flow. Depends only on `PromptService`, keeping it decoupled from
  any specific LLM backend (backend is selected by name, defaulting to
  `"claude"`).
- **`TranscriptIngestionException`** — dedicated unchecked exception wrapping
  I/O failures on read (transcript) and write (summary), so callers get a
  single, meaningful failure type. Missing input is reported earlier as
  `IllegalArgumentException`.
- **Format-agnostic extraction** — `extractMessages` detects JSON by file
  extension or leading `{`/`[`, parses via Gson, and handles several shapes:
  top-level arrays, `messages`/`transcript` arrays, `role`+`content`/`text`
  objects, and `prompt`+`response` pairs. On any parse failure it falls back to
  the raw trimmed text, so Markdown transcripts pass through unchanged.
- **Deterministic output naming** — `computeOutputPath` strips the input
  extension and appends `_CONSOLIDATED.md`, resolved as a sibling of the input.
- **Fixed summarization prompt** — `wrapInSummarizationPrompt` embeds the
  four-section rubric (Objective, Architecture, Implementation, Status) that
  shapes the generated document.

## 3. Implementation Details & Key Findings

- All static helpers (`computeOutputPath`, `extractMessages`,
  `wrapInSummarizationPrompt`) are exposed for direct unit testing without
  needing a live backend.
- UTF-8 is used explicitly for both read and write.
- Parent directories are created before writing the summary.
- JSON parsing failures are silently tolerated (raw-text fallback), which is
  robust for mixed/malformed input but means a genuinely broken JSON file
  produces a whole-file "message" rather than a hard error — acceptable for
  best-effort ingestion.
- The class is `final` with a null-checked constructor dependency, following
  the codebase's application-service conventions.

## 4. Current Status & Next Action Steps

**Status:** Implementation present in the working tree (untracked):
`TranscriptIngestionService.java` and `TranscriptIngestionException.java`, with
unit tests under `tst/unit/myclaw/application/`. Related edits are staged in
`HarnessMainApplication`, `ResultReporter`, and the harness integration test.

**Next steps:**
1. Wire a CLI/harness entry point so a transcript path can be ingested from the
   command line.
2. Confirm unit-test coverage for each JSON shape and the raw-Markdown fallback.
3. Run the integration test against a real `PromptService` backend.
4. Commit the new files and staged changes once verified.
5. Revisit naming/placement under the broader **Switchboard** direction (this
   feature fits a Session's transcript-consolidation action).
