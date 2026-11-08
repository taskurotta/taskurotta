# Project Knowledge Base (Living Documentation)

This directory is the **Single Source of Truth (SSOT)** for the system. It represents the "Living State" of the architecture. 

**Core Principle**: Documentation is not a record of what we *did*; it is a definition of what the system *is*. If the code changes, the documentation MUST mutate to reflect the new state.

## Navigation Map

### 1. System Architecture (C4 Model)
Zoom-in navigation from high-level context to technical components.
- **L1: System Context**: [system/01-context.md](system/01-context.md) — The system's place in the world.
- **L2: Containers**: [system/02-containers.md](system/02-containers.md) — High-level technical building blocks.
- **L3: Domain Components**: [docs/domains/](./domains/) — Bounded Context and DDD components.
  - Each domain contains its own `domain.md` (Ubiquitous Language) and `components/*.md` (Logic).
  - [metrics](domains/metrics/domain.md) — Оперативные метрики оркестратора: сбор и хранение рядов в памяти процесса, предоставление веб-консоли.
  - **Dictionaries Component**: Если домен использует технические коды, ID или перечисления, они ДОЛЖНЫ быть централизованы в `components/dictionaries.md` (запрещен хардкод «магических кодов» в L3 спецификациях).

### 2. Architectural Governance
- **ADRs (The "Why")**: [docs/adr/](./adr/) — Append-only records of decisions. 
- **Epics (The "Where to")**: [docs/epics/](./epics/) — Long-term strategic transitions.
- **Guidelines**: [docs/guidelines/](./guidelines/) — Standard patterns and constraints.

### 3. Public API Projections
- **Public Specs**: [docs/public/](./public/) — Publicly exposed projections of the internal living documentation.

### 4. How-to / Playbooks (The "How")
- **Procedures**: [docs/howto/](./howto/) — Cross-domain step-by-step recipes ("how to do X"). One directory per playbook: `docs/howto/<slug>/playbook.md` (+ optional `files/`, `scripts/`).
  - **Catalog**: ALL playbooks MUST be listed in this section (the agent discovers recipes from this README).
    - [project-environment](./howto/project-environment/playbook.md) — How to work with the project environment (logs, databases, task engine, tracker, environments). Created by bootstrap, always present; filled by the project with its real tools or an explicit "no specific tools" note.
    - [e2e-regression-run](howto/e2e-regression-run/playbook.md) — Регрессионный E2E-прогон: стек «как на площадке» (боевой деплой-конфиг), 363 актёра по профилю пика инцидента, серия heap-дампов и отчёт «корректность + память метрик»; зафиксированный эталон для регресс-сравнения будущих сборок — `baseline.md`.
  - **Inverse index**: [howto/README.md](./howto/README.md) — "domain → playbooks": when a domain changes, re-check the affected playbooks.

### 5. Business Requests (The "What's Next")
- **Requests**: [docs/requests/](./requests/) — Business requests for product changes: the recorded FUTURE, not the current state of the system. A task-like catalog alongside epics. Map: [requests/map.md](./requests/map.md); regulation: [requests/regulation.md](./requests/regulation.md); template: [requests/template.md](./requests/template.md).
  - *Rule (Exclusion)*: `docs/requests/` is excluded from normal agent work: do NOT search it when analyzing the current state, do NOT cite it in answers about the system. Entry — only on an explicit task: the owner's command or work on a specific request (then it may and must be read).

### 6. Support Cases (The "Why It Broke")
- **Cases**: [docs/support/](./support/) — Generalized incident types with quick diagnosis scenarios: repeatable check sequences to find the cause. A task-like catalog: requests — the future, cases — the past + diagnosis. Map: [support/map.md](./support/map.md); regulation: [support/regulation.md](./support/regulation.md); template: [support/template.md](./support/template.md).
  - *Rule*: before analyzing a new support request, look at `support/map.md` first — a case with a ready scenario may be there. The catalog is NOT excluded from search (unlike `docs/requests/`).
  - *Tools*: the project's diagnostic tools are described in the [project-environment playbook](./howto/project-environment/playbook.md); the case regulation references that playbook, not tool names.

## Operational Framework

### Truth vs. Tooling
- **`docs/` (The Truth)**: Permanent, curated, and mutated.
- **Tooling Workspaces**: All development tools must store temporary files **outside the `docs/` directory**.
  - *Rule*: Nothing from a tooling workspace ever enters `docs/` without being refactored into the living state.
  - *Rule (References)*: Living documentation must never link to or reference files that are not in git (issues, task trackers, agent state files, letters): other employees and their agents do not have those files and cannot follow the reference. Every statement in `docs/` must be self-contained.

