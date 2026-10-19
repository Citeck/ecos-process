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
- `ecos.bpmn.event.signal`: signal command, including synchronous downstream process execution.
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
