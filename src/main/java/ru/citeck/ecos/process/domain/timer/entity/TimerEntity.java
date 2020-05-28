package ru.citeck.ecos.process.domain.timer.entity;

import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import ru.citeck.ecos.process.domain.common.entity.EntityUuid;

import java.time.Instant;

@Data
@Document(collection = "timer")
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(
        name = "timer_active_triggerTime_idx",
        def = "{'active': -1, 'triggerTime': 1}"
    )
})
public class TimerEntity {

    @Id
    private EntityUuid id;

    private Integer retryCounter = 0;
    private Instant triggerTime;
    private boolean active = true;
    private String command;
    private String result;
}
