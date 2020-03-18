package ru.citeck.ecos.process.web.records.dao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.service.CaseTemplateService;
import ru.citeck.ecos.process.web.records.record.CaseTemplateRecord;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.Elements;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.RecordElement;
import ru.citeck.ecos.records2.predicate.RecordElements;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CaseTemplateRecordsDAO extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<CaseTemplateRecord>, LocalRecordsMetaDAO<CaseTemplateRecord> {

    @Getter
    public final String id = "case-template";

    private static final String LANGUAGE_EMPTY = "";
    private static final CaseTemplateRecord EMPTY_RECORD = new CaseTemplateRecord(new CaseTemplateEntity());

    private final CaseTemplateService caseTemplateService;

    @Override
    public RecordsQueryResult<CaseTemplateRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<CaseTemplateRecord> result = new RecordsQueryResult<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            recordsQuery.setSourceId(id);
            recordsQuery.setLanguage(LANGUAGE_EMPTY);

            Elements<RecordElement> elements = new RecordElements(recordsService, recordsQuery);

            Set<String> filteredResultIds = predicateService.filter(elements, predicate).stream()
                .map(e -> e.getRecordRef().getId())
                .collect(Collectors.toSet());

            result.addRecords(caseTemplateService.getAll(filteredResultIds).stream()
                .map(CaseTemplateRecord::new)
                .collect(Collectors.toList()));

        } else {
            result.setRecords(caseTemplateService.getAll().stream()
                .map(CaseTemplateRecord::new)
                .collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public List<CaseTemplateRecord> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
        if (list.size() == 1 && list.get(0).getId().isEmpty()) {
            return Collections.singletonList(EMPTY_RECORD);
        }

        return list.stream()
            .map(ref -> new CaseTemplateRecord(caseTemplateService.get(ref.getId())))
            .collect(Collectors.toList());
    }
}
