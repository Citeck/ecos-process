package ru.citeck.ecos.process.domain.procdef.command.finddef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;

@Data
@CommandType("find-proc-def")
@NoArgsConstructor
@AllArgsConstructor
public class FindProcDef {

    private String procType;
    private EntityRef ecosTypeRef;
    private List<String> alfTypes;
    private String workspace;

    public FindProcDef(String procType, EntityRef ecosTypeRef, List<String> alfTypes) {
        this.procType = procType;
        this.ecosTypeRef = ecosTypeRef;
        this.alfTypes = alfTypes;
        this.workspace = "";
    }
}
