package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.callactivity

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.common.toProcessKey
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.RefBinding
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.VariablesMappingPropagation
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class BpmnCallActivityDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val processRef: EntityRef,
    val calledActivity: String = processRef.toProcessKey(),
    val binding: RefBinding,

    val inVariablePropagation: VariablesMappingPropagation = VariablesMappingPropagation(),
    val outVariablePropagation: VariablesMappingPropagation = VariablesMappingPropagation(),

    val version: Int? = null,
    val versionTag: String? = null,

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val multiInstanceConfig: MultiInstanceConfig? = null
)
