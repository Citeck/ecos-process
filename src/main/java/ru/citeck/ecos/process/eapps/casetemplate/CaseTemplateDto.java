package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "data")
public class CaseTemplateDto {

    private String filePath;
    private byte[] data;
}
