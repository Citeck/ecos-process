package ru.citeck.ecos.process.domain.bpmn.engine.camunda.interceptors;

import org.camunda.bpm.engine.impl.delegate.DefaultDelegateInterceptor;
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation;
import org.camunda.bpm.engine.impl.delegate.ScriptInvocation;
import ru.citeck.ecos.context.lib.auth.AuthContext;

/**
 * @author Roman Makarskiy
 */
public class InvokeScriptAsSystemDelegateInterceptor extends DefaultDelegateInterceptor {

    @Override
    public void handleInvocation(DelegateInvocation invocation) throws Exception {
        if (invocation instanceof ScriptInvocation) {
            Exception ex = AuthContext.runAsSystem(() -> {
                try {
                    super.handleInvocation(invocation);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            });

            if (ex != null) {
                throw ex;
            }
        } else {
            super.handleInvocation(invocation);
        }
    }
}
