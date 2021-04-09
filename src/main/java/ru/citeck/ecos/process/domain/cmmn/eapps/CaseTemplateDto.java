package ru.citeck.ecos.process.domain.cmmn.eapps;

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
