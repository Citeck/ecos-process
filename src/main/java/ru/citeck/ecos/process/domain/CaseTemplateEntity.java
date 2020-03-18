package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.citeck.ecos.records2.RecordRef;

@Document(collection = "caseTemplate")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseTemplateEntity {

    @Id
    private String id;

    private RecordRef ecosTypeRef;

    private byte[] xmlContent;
}
