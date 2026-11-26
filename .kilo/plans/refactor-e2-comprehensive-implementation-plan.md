# Refactor-E2 Comprehensive Implementation Plan

Branch: `Refactor-E2` (verified clean). Stack: Spring Boot 4.1.0 / Java 21 hexagonal backend (PostgreSQL), Angular 22 + Vitest 4 frontend. No Flyway/Liquibase; Hibernate `ddl-auto: validate`. No drag-drop dependency present.

## Baseline (pending measurement)
- Backend: `cd backend && mvn verify` (JaCoCo report). Frontend: `cd frontend && npm test` (Vitest coverage-v8). Thresholds not currently configured; must add 85% line+branch to both.

## R1 Global Search
- Backend: add `search(q)` to ProjectQueryService/TaskQueryService/HierarchyQueryService; reuse `SearchText`. Controller endpoints: `GET /api/projects?q=`, `/api/tasks?q=`, `/api/activities?q=`. Case/accent-insensitive via DB `ILIKE` + normalize.
- Frontend: search component debounced (300ms), grouped results with entity type + parent context, navigate via new routes.
- Tests: empty/partial/no-results/special chars/duplicate names/auth scoping.

## R2 Duration consistency
- Canonical: minutes (int) already used. Keep minutes internally; API minutes; UI shows unit label. Add validation (positive, max). No migration needed. Ensure Task/Subtask actual minutes nullable handled. TimeTracking seconds internal unaffected.
- Tests: conversions, rounding, invalid values.

## R3 Direct hierarchical navigation
- Add Angular routes with identifiers: `/projects/:projectId`, `/projects/:projectId/stages/:stageId`, `.../activities/:activityId`, `.../tasks/:taskId`, `.../subtasks/:subtaskId`. Backend already supports GET by id. Handle 404/403.
- Tests: deep link refresh, invalid id, back navigation.

## R4 Hierarchical creation
- Validate parent refs in commands (already). UI contextual "Add" buttons per level. Backend rejects orphan creation. Tests.

## R5 Phase double-click bug
- Investigate `HierarchyController`/component event handlers. Likely missing `disabled`/loading guard causing double submit. Fix with single-flight guard + disable button. Tests.

## R6 Drag-and-drop reordering
- Add `position`/`orderIndex` column? Currently no ordering field. Add `sortOrder` to Stage/Activity/Task entities + migration SQL (manual, since no Flyway). Backend endpoint `PUT .../reorder` with ordered ids, transactional. Frontend: use Angular CDK DragDrop (already? check node_modules @angular/cdk). If absent, add dependency justification. Optimistic update + rollback. Keyboard move up/down. Tests.

## R7 Collapsed counts
- Fix conditional rendering in tree component; keep count badge outside collapse. Tests.

## R8 UX simplification
- Contextual creation dialogs, progressive disclosure, defaults. Reduce steps. Before/after documented in plan. Tests.

## R9 Entity construction
- Project.java constructor 11 params. Introduce static factory `Project.create(...)` already exists; add builder only if needed. Keep JPA constructor. Apply to Task similarly. Tests for invariants.

## Cross-cutting
- API consistency, DB integrity, security, performance.
- Coverage: add thresholds 85% both metrics to pom JaCoCo rules and frontend vitest config. Run after each phase.

## Implementation order
Phase 0 baseline → Phase 1 domain (duration, construction, ordering schema) → Phase 2 backend (search, reorder, validation) → Phase 3 frontend (routes, search, DnD, counts, UX) → Phase 4 integration → Phase 5 coverage.

## Risk register
- Manual DB migration (no Flyway) → provide SQL script.
- DnD dependency → prefer @angular/cdk if installed.
- Coverage gap → add tests.
- Deep link auth → guard.

## Acceptance checklist
All R1-R9 + 4 coverage thresholds + builds pass.
