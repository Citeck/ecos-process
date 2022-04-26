package ru.citeck.ecos.process.config.camunda;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.camunda.bpm.spring.boot.starter.property.DatabaseProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import ru.citeck.ecos.process.domain.datasource.DataSourceFactory;

import java.util.Collections;

/**
 * @author Roman Makarskiy
 */
public class CamundaCustomDataSourceConfiguration extends AbstractCamundaConfiguration
    implements CamundaDatasourceConfiguration {

    @Autowired
    protected DataSourceFactory dataSourceFactory;

    @Autowired
    @Qualifier("camundaTransactionManager")
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected ParseListenerPlugin testPlugin;

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        final DatabaseProperty database = camundaBpmProperties.getDatabase();

        configuration.setTransactionManager(transactionManager);

        configuration.setDataSource(dataSourceFactory.getJdbcDataSource("camunda").getJavaDataSource());

        configuration.setDatabaseType(database.getType());
        configuration.setDatabaseSchemaUpdate(database.getSchemaUpdate());

        if (!StringUtils.isEmpty(database.getTablePrefix())) {
            configuration.setDatabaseTablePrefix(database.getTablePrefix());
        }

        if (!StringUtils.isEmpty(database.getSchemaName())) {
            configuration.setDatabaseSchema(database.getSchemaName());
        }

        configuration.setJdbcBatchProcessing(database.isJdbcBatchProcessing());

        configuration.setProcessEnginePlugins(Collections.singletonList(testPlugin));
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public ParseListenerPlugin getTestPlugin() {
        return testPlugin;
    }

    public void setTestPlugin(ParseListenerPlugin testPlugin) {
        this.testPlugin = testPlugin;
    }
}
