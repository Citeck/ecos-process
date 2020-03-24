package ru.citeck.ecos.process.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.records2.RecordRef;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseTemplateDto {

    private String id;
    private RecordRef ecosTypeRef;
    private byte[] xmlContent;
}
