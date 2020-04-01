package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.citeck.ecos.records2.RecordRef;

@Document(collection = "case_template")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseTemplate {

    @Id
    private String id;

    private RecordRef typeRef;

    private byte[] xmlContent;
}
