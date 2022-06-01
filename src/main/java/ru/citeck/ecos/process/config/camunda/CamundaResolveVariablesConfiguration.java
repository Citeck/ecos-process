package ru.citeck.ecos.process.config.camunda;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.variables.document.CamundaEcosVariablesScriptResolverFactory;

import java.util.List;

/**
 * @author Roman Makarskiy
 */
@Slf4j
public class CamundaResolveVariablesConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<ResolverFactory> resolverFactories = processEngineConfiguration.getResolverFactories();
        resolverFactories.add(new CamundaEcosVariablesScriptResolverFactory());
    }
}
