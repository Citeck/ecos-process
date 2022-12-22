package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

/**
 * Implement this interface to add service to camunda engine
 */
interface CamundaProcessEngineService {

    /**
     * @return The service key by which the service will be available from the camunda engine
     */
    fun getKey(): String
}
