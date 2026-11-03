# GraalJS engine and context lifetime

The 2.30.13 maintenance release backports both parts of COREDEV-418 from
commits `35d4f0e8` and `eea73923`. The 2.30.12 line did not contain those fixes,
although they were already present in the main release line starting at 2.33.0.

Previously, Camunda JavaScript engine resolution built a new polyglot `Engine`
for each evaluation. GraalJS does not advertise a cacheable JSR-223 engine, so
Camunda's engine cache did not prevent this allocation. Unclosed engines remained
reachable through the static `Engine.ENGINES` registry and its
`EngineReference.receiver` field. Each engine also retained its JavaScript
contexts and their functions, shapes, properties and execution metadata.

`GraalScriptEngineResolver` now shares one polyglot Engine per resolver while
keeping a separate Context for each execution. Sharing a Context would allow
global JavaScript variables to escape into later process executions.

Engine reuse alone is insufficient. `EcosScriptingEnvironment` also owns the
BPMN evaluation context and closes it after transaction commit or rollback.
The environment scripts and user script must share that context until evaluation
finishes. Transaction completion is the closing boundary because Camunda can
still serialize script-created variable values after the script returns. Without
a transaction, the context closes inline. Returned results are detached before
closing so callers can continue to use them.

DMN JavaScript decisions use `EcosDmnScriptEngineResolver` and
`EcosDmnScriptEngine`. They detach results and close the evaluation context on
both success and failure. The wrapper does not expose `Compilable`: compiling
through GraalJS would create an additional default context with no owner to
close it. Plain bindings avoid creating a second unused context.

Contexts remain open until the end of their transaction. A transaction containing
many script executions can therefore have a temporary memory peak; an async
process boundary limits the work done in one transaction. This is distinct from
unbounded retention across completed transactions.

The regression suite checks engine-registry growth, retained context counts,
concurrent execution and isolated globals, error paths, detached results, BPMN
JSON variables and result variables, and JavaScript DMN evaluation. The stock
Camunda scripting environment is also tested as a positive leak control so the
context counter demonstrates that it can detect the original defect.
