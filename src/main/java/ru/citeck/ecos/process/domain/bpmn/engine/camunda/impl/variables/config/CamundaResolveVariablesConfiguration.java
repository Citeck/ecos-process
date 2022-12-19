package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.resolver.CamundaEcosVariablesScriptResolverFactory;
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProps;

import java.util.List;

/**
 * @author Roman Makarskiy
 */
@Slf4j
public class CamundaResolveVariablesConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    private final EcosWebAppProps ecosWebAppProps;

    public CamundaResolveVariablesConfiguration(EcosWebAppProps ecosWebAppProps) {
        this.ecosWebAppProps = ecosWebAppProps;
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<ResolverFactory> resolverFactories = processEngineConfiguration.getResolverFactories();
        resolverFactories.add(new CamundaEcosVariablesScriptResolverFactory(ecosWebAppProps));
    }
}
