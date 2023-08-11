package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnMutateDataProcessor {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun getCompletedMutateData(mutateRecord: BpmnProcessDefRecords.BpmnMutateRecord): BpmnMutateData {

        val (statedInitialDefinition, saveAsDraft) = mutateRecord.getStatedDefinition()

        var recordId = mutateRecord.id
        var ecosType = mutateRecord.ecosType
        var formRef = mutateRecord.formRef
        var name = mutateRecord.name
        var processDefId = mutateRecord.processDefId
        var enabled = mutateRecord.enabled
        var autoStartEnabled = mutateRecord.autoStartEnabled
        var newEcosDefinition = ""
        var newCamundaDefinitionStr = ""

        if (statedInitialDefinition.isNotBlank()) {
            if (saveAsDraft) {
                // parse definition data from raw bpmn

                val draftDefinition = BpmnXmlUtils.readFromString(statedInitialDefinition)

                ecosType = EntityRef.valueOf(draftDefinition.otherAttributes[BPMN_PROP_ECOS_TYPE])
                formRef = EntityRef.valueOf(draftDefinition.otherAttributes[BPMN_PROP_FORM_REF])
                name =
                    Json.mapper.convert(draftDefinition.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java)
                        ?: MLText()
                processDefId = draftDefinition.otherAttributes[BPMN_PROP_PROCESS_DEF_ID]!!
                enabled = draftDefinition.otherAttributes[BPMN_PROP_ENABLED].toBoolean()
                autoStartEnabled = draftDefinition.otherAttributes[BPMN_PROP_AUTO_START_ENABLED].toBoolean()

                newEcosDefinition = statedInitialDefinition
            } else {
                // Parse definition data from Ecos BPMN format

                validateEcosBpmnFormat(statedInitialDefinition)

                val ecosBpmnDefinition = BpmnIO.importEcosBpmn(statedInitialDefinition)

                debugLogEcosAndCamundaDefStr(ecosBpmnDefinition)

                ecosType = ecosBpmnDefinition.ecosType
                formRef = ecosBpmnDefinition.formRef
                name = ecosBpmnDefinition.name
                processDefId = ecosBpmnDefinition.id
                enabled = ecosBpmnDefinition.enabled
                autoStartEnabled = ecosBpmnDefinition.autoStartEnabled

                newEcosDefinition = BpmnIO.exportEcosBpmnToString(ecosBpmnDefinition)
                newCamundaDefinitionStr = BpmnIO.exportCamundaBpmnToString(ecosBpmnDefinition)
            }

            if (recordId.isBlank()) {
                recordId = processDefId
            }
        }

        if (processDefId.isBlank()) {
            error("processDefId is missing")
        }

        return BpmnMutateData(
            recordId = recordId,
            processDefId = processDefId,
            name = name,
            ecosType = ecosType,
            formRef = formRef,
            enabled = enabled,
            autoStartEnabled = autoStartEnabled,
            sectionRef = mutateRecord.sectionRef,
            createdFromVersion = mutateRecord.createdFromVersion,
            image = mutateRecord.imageBytes,
            newEcosDefinition = if (newEcosDefinition.isBlank()) {
                null
            } else {
                newEcosDefinition.toByteArray()
            },
            newCamundaDefinitionStr = newCamundaDefinitionStr,
            saveAsDraft = saveAsDraft
        )
    }

    private fun BpmnProcessDefRecords.BpmnMutateRecord.getStatedDefinition(): Pair<String, Boolean> {
        val initialDefinition = definition

        return if (initialDefinition.isNullOrBlank()) {
            "" to false
        } else {
            val bpmnDef = BpmnXmlUtils.readFromString(initialDefinition)

            val defStateAtt = bpmnDef.otherAttributes[BPMN_PROP_DEF_STATE]
            val initialDefState = if (defStateAtt.isNullOrBlank()) {
                null
            } else {
                ProcDefRevDataState.valueOf(defStateAtt)
            }

            val isRaw = action == BpmnProcessDefActions.DRAFT.name ||
                (action.isBlank() && initialDefState == ProcDefRevDataState.RAW)

            val state = if (isRaw) {
                ProcDefRevDataState.RAW
            } else {
                ProcDefRevDataState.CONVERTED
            }

            bpmnDef.otherAttributes[BPMN_PROP_DEF_STATE] = state.name

            BpmnXmlUtils.writeToString(bpmnDef) to isRaw
        }
    }

    private fun validateEcosBpmnFormat(definition: String) {
        val ecosBpmnDef = BpmnIO.importEcosBpmn(definition)
        BpmnIO.exportEcosBpmn(ecosBpmnDef)
        BpmnIO.exportCamundaBpmn(ecosBpmnDef)
    }

    private fun debugLogEcosAndCamundaDefStr(ecosBpmnDef: BpmnDefinitionDef) {
        if (log.isDebugEnabled) {
            val ecosBpmnStr = BpmnIO.exportEcosBpmnToString(ecosBpmnDef)
            log.debug { "exportEcosBpmnToString:\n$ecosBpmnStr" }

            val camundaStr = BpmnIO.exportCamundaBpmnToString(ecosBpmnDef)
            log.debug { "exportCamundaBpmnToString:\n$camundaStr" }
        }
    }
}

data class BpmnMutateData(
    val recordId: String,
    val processDefId: String,
    val name: MLText,
    val ecosType: EntityRef,
    val formRef: EntityRef,
    val enabled: Boolean,
    val autoStartEnabled: Boolean,
    val sectionRef: EntityRef,
    val createdFromVersion: EntityRef = EntityRef.EMPTY,
    val image: ByteArray?,
    val newEcosDefinition: ByteArray?,
    val newCamundaDefinitionStr: String,
    val saveAsDraft: Boolean
)
