package ru.citeck.ecos.process.domain.bpmn.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.mixin.AttMixin
import java.time.Duration

class BpmnProcessElementsMixin(private val records: RecordsService) : AttMixin {

    companion object {
        const val ATT_PROC_DEF_VERSION_LABEL = "procDefVersionLabel"

        val PROVIDED_ATTS = listOf(ATT_PROC_DEF_VERSION_LABEL)
    }

    private val cache: LoadingCache<ProcElementAtts, DataValue> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(100)
        .build(CacheLoader.from { key -> evalVersionLabel(key) })

    override fun getAtt(path: String, value: AttValueCtx): Any? {
        if (path != ATT_PROC_DEF_VERSION_LABEL) {
            return null
        }
        val depAtts = value.getAtts(ProcElementAtts::class.java)
        if (depAtts.procDeploymentVersion.isNullOrBlank() || depAtts.procDefId.isBlank()) {
            return "1.0"
        }
        val res = cache.getUnchecked(depAtts)
        if (res.isNull()) {
            return "1.0"
        }
        return res
    }

    private fun evalVersionLabel(key: ProcElementAtts?): DataValue {

        key ?: return DataValue.NULL

        val query = RecordsQuery.create {
            withSourceId("alfresco/")
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
            withQuery(
                Predicates.and(
                    Predicates.eq("type", "ecosbpm:deploymentInfo"),
                    Predicates.eq("ecosbpm:deploymentProcDefId", key.procDefId),
                    Predicates.eq("ecosbpm:deploymentVersion", key.procDeploymentVersion)
                )
            )
        }

        return records.queryOne(query, "ecosbpm:deploymentProcDefVersion")
    }

    override fun getProvidedAtts(): Collection<String> {
        return PROVIDED_ATTS
    }

    class ProcElementAtts(
        val procDeploymentVersion: String?,
        val procDefId: String,
        val engine: String?
    )
}
