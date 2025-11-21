package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.config;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.interceptors.ExecuteJobAsSystemInterceptor;
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
public class CamundaSystemContextConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    private final ProcUtils procUtils;

    public CamundaSystemContextConfiguration(ProcUtils procUtils) {
        this.procUtils = procUtils;
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<CommandInterceptor> interceptors = processEngineConfiguration.getCustomPostCommandInterceptorsTxRequired();
        if (interceptors == null) {
            interceptors = new ArrayList<>();
        }

        interceptors.add(new ExecuteJobAsSystemInterceptor(procUtils));

        processEngineConfiguration.setCustomPostCommandInterceptorsTxRequired(interceptors);
    }
}
