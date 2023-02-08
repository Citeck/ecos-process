package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.events

import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionManager

class CustomEventSubscriptionManager : EventSubscriptionManager() {

    fun getCreatedSubscriptions(): List<EventSubscriptionEntity> {
        return this.createdSignalSubscriptions
    }

    override fun findEventSubscriptionById(id: String): EventSubscriptionEntity? {
        return super.findEventSubscriptionById(id) ?: createdSignalSubscriptions.find { it.id == id }
    }
}
