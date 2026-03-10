package com.flora.ai.ecommerce.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DuckDB 内存数据库配置
 * <p>
 * 注意：DuckDB 内存模式下，每个 Connection 是一个独立的数据库实例。
 * 如需在多个组件间共享数据，必须共享同一个 Connection 实例。
 */
@Slf4j
@Configuration
public class DuckDbConfig {

    /**
     * DuckDB 数据源配置属性
     */
    @Bean
    @ConfigurationProperties("spring.datasource-duckdb")
    public DataSourceProperties duckdbDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * DuckDB 数据源（HikariCP 连接池）
     * <p>
     * 注意：由于是内存模式，连接池大小应设置为 1，否则多个连接会是不同的数据库实例
     */
    @Bean(name = "duckdbDataSource", destroyMethod = "close")
    public HikariDataSource duckdbDataSource() {
        HikariDataSource dataSource = duckdbDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        // 内存模式下，连接池必须设置为 1，确保只有一个共享连接
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);

        log.info("DuckDB 内存数据库数据源初始化完成");
        return dataSource;
    }

    /**
     * 共享的 DuckDB 连接
     * <p>
     * 所有组件通过此 Bean 共享同一个内存数据库实例
     */
    @Bean(name = "sharedDuckdbConnection", destroyMethod = "close")
    public Connection sharedDuckdbConnection(
            @Qualifier("duckdbDataSource") DataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection();
        log.info("DuckDB 共享连接创建成功");
        return connection;
    }

    /**
     * DuckDB JdbcTemplate（基于共享连接）
     */
    @Bean(name = "duckdbJdbcTemplate")
    public JdbcTemplate duckdbJdbcTemplate(
            @Qualifier("sharedDuckdbConnection") Connection connection) {
        // 使用 SingleConnectionDataSource 包装共享连接
        org.springframework.jdbc.datasource.SingleConnectionDataSource singleDs =
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true);
        return new JdbcTemplate(singleDs);
    }

    /**
     * DuckDB NamedParameterJdbcTemplate（基于共享连接）
     */
    @Bean(name = "duckdbNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate duckdbNamedParameterJdbcTemplate(
            @Qualifier("duckdbJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * 初始化 DuckDB 扩展（可选）
     * <p>
     * 可以在这里预加载需要的 DuckDB 扩展
     */
    @Bean
    public DuckDbInitializer duckDbInitializer(
            @Qualifier("sharedDuckdbConnection") Connection connection) {
        return new DuckDbInitializer(connection);
    }

    /**
     * DuckDB 初始化器
     */
    @Slf4j
    public static class DuckDbInitializer {

        private final Connection connection;

        public DuckDbInitializer(Connection connection) {
            this.connection = connection;
            initialize();
        }

        private void initialize() {
            try (Statement stmt = connection.createStatement()) {
                // 安装并加载常用扩展
                // stmt.execute("INSTALL httpfs");
                // stmt.execute("LOAD httpfs");

                // 设置内存限制（可选）
                // stmt.execute("SET memory_limit = '1GB'");

                log.info("DuckDB 初始化完成");
            } catch (SQLException e) {
                log.error("DuckDB 初始化失败", e);
                throw new RuntimeException("DuckDB 初始化失败", e);
            }
        }
    }
}