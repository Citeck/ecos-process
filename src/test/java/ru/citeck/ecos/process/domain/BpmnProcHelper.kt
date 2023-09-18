package ru.citeck.ecos.process.domain

import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BPMN_PROCESS_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefActions
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct

private val typeRef = TypeUtils.getTypeRef("type0")

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnProcHelper(
    val recordsService: RecordsService,
    val procDefRepo: ProcDefRepository,
    val procDefRevRepo: ProcDefRevRepository,
) {
    @PostConstruct
    private fun init() {
        helper = this
    }
}

private lateinit var helper: BpmnProcHelper

fun getBpmnProcessDefDto(resource: String, id: String): NewProcessDefDto {
    return NewProcessDefDto(
        id,
        MLText.EMPTY,
        BPMN_PROC_TYPE,
        "xml",
        "{http://www.citeck.ru/model/content/idocs/1.0}type",
        typeRef,
        EntityRef.EMPTY,
        ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
            .toByteArray(StandardCharsets.UTF_8),
        null,
        enabled = true,
        autoStartEnabled = false,
        sectionRef = EntityRef.EMPTY
    )
}

fun saveAndDeployBpmnFromResource(resource: String, id: String) {
    saveAndDeployBpmnFromString(
        ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8),
        id
    )
}

fun saveBpmnWithAction(resource: String, id: String, action: BpmnProcessDefActions?) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)

        action?.let {
            this["action"] = it.name
        }
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveBpmnWithActionAndReplaceDefinition(
    resource: String,
    id: String,
    action: BpmnProcessDefActions?,
    replace: Pair<String, String>
) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
            .replace(replace.first, replace.second)

        action?.let {
            this["action"] = it.name
        }
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveAndDeployBpmnFromString(bpmnData: String, id: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = bpmnData
        this["action"] = BpmnProcessDefActions.DEPLOY.toString()
    }

    AuthContext.runAsSystem {
        helper.recordsService.mutate(recordAtts)
    }
}

fun saveAndDeployDmnFromResource(resource: String, id: String) {
    saveAndDeployDmnFromString(
        ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8),
        id
    )
}

fun saveAndDeployDmnFromString(dmnData: String, id: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, DMN_DEF_RECORDS_SOURCE_ID, "")).apply {
        this["defId"] = id
        this["definition"] = dmnData
        this["action"] = DmnDefActions.DEPLOY.toString()
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveAndDeployBpmn(elementFolder: String, id: String) {
    saveAndDeployBpmnFromResource(
        "test/bpmn/elements/$elementFolder/$id.bpmn.xml",
        id
    )
}

fun deleteAllProcDefinitions() {
    helper.procDefRevRepo.deleteAll()
    helper.procDefRepo.deleteAll()
}

fun buildProcDefXml(id: String): ByteArray {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                              id="Definitions_$id" targetNamespace="http://bpmn.io/schema/bpmn"
                              xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                              exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="8.2.0"
                              ecos:processDefId="$id">
              <bpmn:process id="Process_$id" isExecutable="false">
                <bpmn:startEvent id="StartEvent_0lly8qf">
                  <bpmn:outgoing>Flow_15brz3r</bpmn:outgoing>
                </bpmn:startEvent>
                <bpmn:sequenceFlow id="Flow_15brz3r" sourceRef="StartEvent_0lly8qf" targetRef="Event_0fitnzy"/>
                <bpmn:endEvent id="Event_0fitnzy">
                  <bpmn:incoming>Flow_15brz3r</bpmn:incoming>
                </bpmn:endEvent>
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
}
