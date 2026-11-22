/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.zaxxer.hikari.HikariDataSource
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.context.properties.EnableConfigurationProperties
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.core.io.Resource
 *  org.springframework.core.io.ResourceLoader
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.jdbc.datasource.DataSourceTransactionManager
 *  org.springframework.transaction.PlatformTransactionManager
 */
package com.example.waterfactoryceshi.config;

import com.example.waterfactoryceshi.config.MonitorDbProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(value={MonitorDbProperties.class})
public class MonitorDbConfig {
    private static final Logger log = LoggerFactory.getLogger(MonitorDbConfig.class);

    @Bean(name={"monitorDataSource"})
    @ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
    public DataSource monitorDataSource(MonitorDbProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setMinimumIdle(1);
        dataSource.setPoolName("monitor-db-pool");
        dataSource.setConnectionTimeout(10000L);
        dataSource.setValidationTimeout(5000L);
        dataSource.setIdleTimeout(300000L);
        dataSource.setMaxLifetime(1800000L);
        return dataSource;
    }

    @Bean(name={"monitorJdbcTemplate"})
    @ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
    public JdbcTemplate monitorJdbcTemplate(DataSource monitorDataSource) {
        return new JdbcTemplate(monitorDataSource);
    }

    @Bean(name={"monitorTransactionManager"})
    @ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
    public PlatformTransactionManager monitorTransactionManager(@Qualifier(value="monitorDataSource") DataSource monitorDataSource) {
        return new DataSourceTransactionManager(monitorDataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix="monitor.db", name={"enabled", "init-schema"}, havingValue="true")
    public InitializingBean monitorSchemaInitializer(final DataSource monitorDataSource, final MonitorDbProperties properties, final ResourceLoader resourceLoader) {
        return new InitializingBean(){

            public void afterPropertiesSet() throws Exception {
                Resource resource = resourceLoader.getResource(properties.getSchemaLocation());
                if (!resource.exists()) {
                    throw new IllegalStateException("Monitor schema SQL not found: " + properties.getSchemaLocation());
                }
                try (Connection connection = monitorDataSource.getConnection();){
                    MonitorDbConfig.this.executeMysqlScript(connection, resource);
                    log.info("Monitor schema initialized from {}", (Object)properties.getSchemaLocation());
                }
            }
        };
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void executeMysqlScript(Connection connection, Resource resource) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        String delimiter = ";";
        StringBuilder statement = new StringBuilder();
        Statement jdbcStatement = connection.createStatement();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                if (trimmed.toUpperCase().startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring("DELIMITER ".length()).trim();
                    continue;
                }
                statement.append(line).append('\n');
                String sql = statement.toString().trim();
                if (!sql.endsWith(delimiter)) continue;
                if (!(sql = sql.substring(0, sql.length() - delimiter.length()).trim()).isEmpty()) {
                    jdbcStatement.execute(sql);
                }
                statement.setLength(0);
            }
            String sql = statement.toString().trim();
            if (!sql.isEmpty()) {
                jdbcStatement.execute(sql);
            }
        }
        finally {
            jdbcStatement.close();
            reader.close();
        }
    }
}
