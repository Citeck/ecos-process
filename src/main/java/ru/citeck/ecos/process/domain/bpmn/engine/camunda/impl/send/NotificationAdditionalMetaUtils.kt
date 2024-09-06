package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import org.camunda.bpm.engine.delegate.VariableScope
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.collections.component1
import kotlin.collections.component2

private const val VAR_FORCE_STR_PREFIX = "!str_"
private const val VAR_CURRENT_RUN_AS_USER = "currentRunAsUser"
private const val PERSON_SOURCE_ID = "person"
private const val VAR_PROCESS = "process"

fun getBaseNotificationAdditionalMeta(
    variablesScope: VariableScope, metaFromUserInput: Map<String, Any>
): Map<String, Any> {
    val processVariables = variablesScope.getPreparedProcessVariables().toMutableMap()
    processVariables[VAR_CURRENT_RUN_AS_USER] = AuthContext.getCurrentRunAsUserRef()

    val additionalMeta = metaFromUserInput.map { (key, value) ->
        val valueToPut = when (value) {
            is String -> {
                if (value.startsWith(VAR_FORCE_STR_PREFIX)) {
                    value.removePrefix(VAR_FORCE_STR_PREFIX)
                } else {
                    EntityRef.valueOf(value)
                }
            }

            else -> value
        }
        key to valueToPut
    }.toMap().toMutableMap()

    additionalMeta[VAR_PROCESS] = processVariables

    return additionalMeta
}

private fun VariableScope.getPreparedProcessVariables(): Map<String, Any> {
    return variables.map { (key, value) ->
        val valueToPut = when (value) {
            is BpmnDataValue -> value.asDataValue()
            else -> value
        }
        key to valueToPut
    }.toMap()
}

private fun AuthContext.getCurrentRunAsUserRef(): EntityRef {
    val user = getCurrentRunAsUser()
    if (user.isBlank()) {
        return EntityRef.EMPTY
    }
    return EntityRef.create(AppName.EMODEL, PERSON_SOURCE_ID, user)
}
