# Deployed BPMN process picker

`eproc/bpmn-proc-latest` returns the latest deployed Camunda version for each process key. Its record ID remains the engine process key, so selecting a result can still start that process or resolve its start form.

The menu's “Start workflow” picker sends an OR of `id contains text` and `definition contains text`, combined with its type and advanced filters. These conditions must retain their AND/OR/NOT structure; converting them to a flat DTO loses identifier filters and boolean semantics.

- `id` filters the engine's `DEF.KEY_`. Text contains matching is case insensitive and treats `%` and `_` literally.
- `definition` searches enabled BPMN models by serialized localized name or model identifier (`moduleId`, mapped to `extId` by the ecos-data adapter). Matching locale keys in serialized JSON is acceptable. The lookup requests only the first 50 matches, at offset 0; it does not scan subsequent pages. Disabled models are excluded from this lookup. Engine key filters remain independent.
- Matching model IDs are converted to deployment resource names, including workspace system ID prefixes. Camunda `NAME_` may be empty, so it cannot substitute for the model's localized name.
- Workspace restrictions and latest-version selection remain outside the search predicate. A `null` workspace scope denies the query. An empty set for a system query means all workspaces and must reach the engine without a workspace restriction.
- Count and list use the same SQL condition. Filtering happens before pagination, which orders by engine key and row ID for stable pages.

`BpmnProcessLatestFilter` generates SQL only from known fields and operators. User values, including resource names, are bound MyBatis parameters. Unsupported fields fail explicitly rather than silently returning an unfiltered list. Mongo model storage is legacy and is not part of this change's supported verification scope.

Run the focused regression checks and a clean build from this repository:

```sh
mvn -DskipITs -Dtest=BpmnProcessLatestFilterTest,BpmnProcessLatestRecordsTest clean verify
```

The SQL tests execute the actual MyBatis mapper against PostgreSQL supplied by the existing `TestContainers.getPostgres` infrastructure, using a separate schema per test; DAO tests cover workspace denial, name lookup scope, enabled filtering and the 50-match lookup limit. They do not replace verification against deployed PostgreSQL and the actual picker UI.

Existing `WorkspaceScopedDmnBpmnTest` integration checks cover all-workspace system queries, explicit scopes, nonmember denials and pagination.
