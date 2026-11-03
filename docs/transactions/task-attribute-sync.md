# Resumable task attribute synchronization

Full/type synchronization requests still arrive through `bpmn-task-atts-sync`.
The consumer now saves a job in ecos-data and returns; RabbitMQ acknowledges
the delivery only after that independent database transaction commits. It no
longer waits for all task variables of a type to be populated. Moving work to an
in-memory executor before acknowledgement would not provide restart recovery.
Registration failures explicitly NACK/requeue the delivery without escaping the
consumer callback: the Java client's default exception handler closes the channel
when a callback throws. A redelivery after the database commit finds the existing
job. The Rabbit wrapper does not ACK a delivery already explicitly NACKed.

Jobs use the same `DbDomainFactory`/Records storage pattern as BPMN elements:
schema `ecos_data`, table `bpmn_task_atts_sync_job`, technical type and Records
source `bpmn-task-atts-sync-job`. System/admin permissions restrict access.
No custom SQL table migration or external service is required. Each job stores
its request ID, document type, mode, task creation cutoff, last task ID,
processed count, completion flag and next-attempt time.

A scheduled worker processes at most one 50-task page per invocation. Its delay
is `ecos-process.bpmn.task-atts-sync.job-rate-ms` (default 1000 ms, measured after
the previous invocation finishes). Application readiness gates startup. The
existing application distributed lock serializes workers across instances;
registration uses a separate lock so it does not wait for a full page of task
work. Lock ownership is checked while processing and before checkpointing.

Pagination uses task IDs greater than the saved cursor, ordered ascending,
instead of OFFSET. Removing previously visited tasks does not shift the next
page. The creation cutoff bounds the run; tasks created afterwards use the
existing synchronous task-create projection. The task-ID query uses JDBC,
avoiding incomplete native-query entities in Camunda's command cache.

The worker commits the cursor only after all writes in the page succeed.
Camunda writes and the ecos-data checkpoint are separate transactions: this is
at-least-once processing, not exactly-once. A crash or checkpoint failure can
repeat the current page (up to 50 writes), but completed pages are not rescanned.
An error preserves the last checkpoint and retries after 30 seconds. A permanent
bad record can keep that job retrying; a retry limit/error-queue redesign is
outside this hotfix. Other due jobs can proceed during the backoff. No unfinished
job depends on publishing a continuation message: the database is polled again
after a restart.

New queue messages include a unique `requestId`; redelivery does not create a
second job. Legacy messages without that field use a stable type/mode identity,
coalescing old pending duplicates into one run using current settings. Completed
job records are retained to recognize redelivery. A later intentional request
with a new ID still creates a new run. Existing request producers, settings
lookup and document-change/task-create synchronization retain their behavior;
this hotfix does not batch attribute reads or filter additional type events.
Checkpoints cannot recover progress already performed by an older binary which
never stored it; the first upgraded run starts with an empty cursor.

Logs identify the job, type and mode, actual completed page size, cumulative
visited task count, elapsed page time and completion state. The count includes
visited tasks without a document reference, not only changed variables.

`ProcTaskResumableSyncIT` exercises the registered Rabbit consumer, ecos-data on
PostgreSQL, Records projections and actual Camunda writes. It queries the test
broker's Ready/Unacked counts, recreates the worker/repository over the same
durable database, injects failures inside a page and before checkpoint saving,
deletes already-visited tasks, and tests redelivery after registration before
ACK plus legacy payloads. Worker recreation is not a Kubernetes pod restart;
no production deployment or throughput claim follows from the fixture.

The existing `UserTaskAttsSyncTest` explicitly enables the real scheduled worker
in its isolated test configuration (100 ms delay, context closed after the
class). Its 16 cases retain the assertions for settings changes, type attributes,
document updates and inherited types. Ordinary test contexts keep automatic
sync scheduling disabled so failure/recovery tests can control page boundaries.
