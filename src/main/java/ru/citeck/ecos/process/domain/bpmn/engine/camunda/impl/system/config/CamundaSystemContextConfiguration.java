package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.config;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.interceptors.ExecuteJobAsSystemInterceptor;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.interceptors.InvokeScriptAsSystemDelegateInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
public class CamundaSystemContextConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setDelegateInterceptor(new InvokeScriptAsSystemDelegateInterceptor());
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<CommandInterceptor> interceptors = processEngineConfiguration.getCustomPreCommandInterceptorsTxRequired();
        if (interceptors == null) {
            interceptors = new ArrayList<>();
        }

        interceptors.add(new ExecuteJobAsSystemInterceptor());

        processEngineConfiguration.setCustomPreCommandInterceptorsTxRequired(interceptors);
    }

}
