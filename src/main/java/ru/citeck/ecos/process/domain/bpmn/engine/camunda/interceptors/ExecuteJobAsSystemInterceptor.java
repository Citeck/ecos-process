package ru.citeck.ecos.process.domain.bpmn.engine.camunda.interceptors;

import org.camunda.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import ru.citeck.ecos.context.lib.auth.AuthContext;

/**
 * @author Roman Makarskiy
 */
public class ExecuteJobAsSystemInterceptor extends CommandInterceptor {

    @Override
    public <T> T execute(Command<T> command) {
        if (command instanceof ExecuteJobsCmd) {
            return AuthContext.runAsSystem(() -> next.execute(command));
        } else {
            return next.execute(command);
        }
    }
}
