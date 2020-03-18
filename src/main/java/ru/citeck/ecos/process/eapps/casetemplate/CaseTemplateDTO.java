package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.Data;

@Data
public class CaseTemplateDTO {

    private String id;
    private String ecosTypeRef;
    private byte[] xmlContent;
}
