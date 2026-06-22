package com.epam.gym.config;

import org.springframework.context.annotation.*;

@Configuration
@ComponentScan("com.epam.gym")
@Import(StorageConfig.class)
@PropertySource("classpath:application.properties")
public class AppConfig {

    // Required to resolve ${...} placeholders from @Value
    @Bean
    public static org.springframework.context.support.PropertySourcesPlaceholderConfigurer
    propertySourcesPlaceholderConfigurer() {
        return new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
    }
}