package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getNotBlankDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.webapp.api.entity.EntityRef

class SetStatusDelegate : JavaDelegate {

    var status: Expression? = null

    private lateinit var statusSetter: CamundaStatusSetter
    private lateinit var document: EntityRef

    private fun init(execution: DelegateExecution) {
        statusSetter = AppContext.getBean(CamundaStatusSetter::class.java)
        document = execution.getNotBlankDocumentRef()
    }

    override fun execute(execution: DelegateExecution) {
        init(execution)

        val statusValue = status?.expressionText ?: ""
        if (statusValue.isBlank()) {
            error("Status value is empty")
        }

        AuthContext.runAsSystem {
            statusSetter.setStatus(document, statusValue)
        }
    }
}
