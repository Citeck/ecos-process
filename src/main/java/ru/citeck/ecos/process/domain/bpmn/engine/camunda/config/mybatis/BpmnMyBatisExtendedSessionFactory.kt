package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.mybatis

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration
import org.camunda.bpm.engine.impl.interceptor.CommandContextInterceptor
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor
import org.camunda.bpm.engine.impl.interceptor.LogInterceptor
import org.camunda.bpm.engine.spring.SpringTransactionInterceptor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.ResourceUtils
import java.io.InputStream

class BpmnMyBatisExtendedSessionFactory : StandaloneProcessEngineConfiguration() {

    companion object {
        private const val MY_BATIS_CONFIG_FILE = "classpath:camunda/mybatis/bpmnMyBatisConfiguration.xml"
    }

    override fun init() {
        throw IllegalArgumentException(
            "Normal 'init' on process engine only used for extended MyBatis mappings is not allowed."
        )
    }

    private lateinit var txnInterceptor: CommandInterceptor

    fun initFromProcessEngineConfiguration(
        processEngineConfiguration: ProcessEngineConfigurationImpl,
        transactionManager: PlatformTransactionManager
    ) {

        setDataSource(processEngineConfiguration.dataSource)
        setTransactionContextFactory(processEngineConfiguration.transactionContextFactory)
        setTransactionFactory(processEngineConfiguration.transactionFactory)
        isTransactionsExternallyManaged = true

        this.txnInterceptor = SpringTransactionInterceptor(
            transactionManager,
            TransactionTemplate.PROPAGATION_REQUIRED,
            this
        )

        initDataSource()
        initCommandContextFactory()
        initTransactionFactory()
        initTransactionContextFactory()
        initCommandExecutors()
        initSqlSessionFactory()
        initIncidentHandlers()
        initIdentityProviderSessionFactory()
        initSessionFactories()
    }

    /**
     * In order to always open a new command context set the property
     * "alwaysOpenNew" to true inside the CommandContextInterceptor.
     *
     * If you execute the custom queries inside the process engine
     * (for example in a service task), you have to do this.
     */
    override fun getDefaultCommandInterceptorsTxRequired(): Collection<CommandInterceptor> {
        val defaultCommandInterceptorsTxRequired: MutableList<CommandInterceptor> = ArrayList()
        defaultCommandInterceptorsTxRequired.add(LogInterceptor())
        defaultCommandInterceptorsTxRequired.add(txnInterceptor)
        defaultCommandInterceptorsTxRequired.add(CommandContextInterceptor(commandContextFactory, this, true))
        return defaultCommandInterceptorsTxRequired
    }

    override fun getMyBatisXmlConfigurationSteam(): InputStream {
        return ResourceUtils.getFile(MY_BATIS_CONFIG_FILE).inputStream()
    }
}
