package com.epam.gym.config;

import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    /**
     * LOCAL Profile
     * Provisions a lightweight In-Memory H2 database for local development.
     * No Docker container is required for this profile.
     */
    @Bean
    @Profile("local")
    public DataSource localDataSource() {
        log.info("Initializing H2 database for LOCAL environment...");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:gym_local_db;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    /**
     * DEV Profile
     * Connects to the local Docker PostgreSQL instance (gym_dev database).
     * Hardcoded credentials are used here for development convenience.
     */
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        log.info("Initializing PostgreSQL for DEV environment...");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/gym_dev");
        dataSource.setUsername("gym_user");
        dataSource.setPassword("gym_pass");
        return dataSource;
    }

    /**
     * STG Profile (Staging)
     * Connects to the staging database (gym_stg).
     * Uses @Value to allow overriding via environment variables, but provides
     * your local Docker credentials as default fallback values.
     */
    @Bean
    @Profile("stg")
    public DataSource stgDataSource(
            @Value("${STG_DB_URL:jdbc:postgresql://localhost:5432/gym_stg}") String url,
            @Value("${STG_DB_USERNAME:gym_user}") String username,
            @Value("${STG_DB_PASSWORD:gym_pass}") String password) {

        log.info("Initializing PostgreSQL for STG environment...");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * PROD Profile (Production)
     * Maximum security. Retrieves all sensitive credentials strictly from Environment Variables.
     * Fails fast (prevents application startup) if configuration is missing.
     */
    @Bean
    @Profile("prod")
    public DataSource prodDataSource(
            @Value("${PROD_DB_URL}") String url,
            @Value("${PROD_DB_USERNAME}") String username,
            @Value("${PROD_DB_PASSWORD}") String password) {

        log.info("Initializing PostgreSQL for PROD environment...");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * Hibernate Customizer
     * Enables SQL query logging only for LOCAL and DEV profiles.
     * Ensures sensitive data and queries are not logged in STG or PROD.
     */
    @Bean
    @Profile({"local", "dev"})
    public HibernatePropertiesCustomizer showSqlCustomizer() {
        log.info("Enabling SQL logging for non-production environment...");
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.show_sql", "true");
            hibernateProperties.put("hibernate.format_sql", "true");
            hibernateProperties.put("hibernate.hbm2ddl.auto", "update");
        };
    }

    /**
     * Hibernate configuration for STG and PROD (DATABASE PROTECTION).
     * Replaces spring.jpa.hibernate.ddl-auto=validate in properties.
     */
    @Bean
    @Profile({"stg", "prod"})
    public HibernatePropertiesCustomizer prodHibernateCustomizer() {
        log.info("Configuring Hibernate for PROD (show_sql=false, ddl-auto=validate)...");
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.show_sql", "false");
            hibernateProperties.put("hibernate.hbm2ddl.auto", "validate");
        };
    }
}