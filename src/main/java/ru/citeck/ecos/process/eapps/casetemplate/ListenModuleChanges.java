package ru.citeck.ecos.process.eapps.casetemplate;

import ru.citeck.ecos.process.domain.CaseTemplateEntity;

import java.util.function.Consumer;

public interface ListenModuleChanges<T> {

    void perform(CaseTemplateEntity entity);
}
