package ru.citeck.ecos.process.domain.bpmn.api.records

import ecos.com.fasterxml.jackson210.databind.JavaType
import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity
import org.camunda.bpm.engine.runtime.VariableInstanceQuery
import org.camunda.bpm.engine.variable.value.SerializableValue
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class BpmnVariableInstanceRecords(
    private val camundaRuntimeService: RuntimeService
) : AbstractRecordsDao(), RecordsQueryDao, RecordAttsDao, RecordMutateDao, RecordDeleteDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "bpmn-variable-instance"

        private const val ATT_NAME = "name"
        private const val ATT_TYPE = "type"
        private const val ATT_VALUE = "value"
        private const val ATT_OBJECT_TYPE_NAME = "objectTypeName"
        private const val ATT_EXECUTION_ID = "executionId"

        private const val TYPE_STRING = "string"
        private const val TYPE_DATE = "date"
        private const val TYPE_INTEGER = "integer"
        private const val TYPE_BOOLEAN = "boolean"
        private const val TYPE_DOUBLE = "double"
        private const val TYPE_LONG = "long"
        private const val TYPE_OBJECT = "object"
        private const val TYPE_JSON = "json"
        private const val TYPE_XML = "xml"
        private const val TYPE_NULL = "null"
    }

    override fun getId() = ID

    override fun mutate(record: LocalRecordAtts): String {
        val mutateData = record.toMutateData()

        log.debug {
            "Mutate bpmn variable: \n${Json.mapper.toPrettyString(mutateData)}"
        }

        camundaRuntimeService.setVariableLocal(mutateData.executionId, mutateData.name, mutateData.value)

        return record.id.ifBlank {
            val variableInstance = camundaRuntimeService.createVariableInstanceQuery()
                .executionIdIn(mutateData.executionId)
                .variableName(mutateData.name)
                .singleResult() ?: error("Variable with name '${mutateData.name}' not found")

            variableInstance.id
        }
    }

    private fun LocalRecordAtts.toMutateData(): MutateValueData {
        val id = this.id

        val (executionId: String, name: String) = if (id.isNotBlank()) {
            val variable = camundaRuntimeService.createVariableInstanceQuery()
                .variableId(id)
                .singleResult() ?: error("Variable with id '$id' not found")
            variable.executionId to variable.name
        } else {
            this.getAtt(ATT_EXECUTION_ID).asText() to this.getAtt(ATT_NAME).asText()
        }

        if (this.id.isBlank() && executionId.isBlank()) {
            error("You must specify either 'id' or 'executionId' attribute: $this")
        }

        require(name.isNotBlank()) { "Variable name can't be blank: $this" }
        require(executionId.isNotBlank()) { "Execution id can't be blank: $this" }

        val objectTypeName = this.getAtt(ATT_OBJECT_TYPE_NAME).asText()

        val type: String? = when (this.getAtt(ATT_TYPE).asText().lowercase()) {
            TYPE_STRING -> String::class.java.typeName
            TYPE_DATE -> Date::class.java.typeName
            TYPE_INTEGER -> Int::class.java.typeName
            TYPE_BOOLEAN -> Boolean::class.java.typeName
            TYPE_DOUBLE -> Double::class.java.typeName
            TYPE_LONG -> Long::class.java.typeName
            TYPE_OBJECT -> {
                if (objectTypeName.isBlank()) {
                    error("Object type name can't be blank for type 'Object': $this")
                }
                objectTypeName
            }

            TYPE_JSON -> error("Json type is not supported for mutation")
            TYPE_NULL -> null
            else -> error("Unknown variable type: $this")
        }

        val valueToMutate: Any? = if (type != null) {
            val explicitType: JavaType = Json.mapper.getTypeFactory().constructFromCanonical(type)
                ?: error("Type not constructed from canonical: $this")
            val rawValue = this.getAtt(ATT_VALUE).asJavaObj() ?: error("Value is null: $this")

            val explicitValue: Any = Json.mapper.convert(rawValue, explicitType)
                ?: error("Failed to convert value: $rawValue to type: $explicitType")

            explicitValue
        } else {
            null
        }

        return MutateValueData(id, executionId, name, valueToMutate)
    }

    override fun delete(recordId: String): DelStatus {
        val variableInstance = camundaRuntimeService.createVariableInstanceQuery()
            .variableId(recordId)
            .singleResult() ?: error("Variable with id '$recordId' not found")

        camundaRuntimeService.removeVariable(variableInstance.executionId, variableInstance.name)

        return DelStatus.OK
    }

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

    private fun VariableInstanceQuery.applyPredicate(pred: Predicate): VariableInstanceQuery {
        val query = PredicateUtils.convertToDto(pred, BpmnVariableInstanceQuery::class.java)

        return this.apply {
            if (query.processInstance.getLocalId().isNotBlank()) {
                processInstanceIdIn(query.processInstance.getLocalId())
            }

            if (query.name.isNotBlank()) {
                variableNameLike("%${query.name}%")
            }

            val scopeId = query.scope.getLocalId()
            if (scopeId.isNotBlank()) {
                activityInstanceIdIn(query.scope.getLocalId())
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

    override fun getRecordAtts(recordId: String): Any? {
        return camundaRuntimeService.createVariableInstanceQuery()
            .variableId(recordId)
            .singleResult()?.let {
                BpmnVariableInstanceRecord(it as VariableInstanceEntity)
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

        @AttName("scope")
        fun getScope(): EntityRef {
            val activityInstanceId: String? = variableInstance.activityInstanceId

            if (activityInstanceId.isNullOrBlank()) {
                return EntityRef.EMPTY
            }

            return if (activityInstanceId.contains(":")) {
                EntityRef.create(
                    AppName.EPROC,
                    BpmnHistoricActivityInstanceRecords.ID,
                    activityInstanceId
                )
            } else {
                EntityRef.create(
                    AppName.EPROC,
                    BpmnProcessRecords.ID,
                    activityInstanceId
                )
            }
        }

        @AttName("value")
        fun getValue(): Any {
            return when (variableInstance.typeName) {
                TYPE_JSON -> variableInstance.value?.toString() ?: ""
                TYPE_XML -> "Xml object"
                TYPE_OBJECT -> variableInstance.textValue2 ?: ""
                else -> variableInstance.value ?: ""
            }
        }

        @AttName("serializableValue")
        fun getSerializableValue(): SerializableValueData? {
            if (variableInstance.typeName == TYPE_XML) {
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

    private data class MutateValueData(
        val variableId: String,
        var executionId: String,
        val name: String?,
        val value: Any?
    )

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
        var scope: EntityRef = EntityRef.EMPTY
    )
}
