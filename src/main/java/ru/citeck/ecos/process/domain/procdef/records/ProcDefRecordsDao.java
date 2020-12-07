package ru.citeck.ecos.process.domain.procdef.records;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.ModuleRef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProcDefRecordsDao extends LocalRecordsDao
    implements LocalRecordsMetaDao<ProcDefRecord>,
    LocalRecordsQueryWithMetaDao<ProcDefRecord>,
    MutableRecordsLocalDao<ProcDefRecord> {

    private static final String ID = "procdef";

    private final ProcDefService procDefService;

    public ProcDefRecordsDao(ProcDefService procDefService) {
        setId(ID);
        this.procDefService = procDefService;
    }

    @Override
    public List<ProcDefRecord> getValuesToMutate(List<RecordRef> records) {
        return getLocalRecordsMeta(records, null);
    }

    @Override
    public RecordsQueryResult<ProcDefRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        RecordsQueryResult<ProcDefRecord> result = new RecordsQueryResult<>();
        int max = recordsQuery.getMaxItems();
        if (max <= 0) {
            max = 10000;
        }
        int skip = recordsQuery.getSkipCount();

        if ("predicate".equals(recordsQuery.getLanguage())) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            Collection<ProcDefRecord> types = procDefService.findAll(
                predicate,
                max,
                recordsQuery.getSkipCount()
            ).stream().map(ProcDefRecord::new)
                .collect(Collectors.toList());

            result.setRecords(types.stream()
                .map(ProcDefRecord::new)
                .collect(Collectors.toList()));
            result.setTotalCount(procDefService.getCount(predicate));

            return result;
        }

        if ("criteria".equals(recordsQuery.getLanguage())) {

            result.setRecords(procDefService.findAll(null, max, skip)
                .stream()
                .map(ProcDefRecord::new)
                .collect(Collectors.toList())
            );
            result.setTotalCount(procDefService.getCount());

            return result;
        }

        return new RecordsQueryResult<>();
    }

    @Override
    public RecordsMutResult save(List<ProcDefRecord> values) {

        RecordsMutResult result = new RecordsMutResult();
        values.forEach(dto -> {
            if (StringUtils.isBlank(dto.getId())) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory");
            }
            ProcDefDto saved = procDefService.uploadNewRev(dto);
            result.addRecord(new RecordMeta(ModuleRef.create(saved.getProcType(), saved.getId()).toString()));
        });

        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        throw new IllegalArgumentException("No supported");
    }

    @Override
    public List<ProcDefRecord> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(RecordRef::getId)
            .map(this::getProcDefByRef)
            .map(ProcDefRecord::new)
            .collect(Collectors.toList());
    }

    private ProcDefWithDataDto getProcDefByRef(String id) {

        if (id.isEmpty()) {
            return new ProcDefWithDataDto();
        }
        return procDefService.getProcessDefById(ModuleRef.valueOf(id))
            .orElseThrow(() -> new IllegalArgumentException("Process definition is not found by id: " + id));
    }
}
