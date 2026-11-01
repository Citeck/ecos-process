# Lazy Approval transaction boundary

`BpmnLazyApprovalService.approveTask` isolates one approval attempt with
`TxnContext.doInNewTxn`. The transaction includes active/historic task lookup,
outcome and token validation, report-template resolution, and task completion.
The caller transaction is restored after this transaction commits or rolls back.

The separate transaction was introduced in commit
`afe5e5f21c4cf7b7b23ccccb2897472ec53a59c8` (ECOSENT-2879, 2024-01-24), together
with the existing try/catch report flow. Its role in that flow is to roll back a
failed completion before returning an `EXCEPTION` report. Replacing it with a
joined transaction would not preserve that isolation.

Task reads must not precede this boundary. Camunda commands can enlist database
resources and flush pending changes; suspending their transaction while a second
transaction completes the same task can retain a conflicting lock. A database
`idle in transaction` state alone does not prove that this happened: diagnosing a
particular incident still requires blocking PIDs and application correlation.

Report construction is also database work. `TaskDefinitionUtils` loads the BPMN
model on a cache miss; the process-definition fallback can query the engine.
Success/error report metadata is therefore resolved before completion, inside
the isolated transaction. After rollback the catch block changes only the
in-memory report code/message. If task or report metadata could not be loaded,
the error report contains only the information already available; it does not
attempt another database lookup and mask the original failure.

`BpmnLazyApprovalTransactionIT` uses the real application transaction manager,
Camunda and PostgreSQL. Its task-read fixture deliberately flushes a local
variable, making the old boundary wait on its own caller transaction; a short
local lock timeout bounds the regression. This is a deterministic test of the
boundary, not evidence that every production task getter writes. Controls cover
rollback after deletion, cold report-template reads, rejected tokens/outcomes,
historic and missing tasks. Existing application tests cover the real Records
completion and permission paths.

`BpmnLazyApprovalRecordsIT` additionally uses the registered `lazy-approval` DAO,
real email-to-person lookup, approval service, Records task mutation and
`ProcTaskService` completion. External person/document records and the downstream
BPMN business action are fixtures; the approval/completion chain is not mocked.
Assertions inspect actual task/history/variable state, actor permissions,
outcome/comment/mail flag, rollback followed by successful retry, and repeated
approval of the historic task.

Its native read-flush case seeds an empty child execution's variable cache as
unknown before the request. Camunda's `VARIABLES_STATE_BIT` is one-based; the mask
comes from `BitMaskUtil.getMaskForBit`, rather than a hard-coded shift. The real
`getTaskById` loads the empty variable collection and updates `ACT_RU_EXECUTION`
(cache flag and revision). Approval must complete when that read is performed
inside the completion transaction. This models a supported lazy-cache state; it
does not establish the exact cache flags or Java call stack of the production
blocking PIDs. JDBC statement timeout bounds the negative test only and is not a
production timeout change. The test also runs against the old service boundary
as a discriminator, with the source restored afterwards.

The real Records suite also checks that an unrelated write made by the caller
transaction survives a failed approval. Negative runs distinguish two defects:
restoring the original service boundary makes the native execution-cache case
wait on `ACT_RU_EXECUTION`; replacing `doInNewTxn` with `doInTxn` leaves
`isCompletedViaMail=true` after a failed approval. Both variants are temporary
test mutations and are restored before final verification.
