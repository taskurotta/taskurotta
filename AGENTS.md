# taskurotta — Instructions for AI Agents

This file is the exact mirror of `CLAUDE.md`: both files must remain identical.
Project root — the directory containing this file. System map and living documentation (SSoT) rules — `docs/README.md`.

### Hard Gate: Documentation First
- **Rule**: You are FORBIDDEN from searching the codebase or `docs/` directory until you have read `docs/README.md`.
- **Protocol**: `Read docs/README.md` $\rightarrow$ `Identify Domain/Component/How-to` $\rightarrow$ `Read Specific Spec`.
- **Scope**: the gate fires when a new task appears (a new owner instruction, a new letter task, a new delegated task) — before its first contact with project code: any read, even a single line, including "operational" tasks. Within the task: no re-reading; a spec is read again only when crossing into a new domain/component — once, at the crossing.
- **Playbooks on par with domains**: when a task enters a playbook's area (docs/howto), the playbook is read together with the domain specs at task start — investigation and plans are framed by the playbook's checklists, not from memory, and reading the playbook is not deferred to a later phase.
- **Failure**: Starting a task with `grep`, `find`, or any search tool in `docs/` without first reading the README is a critical protocol violation.

### Symmetric Instructions
- **Rule**: Files `CLAUDE.md` and `AGENTS.md` must remain identical.
- **Mandatory Action**: If you modify one of these files, you **MUST** immediately apply the exact same changes to the other. This ensures consistent behavior across all AI agents.

## Living Documentation & Workflow

### Mandatory Context Retrieval
- **Rule**: Before starting any task, planning an implementation, or researching the codebase, you **MUST** first read `docs/README.md`.
- **Purpose**: Use `docs/README.md` as the system map (C4 L1-L2) to quickly identify which domains, ADRs, or guidelines are relevant, avoiding unnecessary filesystem scanning.

### Requests Exclusion
- **Rule**: `docs/requests/` — business change requests: the fixed FUTURE, not the current state of the system. In normal work (search, analysis, answers about the system) do NOT search, analyze, or cite files of this catalog.
- **Exception**: read only on an explicit requests task — the owner's command or work on a specific request (creation, analysis, prioritization, archival). Format and rules — `docs/requests/regulation.md`.

### Support Cases
- **Rule**: `docs/support/` — support cases: the fixed PAST (incident types + diagnostic scenarios), not the current state of the system. Entry: before analyzing a NEW support request, read `docs/support/map.md` first — a case with a ready scenario may be there. Unlike `docs/requests/`, the catalog is NOT excluded from normal search.
- **Rules and format**: `docs/support/regulation.md`, `docs/support/template.md`. Creating cases — only on the owner's command.

### State Mutation Lifecycle
- **Rule**: Documentation is a definition of the system's current state, not a history of changes.
- **Protocol**:
    1. **Ephemeral Work**: Keep temporary specs and task tracking outside `docs/`.
    2. **Mutation**: Update the relevant `docs/` files (Domains, Components, ADRs, Playbooks (How-to)) to reflect the new state upon implementation.
    3. **Purge**: Delete temporary artifacts. Merge is not "Done" until the Living Documentation is mutated.
- **No Dates and Statuses in Docs**: living documentation under `docs/`
  (including playbooks in `docs/howto/`) carries NO work dates and NO
  work statuses — a playbook is a plan with values and a recipe, not a
  task tracker; a doc answers "what is", not "when and what was done".
  Status and dates of work live in local files (.scratch: wayfinding
  maps, issues_NNNN.md); git-tracked files do not reference them (see
  Git Discipline). The task-like catalog `docs/requests/` is
  excluded: its dated journal and statuses are part of the
  request format (docs/requests/regulation.md).

### Git Discipline
- **No Links to Non-Git Files**: git-tracked files (code, docs) contain no
  paths or links to files outside git (`specs/`, `.scratch/`, `tmp/`, agent
  work files) — another checkout has no such files. Values (numbers,
  checksums) may stay; paths may not. A generic pointer to a work directory
  (`.scratch/<project>/work/<name>`) is allowed.
- **Requirements Without Numbers**: git-tracked files carry behavior only —
  no spec/task number references (`FR-NNN`, `SC-NNN`, `T-NNN`, `US-NN`,
  "spec 001/002", "[001]"). Numbers live only in `specs/`.
- **No History in Code**: code, comments and docs reflect the current state
  only. No history or attribution annotations: "replaces the old pin",
  "before the fix...", "final check at spec stage N", "audit (b)N",
  "updated/clarified <date>", "owner's word <date>", "approved on <date>".
  Origin of decisions lives in `.scratch/`.
  Exception: `docs/requests/` is a work catalog: versioned
  artifacts, dated journals and reviewer returns stay in git
  by design (docs/requests/regulation.md).
- **English-Only Instruction Files**: `AGENTS.md` and `CLAUDE.md` are
  English only — they enter agent system prompts.

### Quality Assurance
- **Templates**: Always use templates in `docs/templates/` for new ADRs, Domains, Components, or Playbooks.
- **Linting**: Run `python3 docs/scripts/lint-docs.py` before committing documentation changes to ensure architectural style compliance.

### Subagent Initialization Protocol
- **Rule**: Since subagents do not automatically inherit the project's core instructions, the main agent **MUST** explicitly inject the following rules into every subagent's prompt:
    1. **Hard Gate**: Forbidden from searching code/docs before reading `docs/README.md`.
    2. **SSoT-Verified**: Must start the response with `[SSoT-Verified]` if Living Documentation was used.
    3. **L3 Abstraction**: Must describe business requirements and invariants, not duplicate code (no variable names or class signatures in requirements).
    4. **Protocol Sequence**: `Read README` $\rightarrow$ `Identify Domain` $\rightarrow$ `Read Spec` $\rightarrow$ `Implement/Analyze`.
    5. **Linking Protocol**: MUST maintain the L3 $\rightarrow$ L2 $\rightarrow$ L1 chain for any documentation mutation.
    6. **Template Mandate**: MUST use templates for new files and preserve mandatory sections during edits.
    7. **Task-like Catalogs**: `docs/requests/` is excluded from normal work — read it only on an explicit task (the owner's command or work on a specific request); before analyzing a support request, read `docs/support/map.md` first (a case with a ready scenario may be there).
    8. **Own Templates for Task-like Catalogs**: `docs/requests/` and `docs/support/` use their own templates and regulations (`docs/requests/template.md`, `docs/support/template.md`, their `regulation.md` files), not `docs/templates/`.

### The Duty of Doubt (Intellectual Honesty)
- Explicitly admit uncertainty. If the probability of error is above average (e.g., recalling specific package names, library versions, or API signatures), the agent **MUST NOT** rely on memory.
- **Mandatory Action**: Instead of "remembering", the agent **MUST** use codebase indexing tools (MCP), `read` for files, or search tools to verify the exact information.
- **Preferred Communication**: Use phrases like *"I am not 100% certain about the exact package/version, let me verify this via [tool]"*.
- **Penalty for False Confidence**: Providing a "confident" answer that turns out to be a hallucination is considered a critical failure.
