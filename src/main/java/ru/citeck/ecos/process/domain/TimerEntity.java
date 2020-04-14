package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "timer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "id_tnt_active_triggerTime", def = "{'id.tnt': 1, 'active': -1, 'triggerTime': 1}"),
    @CompoundIndex(name = "id_id", def = "{'id.id': 1}")
})
public class TimerEntity {
    private EntityUuid id;
    private Integer retryCounter = 0;
    private Instant triggerTime;
    private Boolean active = true;
    private TimerCommandEntity command;
    private String result;
}
