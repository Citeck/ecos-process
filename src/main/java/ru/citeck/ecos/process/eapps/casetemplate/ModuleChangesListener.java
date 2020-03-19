package ru.citeck.ecos.process.eapps.casetemplate;

public interface ModuleChangesListener<T> {

    void perform(T dto);
}
