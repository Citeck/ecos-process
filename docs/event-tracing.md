# BPMN event tracing

Synchronous record events execute inside the caller's mutation. A long
`ecos.webapi.executor.action` for `/events/emit` therefore delays the HTTP response.
The event path uses the existing `EcosMicrometerContext` and its runtime sampling
configuration; it does not enable tracing or change event delivery.

## Observation boundaries

- `ecos.bpmn.event.process`: processing one incoming BPMN event, including empty results and failures.
- `ecos.bpmn.event.subscriptions.find`: name generation, subscription query and metadata resolution.
- `ecos.bpmn.event.subscriptions.query`: Camunda command execution, including transaction acquisition/flush; bounded `statement` identifies the query.
- `ecos.bpmn.event.deployment.resolve`: process/deployment metadata lookup.
- `ecos.bpmn.event.definition.resolve`: definition cache lookup and element matching.
- `ecos.bpmn.event.definition.load`: cache miss, revision loading and BPMN parsing.
- `ecos.bpmn.event.attributes` / `ecos.bpmn.event.predicate`: event projection and predicate evaluation.
- `ecos.bpmn.event.signal`: signal construction and command executor, including synchronous downstream process execution.
- `ecos.bpmn.event.signal.command`: command body after entering Camunda's interceptor chain.
- `ecos.bpmn.event.signal.subscriptions.find`: subscription lookup by signal ID.
- `ecos.bpmn.event.signal.definitions.resolve`: start-event definition/cache resolution and enabled-state parsing.
- `ecos.bpmn.event.signal.authorize.catch` / `.authorize.start`: authorization before delivery or process creation.
- `ecos.bpmn.event.signal.executions.notify`: catch subscriptions, with `.execution.check` for lazy execution lookup/state checks
  and `.event.receive` for synchronous `eventReceived` and downstream execution.
- `ecos.bpmn.event.signal.instances.start`: start subscriptions, with `.instance.create` for activity lookup/instance creation
  and `.instance.start` for synchronous execution of the new instance.
- `ecos.bpmn.conditional.process`, `.subscriptions.find`, `.processes.query`,
  `.definition.resolve`, `.definition.load`, `.fire`: corresponding conditional-event phases.
- `ecos.bpmn.script.execute`: BPMN script engine lookup, binding/environment setup,
  evaluation, result detachment and inline context cleanup. Transaction-deferred
  cleanup happens after this span. DMN scripts do not use this entry point.

Event, subscription, deployment and activity identifiers are high-cardinality
trace attributes, not metric dimensions. Script source, predicates, event payloads
and process variables are not added as attributes. The common observation
infrastructure records errors and closes scopes.

BPMN listener IDs are `bpmn-event:<eventName>:<listenerEventName>` and
`bpmn-conditional-record-changed`, making them recognizable in the shared events
library spans. Per-listener spans require the accompanying ecos-events change
to be included in the application's resolved dependencies.

## Shared JDBC and scheduler observations

The webapp parent 3.36.0 supplies `ecos.jdbc.connection.acquire` separately from
SQL/query/update/batch execution. These child spans help distinguish connection
acquisition from statement time in Camunda's JDBC path. Result fetching is not
separately timed, and XAResource-level commit is outside `ecos.jdbc.commit`.
Bound SQL parameters are not captured.

Scheduled invocations have an `ecos.task.scheduler` parent with `taskId`, including
Spring `@Scheduled` methods. Transactions started by those tasks retain that parent.
Camunda's independent job executor still has no automatic command-name observation.

## Reading a slow trace

Look for the deepest long span. For example, time in `.subscriptions.query`
points to the Camunda command/database boundary; time in `.definition.load`
points to revision loading/parsing; time in `.script.execute` points to script
setup or execution. A query span alone does not distinguish a slow SQL statement
from waiting for a connection or transaction lock; use a concurrent database/JVM
profile for that distinction. Short spans may be removed by configured sampling
or exporter filtering.

The motivating trace `242830846bce799f98a3e2bdf4eee80a` had a 25.240-second gap
before the first child of the eproc event executor. Accessible container logs
did not establish its cause. These observations make future occurrences more
specific; they do not prove the cause of that historical request.

Observation metadata is carried by typed `EcosObsContext` classes. Tag policy is
implemented by `GlobalEcosObsConvention`: events in ecos-webapp-commons, BPMN
in ecos-process. The shared convention wrapper controls high-cardinality tags
according to `management.tracing.enabled`. Business code does not add tags directly.

Contexts retain original event, subscription and script/scope objects when available.
Conventions alone choose their exported fields; primitive arguments such as command
IDs remain direct context inputs when they are already the operation input.

A gap between the outer signal span and `.command` points to command construction or
executor/interceptor setup. Time after `.command` ends can include interceptor cleanup
and database flush; neither gap alone proves a database wait. Within `.command`, the
phase spans separate lookup, definition resolution, authorization and continuation.
`.event.receive` and `.instance.start` still include Camunda's downstream execution;
script spans nest inside them. These boundaries do not identify individual SQL waits
or every BPMN activity. Subscription IDs remain high-cardinality attributes; observing
a phase does not resolve execution/process metadata merely to construct tags.
