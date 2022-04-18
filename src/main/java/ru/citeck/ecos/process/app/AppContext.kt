package ru.citeck.ecos.process.app

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class AppContext : ApplicationContextAware {

    companion object {

        private var applicationContext: ApplicationContext? = null

        fun <T : Any> getBean(beanClass: Class<out T>): T {
            return applicationContext!!.getBean(beanClass)
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        AppContext.applicationContext = applicationContext
    }
}
