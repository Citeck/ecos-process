package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.interceptors;

import com.networknt.schema.utils.StringUtils;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.jetbrains.annotations.Nullable;
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * @author Roman Makarskiy
 */
public class ExecuteJobAsSystemInterceptor extends CommandInterceptor {

    private final ProcUtils procUtils;
    private final Function<ExecuteJobsCmd, String> getJobId;

    @SneakyThrows
    public ExecuteJobAsSystemInterceptor(ProcUtils procUtils) {
        this.procUtils = procUtils;
        // todo: find solution without reflection
        Field jobIdField = ExecuteJobsCmd.class.getDeclaredField("jobId");
        jobIdField.setAccessible(true);
        getJobId = (executeJobsCmd -> {
            try {
                Object jobIdValue = jobIdField.get(executeJobsCmd);
                if (jobIdValue instanceof String) {
                    return (String) jobIdValue;
                } else {
                    return "";
                }
            } catch (IllegalAccessException e) {
                return "";
            }
        });
    }

    @Override
    public <T> T execute(Command<T> command) {
        if (command instanceof ExecuteJobsCmd) {
            String procDefKey = getJobProcessKey((ExecuteJobsCmd) command);
            return procUtils.runAsWsSystemIfRequiredForProcDef(procDefKey, () -> next.execute(command));
        } else {
            return next.execute(command);
        }
    }

    @Nullable
    private String getJobProcessKey(ExecuteJobsCmd command) {
        String jobId = getJobId.apply(command);
        if (StringUtils.isBlank(jobId)) {
            return null;
        }
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext == null) {
            return null;
        }
        DbEntityManager dbEntityManager = commandContext.getDbEntityManager();
        if (dbEntityManager == null) {
            return null;
        }
        JobEntity jobEntity = dbEntityManager.selectById(JobEntity.class, jobId);
        if (jobEntity == null) {
            return null;
        }
        String procDefKey = jobEntity.getProcessDefinitionKey();
        if (StringUtils.isBlank(procDefKey)) {
            return null;
        }
        return procDefKey;
    }
}
