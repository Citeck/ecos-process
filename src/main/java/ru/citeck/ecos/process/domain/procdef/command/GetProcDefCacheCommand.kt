package ru.citeck.ecos.process.domain.procdef.command

import org.springframework.stereotype.Component
import ru.citeck.ecos.commands.CommandExecutor
import ru.citeck.ecos.commands.annotation.CommandType
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Component
class GetProcDefCacheCommandExecutor(
    private val procDefService: ProcDefService
) : CommandExecutor<GetProcDefCacheCommand> {

    override fun execute(command: GetProcDefCacheCommand): Any? {
        return GetProcDefCacheCommandResp(procDefService.getCacheKey())
    }
}

@CommandType("get-proc-def-cache")
class GetProcDefCacheCommand

class GetProcDefCacheCommandResp(
    val cacheKey: String
)
