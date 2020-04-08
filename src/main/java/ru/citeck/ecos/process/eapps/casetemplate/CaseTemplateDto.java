package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.records2.RecordRef;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseTemplateDto {

    private String id;
    private RecordRef typeRef;
    private byte[] xmlContent;
}
