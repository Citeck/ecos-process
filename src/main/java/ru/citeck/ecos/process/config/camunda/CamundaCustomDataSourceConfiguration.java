package ru.citeck.ecos.process.config.camunda;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.camunda.bpm.spring.boot.starter.property.DatabaseProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource;
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager;

/**
 * @author Roman Makarskiy
 */
public class CamundaCustomDataSourceConfiguration extends AbstractCamundaConfiguration
    implements CamundaDatasourceConfiguration {

    @Autowired
    protected EcosDataSourceManager dataSourceManager;

    @Autowired
    @Qualifier("camundaTransactionManager")
    protected PlatformTransactionManager transactionManager;

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        final DatabaseProperty database = camundaBpmProperties.getDatabase();

        configuration.setTransactionManager(transactionManager);

        JdbcDataSource dataSource = dataSourceManager.getDataSource("camunda", JdbcDataSource.class);
        configuration.setDataSource(dataSource.getJavaDataSource());

        configuration.setDatabaseType(database.getType());
        configuration.setDatabaseSchemaUpdate(database.getSchemaUpdate());

        if (!StringUtils.isEmpty(database.getTablePrefix())) {
            configuration.setDatabaseTablePrefix(database.getTablePrefix());
        }

        if (!StringUtils.isEmpty(database.getSchemaName())) {
            configuration.setDatabaseSchema(database.getSchemaName());
        }

        configuration.setJdbcBatchProcessing(database.isJdbcBatchProcessing());

        // TODO: remove
        //configuration.setProcessEnginePlugins(Collections.singletonList(testPlugin));
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
