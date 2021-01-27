package ru.citeck.ecos.process.domain.common.repo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityUuid {
    private Integer tnt;
    private UUID id;
}
