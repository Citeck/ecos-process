package ru.citeck.ecos.process.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.datasource.DataSourceTypeFactory
import ru.citeck.ecos.process.domain.datasource.jdbc.JdbcConnectionProperties
import ru.citeck.ecos.process.domain.datasource.jdbc.JdbcDataSource
import ru.citeck.ecos.process.domain.datasource.jdbc.JdbcDataSourceImpl
import java.lang.Exception

@Component
class EmbeddedPgDataSourceFactory : DataSourceTypeFactory<JdbcConnectionProperties, JdbcDataSource> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun create(id: String, props: JdbcConnectionProperties): JdbcDataSource {

        val dbName = props.url.substringAfterLast("/")

        val pg = EmbeddedPostgres.start()
        val database = pg.postgresDatabase
        database.connection.use { conn ->
            conn.prepareStatement(
                "CREATE DATABASE \"" + dbName + "\";" +
                    "CREATE USER " + props.username + " WITH ENCRYPTED PASSWORD '';" +
                    "GRANT ALL ON DATABASE \"" + dbName + "\" TO " + props.username + ";"
            ).use { stmt ->
                stmt.executeUpdate()
            }
            conn.prepareStatement("SELECT version()").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                    log.info("Setup embedded postgresql database with id $id: " + rs.getString(1))
                }
            }
        }
        val config = HikariConfig()
        config.dataSource = pg.getDatabase(props.username, dbName)
        config.isAutoCommit = false
        config.jdbcUrl = pg.getJdbcUrl(props.username, dbName)

        return JdbcDataSourceImpl(id, HikariDataSource(config))
    }

    override fun getType(): String = JdbcDataSource.TYPE

    private fun <T : AutoCloseable, R> T.use(action: (T) -> R): R {
        var exception: Throwable? = null
        try {
            return action.invoke(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            try {
                this.closeFinally(exception)
            } catch (e: Exception) {
                // do nothing
            }
        }
    }

    private fun AutoCloseable?.closeFinally(cause: Throwable?) = when {
        this == null -> {}
        cause == null -> close()
        else -> {
            try {
                close()
            } catch (closeException: Throwable) {
                cause.addSuppressed(closeException)
            }
        }
    }
}
