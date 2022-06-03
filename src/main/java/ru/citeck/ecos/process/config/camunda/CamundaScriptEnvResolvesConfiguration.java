package ru.citeck.ecos.process.config.camunda;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.scripting.env.ScriptEnvResolver;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.variables.EcosScriptEnvResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
public class CamundaScriptEnvResolvesConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<ScriptEnvResolver> resolvers = processEngineConfiguration.getEnvScriptResolvers();
        if (resolvers == null) {
            resolvers = new ArrayList<>();
        }

        resolvers.add(new EcosScriptEnvResolver());

        processEngineConfiguration.setEnvScriptResolvers(resolvers);
    }
}
