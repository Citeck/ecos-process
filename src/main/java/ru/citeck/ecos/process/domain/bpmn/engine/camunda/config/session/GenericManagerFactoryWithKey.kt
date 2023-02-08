package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.session

import org.camunda.bpm.engine.impl.ProcessEngineLogger
import org.camunda.bpm.engine.impl.interceptor.Session
import org.camunda.bpm.engine.impl.interceptor.SessionFactory

class GenericManagerFactoryWithKey : SessionFactory {

    companion object {
        private val LOG = ProcessEngineLogger.PERSISTENCE_LOGGER
    }

    private val key: Class<out Session>
    private val implementation: Class<out Session>

    constructor(key: Class<out Session>, implementation: Class<out Session>) {
        if (!key.isAssignableFrom(implementation)) {
            error("Implementation should be descendant of key. Key: $key Implementation: $implementation")
        }
        this.key = key
        this.implementation = implementation
    }

    constructor(implementation: Class<out Session>) : this(implementation, implementation)

    override fun getSessionType(): Class<*> {
        return key
    }

    override fun openSession(): Session? {
        return try {
            implementation.newInstance()
        } catch (e: Exception) {
            throw LOG.instantiateSessionException(implementation.name, e)
        }
    }
}
