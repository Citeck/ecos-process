package ru.citeck.ecos.process.domain.procdef.records

import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordMeta
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.graphql.meta.value.MetaField
import ru.citeck.ecos.records2.graphql.meta.value.field.EmptyMetaField
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.request.delete.RecordsDelResult
import ru.citeck.ecos.records2.request.delete.RecordsDeletion
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult
import ru.citeck.ecos.records2.request.query.RecordsQuery
import ru.citeck.ecos.records2.request.query.RecordsQueryResult
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao
import java.util.function.Consumer
import java.util.stream.Collectors

@Component
class ProcDefRecordsDao(
    private val procDefService: ProcDefService
) : LocalRecordsDao(),
    LocalRecordsMetaDao<ProcDefRecord>,
    LocalRecordsQueryWithMetaDao<ProcDefRecord>,
    MutableRecordsLocalDao<ProcDefRecord> {

    companion object {
        private const val ID = "procdef"
    }

    override fun getValuesToMutate(records: List<RecordRef>): List<ProcDefRecord> {
        return getLocalRecordsMeta(records.toMutableList(), EmptyMetaField.INSTANCE)
    }


    override fun queryLocalRecords(recordsQuery: RecordsQuery, field: MetaField): RecordsQueryResult<ProcDefRecord> {

        val result = RecordsQueryResult<ProcDefRecord>()
        var max = recordsQuery.maxItems
        if (max <= 0) {
            max = 10000
        }
        val skip = recordsQuery.skipCount
        if ("predicate" == recordsQuery.language) {
            val predicate = recordsQuery.getQuery(Predicate::class.java)
            val types: Collection<ProcDefRecord> = procDefService.findAllWithData(
                predicate,
                max,
                recordsQuery.skipCount
            ).stream().map { model: ProcDefWithDataDto? -> ProcDefRecord(model) }
                .collect(Collectors.toList())
            result.setRecords(types.stream()
                .map { model: ProcDefRecord? -> ProcDefRecord(model) }
                .collect(Collectors.toList()))
            result.totalCount = procDefService.getCount(predicate)
            return result
        }
        if ("criteria" == recordsQuery.language) {
            result.setRecords(procDefService.findAllWithData(null, max, skip)
                .stream()
                .map { model: ProcDefWithDataDto? -> ProcDefRecord(model) }
                .collect(Collectors.toList())
            )
            result.totalCount = procDefService.getCount()
            return result
        }
        return RecordsQueryResult()
    }

    override fun save(values: List<ProcDefRecord>): RecordsMutResult {
        val result = RecordsMutResult()
        values.forEach(Consumer { dto: ProcDefRecord ->
            require(!StringUtils.isBlank(dto.id)) { "Parameter 'id' is mandatory" }
            val saved = procDefService.uploadNewRev(dto)
            result.addRecord(RecordMeta(ProcDefRef.create(saved.procType, saved.id).toString()))
        })
        return result
    }

    override fun delete(deletion: RecordsDeletion): RecordsDelResult {
        throw IllegalArgumentException("No supported")
    }

    override fun getLocalRecordsMeta(records: MutableList<RecordRef>, metaField: MetaField): MutableList<ProcDefRecord> {
        return records
            .map { getProcDefByRef(it.id) }
            .map { ProcDefRecord(it) }
            .toMutableList()
    }

    private fun getProcDefByRef(id: String): ProcDefWithDataDto {
        return if (id.isEmpty()) {
            ProcDefWithDataDto()
        } else {
            procDefService.getProcessDefById(ProcDefRef.valueOf(id))
                ?: error("Process definition is not found by id: $id")
        }
    }

    override fun getId(): String {
        return ID
    }
}
