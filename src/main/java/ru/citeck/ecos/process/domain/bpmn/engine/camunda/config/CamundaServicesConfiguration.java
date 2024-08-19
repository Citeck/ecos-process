package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaProcessEngineService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Makarskiy
 */
@Slf4j
public class CamundaServicesConfiguration extends AbstractCamundaConfiguration
    implements CamundaProcessEngineConfiguration {

    private List<CamundaProcessEngineService> processEngineServices = Collections.emptyList();

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setBeans(getCompletedEngineBeans(processEngineConfiguration));
    }

    private Map<Object, Object> getCompletedEngineBeans(ProcessEngineConfigurationImpl cfg) {
        Map<Object, Object> beans = cfg.getBeans();
        if (MapUtils.isEmpty(beans)) {
            beans = new HashMap<>();
        }

        for (CamundaProcessEngineService s : processEngineServices) {
            log.info("Added camunda process engine service: " + s.getKey());
            beans.put(s.getKey(), s);
        }

        return beans;
    }

    @Autowired(required = false)
    public void setProcessEngineServices(List<CamundaProcessEngineService> processEngineServices) {
        if (CollectionUtils.isNotEmpty(processEngineServices)) {
            this.processEngineServices = processEngineServices;
        }
    }
}
