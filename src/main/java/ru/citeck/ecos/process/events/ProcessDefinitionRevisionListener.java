package ru.citeck.ecos.process.events;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;

import java.util.Comparator;

/**
 *  Listener of persist ProcessDefinitionRevision entity and increment it's version
 */
@Component
@RequiredArgsConstructor
public class ProcessDefinitionRevisionListener extends AbstractMongoEventListener<ProcessDefinitionRevision> {

    private final ProcessDefinitionRevisionRepository processDefinitionRevisionRepository;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProcessDefinitionRevision> event) {
        processDefinitionRevisionRepository
            .findProcessDefinitionRevisionsByProcessDefinitionId(event.getSource().getProcessDefinitionId())
            .stream()
            .max(Comparator.comparingInt(ProcessDefinitionRevision::getVersion))
            .ifPresent(rev -> {
                event.getSource().setVersion(rev.getVersion() + 1);
            });

    }

}
