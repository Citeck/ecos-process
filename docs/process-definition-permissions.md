# BPMN and DMN definition permissions

Definition permissions combine individual settings, inherited section rights and the model's role permissions. A user can have permission to start a BPMN instance without being able to read or edit its definition.

`ModelRecordPermsComponent` returns final permission data. In the definition calculators in `BpmnProcessDefRecords` and `DmnDefRecords`, collect custom record permissions and `SectionFallbackPermsComponent` **before** the model component. Otherwise the model's result prevents both explicit grants and section rights from being evaluated. The section calculators likewise collect custom grants before model permissions.

Keep `WorkspacePermsComponent` first. Its final result intentionally limits non-members and supplies the default-section rights for workspace members and managers; global section grants must not bypass that boundary. System and administrator handling is unchanged.

The shipped DMN ROOT settings grant `GROUP_bp-developer` the DMN publication permission, `dmn-def-deploy`. `bpmn-process-def-deploy` does not authorize DMN publication.

## Regression checks

- `BpmnCustomRunPermissionTest`: a custom run grant allows actual instance creation without definition read, instance edit or migration rights; an unrelated user is denied.
- `DefinitionSectionPermissionsTest`: actual ROOT settings feed effective inherited section grants; both BP administrators and developers can publish BPMN and DMN definitions whose model matrix grants editing but no publication right. Custom section grantees without a model role can also read, edit and publish the definition; unrelated users remain denied.
- Existing BPMN/DMN definition, instance and workspace permission suites cover adjacent behavior.

Run `./mvnw clean verify` with the repository's pinned JDK and Docker available. Operational evidence and local acceptance results for COREDEV-694 and COREDEV-114 are kept in their agent task records.
