package ru.citeck.ecos.process.web.records.dao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.process.dto.CaseTemplateDto;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.process.service.ProcessService;
import ru.citeck.ecos.process.web.records.record.ProcessRecord;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.Elements;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessRecordsDAO extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<ProcessRecord>, LocalRecordsMetaDAO<ProcessRecord>,
    MutableRecordsDAO {

    @Getter
    public final String id = "process";

    private static final String LANGUAGE_EMPTY = "";
    private static final ProcessRecord EMPTY_RECORD = new ProcessRecord(new ProcessDto());

    private final ProcessService processService;

    @Override
    public RecordsQueryResult<ProcessRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<ProcessRecord> result = new RecordsQueryResult<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            recordsQuery.setSourceId(id);
            recordsQuery.setLanguage(LANGUAGE_EMPTY);

            Elements<RecordElement> elements = new RecordElements(recordsService, recordsQuery);

            Set<UUID> filteredResultIds = predicateService.filter(elements, predicate).stream()
                .map(e -> {
                    String refId = e.getRecordRef().getId();
                    return UUID.fromString(refId);
                })
                .collect(Collectors.toSet());

            result.addRecords(processService.getAll(filteredResultIds).stream()
                .map(ProcessRecord::new)
                .collect(Collectors.toList()));

        } else {
            result.setRecords(processService.getAll().stream()
                .map(ProcessRecord::new)
                .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public List<ProcessRecord> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
        if (list.size() == 1 && list.get(0).getId().isEmpty()) {
            return Collections.singletonList(EMPTY_RECORD);
        }

        return list.stream()
            .map(ref -> {
                String refId = ref.getId();
                UUID refIdUUID = UUID.fromString(refId);
                return new ProcessRecord(processService.getById(refIdUUID));
            })
            .collect(Collectors.toList());
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {
        RecordsMutResult result = new RecordsMutResult();

        List<RecordMeta> handledMeta = mutation.getRecords().stream()
            .map(this::handleMeta)
            .collect(Collectors.toList());

        mutation.setRecords(handledMeta);
        return result;
    }

    private RecordMeta handleMeta(RecordMeta meta) {

        String id = meta.getId().getId();
        UUID uuid = UUID.fromString(id);

        if (!StringUtils.isEmpty(id) ) {

            String tenantStr = meta.getAttribute("tenant").asText();
            Integer tenant = null;
            if (!StringUtils.isEmpty(tenantStr)) {
                tenant = Integer.parseInt(tenantStr);
            }

            String record = meta.getAttribute("record").asText();

            String revisionIdStr = meta.getAttribute("revisionId").asText();
            UUID revisionId = null;
            if (!StringUtils.isEmpty(revisionIdStr)) {
                revisionId = UUID.fromString(revisionIdStr);
            }

            String definitionRevIdStr = meta.getAttribute("definitionRevId").asText();
            UUID definitionRevId = null;
            if (!StringUtils.isEmpty(definitionRevIdStr)) {
                definitionRevId = UUID.fromString(definitionRevIdStr);
            }

            String activeStr = meta.getAttribute("active").asText();
            boolean active = Boolean.parseBoolean(activeStr);

            ProcessDto dto = new ProcessDto(uuid, tenant, record, revisionId, definitionRevId, active);
            processService.save(dto);
        }

        return meta;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion recordsDeletion) {
        RecordsDelResult result = new RecordsDelResult();
        recordsDeletion.getRecords().forEach(r -> {
            String id = r.getId();
            UUID uuid = UUID.fromString(id);
            processService.delete(uuid);
            result.addRecord(new RecordMeta(id));
        });
        return result;
    }
}