### Task Lifecycle (Delta vs. State)
1. **Ephemeral Tasks**: Temporary requirements live in tooling workspaces.
2. **Mutation**: Update `docs/` to reflect the new state upon completion.
3. **Cleanup**: Delete ephemeral artifacts.
4. **Exception**: Epics (`docs/epics/`) are long-term strategic specifications; Business Requests (`docs/requests/`) are the second form of "task-like" documentation allowed in the `docs/` directory (the recorded FUTURE state, excluded from normal agent search — see Navigation Map §5); Support Cases (`docs/support/`) are the third form (recorded PAST incidents with repeatable diagnosis scenarios, available for normal agent search — see Navigation Map §6).

### Quality & Consistency

#### 1. The Linking Protocol (L3 $\rightarrow$ L2 $\rightarrow$ L1)
To prevent "Ghost Components" and "Shadow Domains", the following chain of updates is **MANDATORY** and must be treated as a single atomic operation:
- **New Component (L3)** $\rightarrow$ Must be explicitly listed and described in the **Domain Spec (L2)**.
- **New Domain (L2)** $\rightarrow$ Must be explicitly listed and described in the **README (L1)**.
- **New/Changed Playbook (How-to)** $\rightarrow$ Must be listed in the **README (L1)** catalog AND in the inverse index `docs/howto/README.md` (domain → playbook); a change of a depended domain triggers a re-check of the listed playbooks.
- **Deletion**: Removing a file at L3 MUST trigger a removal of its reference at L2.
**Failure to maintain this chain is a critical architectural regression.**

#### 2. Lifecycle Markers
To manage the evolution of the system, the following markers are used in documentation:
- **`[DEPRECATED]`**: Indicates that a feature, method, or logic is still present and functional in the system, but is considered obsolete.
  - **Maintenance**: It is acceptable to use deprecated elements when maintaining existing functionality.
  - **New Development**: It is **STRICTLY FORBIDDEN** to use deprecated elements in new features. Developers must find the alternative solution described in the linked EPIC.
  - **Audit**: The presence of `[DEPRECATED]` markers is a normal state during architectural migrations and is NOT a consistency error.

#### 3. Template Mandate
- **Creation**: All new ADRs, Domain files, Component specs, or Playbooks **MUST** be created using the corresponding templates in `docs/templates/`.
- **Evolution**: When editing an existing file, it is **FORBIDDEN** to remove mandatory sections defined in the template unless the component no longer fits that category.
- **Verification**: Every mutation must be verified against the template.
- **Free Zone**: Sections of a playbook below the mandatory sections belong to its creator; restructuring another playbook's free zone without human approval is forbidden.
- **Task-like Catalogs**: `docs/requests/` and `docs/support/` use their own templates (`docs/requests/template.md`, `docs/support/template.md`) and their own `regulation.md` files; `docs/templates/` does not apply to them.

#### 4. Linter & Standards
- **Linter**: Run `python3 docs/scripts/lint-docs.py` before committing. A "Pass" is a prerequisite for any merge.

## For AI Agents: Execution Protocol

**Mandatory Verification Marker**: You MUST start your response with the prefix `[SSoT-Verified]` when providing analysis based on this documentation.

1. **Initialize**: Read `README.md` $\rightarrow$ `AGENTS.md` $\rightarrow$ Identify Domain in `docs/domains/`.
2. **Analyze**: Read the `domain.md` for language and `adr/` for constraints.
3. **Work in Isolation**: Use tooling workspaces outside `docs/`.
4. **Mutate the State**: Update living documentation in `docs/`.
   - **Atomic Update**: Apply the Linking Protocol (L3 $\rightarrow$ L2 $\rightarrow$ L1).
   - **Template Check**: Ensure all mandatory sections from templates are preserved.
5. **Verify**: Check against `docs/templates/` and run linter.
6. **Purge**: Delete ephemeral artifacts. 

**Task-like catalogs**: `docs/requests/` is excluded from normal work (read it only on an explicit task — the owner's command or work on a specific request); before analyzing a support request, read `docs/support/map.md` first (a case with a ready scenario may be there). Rules — Navigation Map §5–§6.

**Failure to maintain the Linking Protocol or Template Mandate is a critical failure.**