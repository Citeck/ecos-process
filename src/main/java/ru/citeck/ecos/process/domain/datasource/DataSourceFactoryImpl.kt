package ru.citeck.ecos.process.domain.datasource

import org.apache.commons.beanutils.PropertyUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.process.domain.datasource.exception.DataSourceNotFound
import ru.citeck.ecos.process.domain.datasource.exception.UnsupportedDataSourceType
import ru.citeck.ecos.process.domain.datasource.jdbc.JdbcDataSource
import java.util.concurrent.ConcurrentHashMap

@Component
class DataSourceFactoryImpl : DataSourceFactory {

    @Autowired
    private lateinit var env: Environment
    private lateinit var factories: Map<String, DataSourceTypeFactory<Any, EcosDataSource>>

    private val dataSources = ConcurrentHashMap<String, EcosDataSource>()

    override fun <T : EcosDataSource> getDataSource(id: String, type: String): T {
        if (type != JdbcDataSource.TYPE) {
            throw UnsupportedDataSourceType(type)
        }
        val result = dataSources.computeIfAbsent(id) { createDataSource(id, type) }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun createDataSource(id: String, type: String): EcosDataSource {

        val factory = factories[type] ?: throw UnsupportedDataSourceType(type)
        val propsType = ReflectUtils.getGenericArgs(factory::class.java, DataSourceTypeFactory::class.java)[0]

        val propPrefix = "ecos.webapp.datasource.$type.connections.$id."

        val propsValues = mutableMapOf<String, String?>()
        val descriptors = PropertyUtils.getPropertyDescriptors(propsType)
        descriptors.forEach {
            if (it.writeMethod != null) {
                val key: String = propPrefix + it.name
                if (env.containsProperty(key)) {
                    propsValues[it.name] = env.getProperty(key)
                }
            }
        }
        if (propsValues.isEmpty()) {
            throw DataSourceNotFound(id, type)
        }

        val props = propsType.newInstance()
        Json.mapper.applyData(props, propsValues)

        return factory.create(id, props)
    }

    @Autowired
    fun register(factories: List<DataSourceTypeFactory<*, *>>) {
        this.factories = factories.associate {
            @Suppress("UNCHECKED_CAST")
            it.getType() to it as DataSourceTypeFactory<Any, EcosDataSource>
        }
    }
}
