package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseTemplateDto {

    private String filePath;
    private byte[] data;
}
