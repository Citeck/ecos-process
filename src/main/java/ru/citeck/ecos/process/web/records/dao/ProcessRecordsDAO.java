package ru.citeck.ecos.process.web.records.dao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.web.records.record.ProcessRecord;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessRecordsDAO extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<ProcessRecord>, LocalRecordsMetaDAO<ProcessRecord> {

    @Getter
    public final String id = "process";

    @Override
    public RecordsQueryResult<ProcessRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        return null;
    }

    @Override
    public List<ProcessRecord> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
        return null;
    }
}
