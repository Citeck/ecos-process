package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.computed.script.AttValueScriptCtx
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService

/**
 * @author Roman Makarskiy
 */
@Component("Records")
class CamundaRecordsScriptComponent(
    private val recordsScriptService: RecordsScriptService
) : CamundaProcessEngineService {

    override fun getKey(): String {
        return "Records"
    }

    fun get(record: Any): AttValueScriptCtx {
        return recordsScriptService.get(record)
    }

    fun query(query: Any?, attributes: Any?): Any {
        return recordsScriptService.query(query, attributes)
    }

}
