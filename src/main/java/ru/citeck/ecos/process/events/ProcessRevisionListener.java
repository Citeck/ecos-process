package ru.citeck.ecos.process.events;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.repository.ProcessRevisionRepository;

import java.util.Comparator;

/**
 * Listener of persist ProcessRevision entity and increment it's version
 */
@Component
@RequiredArgsConstructor
public class ProcessRevisionListener extends AbstractMongoEventListener<ProcessRevision> {

    private final ProcessRevisionRepository processRevisionRepository;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ProcessRevision> event) {
        processRevisionRepository
            .findProcessRevisionsByProcessId(event.getSource().getProcess().getId())
            .stream()
            .max(Comparator.comparingInt(ProcessRevision::getVersion))
            .ifPresent(rev -> {
                event.getSource().setVersion(rev.getVersion() + 1);
            });

    }

}
