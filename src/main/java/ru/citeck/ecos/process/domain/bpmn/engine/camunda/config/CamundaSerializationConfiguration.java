package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;

/**
 * @author Roman Makarskiy
 */
@Slf4j
public class CamundaSerializationConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setDefaultSerializationFormat(Variables.SerializationDataFormats.JSON.getName());
    }
}
