package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional

import org.camunda.bpm.engine.runtime.VariableInstance
import org.camunda.bpm.engine.variable.value.TypedValue

object ConditionalVariableInstanceStub : VariableInstance {
    override fun getId(): String {
        return "variableStub"
    }

    override fun getName(): String {
        return "variableStub"
    }

    override fun getTypeName(): String {
        error("Not supported")
    }

    override fun getValue(): Any {
        error("Not supported")
    }

    override fun getTypedValue(): TypedValue {
        error("Not supported")
    }

    override fun getProcessInstanceId(): String {
        error("Not supported")
    }

    override fun getExecutionId(): String {
        error("Not supported")
    }

    override fun getProcessDefinitionId(): String {
        error("Not supported")
    }

    override fun getCaseInstanceId(): String {
        error("Not supported")
    }

    override fun getCaseExecutionId(): String {
        error("Not supported")
    }

    override fun getTaskId(): String {
        error("Not supported")
    }

    override fun getBatchId(): String {
        error("Not supported")
    }

    override fun getActivityInstanceId(): String {
        error("Not supported")
    }

    override fun getErrorMessage(): String {
        error("Not supported")
    }

    override fun getTenantId(): String {
        error("Not supported")
    }
}
