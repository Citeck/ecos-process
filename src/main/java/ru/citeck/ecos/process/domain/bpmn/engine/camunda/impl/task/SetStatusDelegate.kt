package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.status.constants.StatusConstants
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getNotBlankDocumentRef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

class SetStatusDelegate : JavaDelegate {

    var status: Expression? = null

    private lateinit var recordsService: RecordsService
    private lateinit var document: RecordRef

    private fun init(execution: DelegateExecution) {
        recordsService = AppContext.getBean(RecordsService::class.java)
        document = execution.getNotBlankDocumentRef()
    }

    override fun execute(execution: DelegateExecution) {
        init(execution)

        val statusValue = status?.expressionText ?: ""
        if (statusValue.isBlank()) {
            error("Status value is empty")
        }

        AuthContext.runAsSystem {
            val recordAtts = RecordAtts(document)
            recordAtts.setAtt(StatusConstants.ATT_STATUS, statusValue)

            recordsService.mutate(recordAtts)
        }
    }
}
