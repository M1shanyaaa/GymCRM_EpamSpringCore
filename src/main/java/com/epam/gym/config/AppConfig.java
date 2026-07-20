package com.epam.gym.config;

import org.springframework.context.annotation.*;

@Configuration
@ComponentScan("com.epam.gym")
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Bean
    public static org.springframework.context.support.PropertySourcesPlaceholderConfigurer
    propertySourcesPlaceholderConfigurer() {
        return new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
    }
}