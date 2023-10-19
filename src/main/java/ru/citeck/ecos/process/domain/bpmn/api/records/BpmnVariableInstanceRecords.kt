package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity
import org.camunda.bpm.engine.runtime.VariableInstanceQuery
import org.camunda.bpm.engine.variable.value.SerializableValue
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

@Component
class BpmnVariableInstanceRecords(
    private val camundaRuntimeService: RuntimeService
) : AbstractRecordsDao(), RecordsQueryDao, RecordAttsDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "bpmn-variable-instance"

        private const val ATT_NAME = "name"
        private const val ATT_TYPE = "type"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, BpmnVariableInstanceQuery::class.java)
        if (query.processInstance.isEmpty()) {
            return RecsQueryRes()
        }

        val totalCount = camundaRuntimeService.createVariableInstanceQuery()
            .applyPredicate(predicate)
            .count()

        val variables = camundaRuntimeService.createVariableInstanceQuery()
            .applyPredicate(predicate)
            .applySort(recsQuery)
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.id)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(variables)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordAtts(recordId: String): Any? {
        return camundaRuntimeService.createVariableInstanceQuery()
            .variableId(recordId)
            .singleResult()?.let {
                BpmnVariableInstanceRecord(it as VariableInstanceEntity)
            }
    }

    private fun VariableInstanceQuery.applyPredicate(pred: Predicate): VariableInstanceQuery {
        val query = PredicateUtils.convertToDto(pred, BpmnVariableInstanceQuery::class.java)

        return this.apply {
            if (query.processInstance.getLocalId().isNotBlank()) {
                processInstanceIdIn(query.processInstance.getLocalId())
            }

            if (query.name.isNotBlank()) {
                variableNameLike("%${query.name}%")
            }

            if (query.activityInstanceId.isNotBlank()) {
                activityInstanceIdIn(query.activityInstanceId)
            }
        }
    }

    private fun VariableInstanceQuery.applySort(recsQuery: RecordsQuery): VariableInstanceQuery {
        if (recsQuery.sortBy.isEmpty()) {
            return this.apply {
                orderByVariableName()
                asc()
            }
        }

        val sortBy = recsQuery.sortBy[0]
        return this.apply {
            when (sortBy.attribute) {
                ATT_TYPE -> orderByVariableType()
                ATT_NAME -> orderByVariableName()
                else -> orderByVariableName()
            }

            if (sortBy.ascending) {
                asc()
            } else {
                desc()
            }
        }
    }

    private inner class BpmnVariableInstanceRecord(
        private val variableInstance: VariableInstanceEntity,

        val id: String = variableInstance.id
    ) {

        private val _typedValueInfo: TypedValueInfo by lazy {
            variableInstance.typedValue?.let {
                val typedInfo = it.type.getValueInfo(it)
                TypedValueInfo(
                    objectTypeName = typedInfo["objectTypeName"] as String? ?: "",
                    serializationDataFormat = typedInfo["serializationDataFormat"] as String? ?: "",
                )
            } ?: TypedValueInfo(
                objectTypeName = variableInstance.textValue2 ?: "",
                serializationDataFormat = variableInstance.serializerName ?: "",
                errorMsg = variableInstance.errorMessage ?: ""
            )
        }

        @AttName("typedValueInfo")
        fun getTypedValueInfo(): TypedValueInfo {
            return _typedValueInfo
        }

        @AttName(ATT_NAME)
        fun getName(): String {
            return variableInstance.name
        }

        @AttName(ATT_TYPE)
        fun getType(): String {
            return variableInstance.typeName
        }

        @AttName("activityInstanceId")
        fun getActivityInstanceId(): String {
            return variableInstance.activityInstanceId
        }

        @AttName("value")
        fun getValue(): Any {
            return when (variableInstance.typeName) {
                "json" -> variableInstance.value?.toString() ?: ""
                "xml" -> "Xml object"
                "object" -> variableInstance.textValue2 ?: ""
                else -> variableInstance.value ?: ""
            }
        }

        @AttName("serializableValue")
        fun getSerializableValue(): SerializableValueData? {
            if (variableInstance.typeName == "xml") {
                val xmlValue = variableInstance.byteArrayValue.toString(StandardCharsets.UTF_8)
                return SerializableValueData(
                    serialized = xmlValue
                )
            }

            if (_typedValueInfo.errorMsg.isNotBlank()) {
                return SerializableValueData(
                    serialized = variableInstance.byteArrayValue.toString(
                        StandardCharsets.UTF_8
                    )
                )
            }

            val typedValue = variableInstance.typedValue as? SerializableValue ?: return null
            return SerializableValueData(
                serialized = typedValue.valueSerialized,
                deserialized = typedValue.value?.toString() ?: ""
            )
        }
    }

    private data class TypedValueInfo(
        var objectTypeName: String = "",
        var serializationDataFormat: String = "",
        var errorMsg: String = ""
    )

    private data class SerializableValueData(
        var serialized: String = "",
        var deserialized: String = ""
    )

    data class BpmnVariableInstanceQuery(
        var processInstance: EntityRef = EntityRef.EMPTY,
        var name: String = "",
        var activityInstanceId: String = ""
    )
}
