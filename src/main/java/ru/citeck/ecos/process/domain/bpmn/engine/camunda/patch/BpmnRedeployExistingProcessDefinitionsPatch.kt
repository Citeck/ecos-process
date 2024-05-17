package ru.citeck.ecos.process.domain.bpmn.engine.camunda.patch

import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import java.util.concurrent.Callable

/**
 * Patch for apply new rule of
 * [ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskAssignParseListener]
 * to all existing process definitions
 */
@Component
@EcosPatch("bpmn-redeploy-existing-process-defs", "2023-05-02T00:00:00Z")
class BpmnRedeployExistingProcessDefinitionsPatch(
    private val camundaRepoService: RepositoryService,
    private val procDefService: ProcDefService,
    private val recordsService: RecordsService,
    private val bpmnProcessDefRecords: BpmnProcessDefRecords
) : Callable<Any> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun call(): Any {

        log.info { "Start patch BpmnRedeployExistingProcessDefinitionsPatch" }

        val defs = camundaRepoService.createProcessDefinitionQuery()
            .latestVersion()
            .list()

        log.info { "Found ${defs.size} process definitions" }

        defs.map {
            procDefService.getProcessDefRevByDeploymentId(it.deploymentId)
        }
            .mapNotNull { it }
            .forEach {
                log.info { "Redeploying process definition: ${it.procDefId}" }

                val stringDef = String(it.data)

                val bpmnMutateRecord = bpmnProcessDefRecords.BpmnMutateRecord(
                    id = "",
                    processDefId = "",
                    name = MLText.EMPTY,
                    ecosType = EntityRef.EMPTY,
                    formRef = EntityRef.EMPTY,
                    workingCopySourceRef = EntityRef.EMPTY,
                    definition = stringDef,
                    enabled = false,
                    autoStartEnabled = false,
                    autoDeleteEnabled = true,
                    action = BpmnProcessDefActions.DEPLOY.toString(),
                    sectionRef = EntityRef.EMPTY,
                    imageBytes = null
                )

                recordsService.mutate("${AppName.EPROC}/${BpmnProcessDefRecords.ID}@", bpmnMutateRecord)
            }

        return "Redeployed ${defs.size} process definitions"
    }
}
