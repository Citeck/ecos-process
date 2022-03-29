package ru.citeck.ecos.process.config.camunda;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.camunda.bpm.spring.boot.starter.property.DatabaseProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * @author Roman Makarskiy
 */
public class CamundaCustomDataSourceConfiguration extends AbstractCamundaConfiguration
    implements CamundaDatasourceConfiguration {

    @Autowired
    @Qualifier("camundaDataSource")
    protected DataSource dataSource;

    @Autowired
    @Qualifier("camundaTransactionManager")
    protected PlatformTransactionManager transactionManager;

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        final DatabaseProperty database = camundaBpmProperties.getDatabase();

        configuration.setTransactionManager(transactionManager);

        configuration.setDataSource(dataSource);

        configuration.setDatabaseType(database.getType());
        configuration.setDatabaseSchemaUpdate(database.getSchemaUpdate());

        if (!StringUtils.isEmpty(database.getTablePrefix())) {
            configuration.setDatabaseTablePrefix(database.getTablePrefix());
        }

        if (!StringUtils.isEmpty(database.getSchemaName())) {
            configuration.setDatabaseSchema(database.getSchemaName());
        }

        configuration.setJdbcBatchProcessing(database.isJdbcBatchProcessing());
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
