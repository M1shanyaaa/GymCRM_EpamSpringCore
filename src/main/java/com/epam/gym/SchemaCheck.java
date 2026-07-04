package com.epam.gym;

import com.epam.gym.config.AppConfig;
import com.epam.gym.config.HibernateConfig;
import com.epam.gym.init.TrainingTypeInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.TimeZone;

public class SchemaCheck {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        var ctx = new AnnotationConfigApplicationContext(AppConfig.class);


        // seed reference data
        ctx.getBean(TrainingTypeInitializer.class).seed();
        System.out.println(">>> Seed done");
        ctx.close();
    }
}