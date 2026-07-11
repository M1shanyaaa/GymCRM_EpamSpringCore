package com.epam.gym;

import com.epam.gym.config.AppConfig;
import jakarta.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;
import java.util.TimeZone;

/**
 * Application entry point.
 * Bootstraps an embedded Tomcat server hosting the Spring MVC DispatcherServlet.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final int PORT = 8080;
    private static final String CONTEXT_PATH = "";
    private static final String SERVLET_NAME = "dispatcher";
    private static final String MAPPING = "/";

    public static void main(String[] args) throws LifecycleException, ServletException {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // 1. Root Spring context (services, DAO, Hibernate, etc.)
        AnnotationConfigWebApplicationContext rootContext =
                new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);

        // 2. Configure embedded Tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector(); // triggers default connector creation

        String docBase = new File(".").getAbsolutePath();
        Context tomcatContext = tomcat.addContext(CONTEXT_PATH, docBase);

        // 3. Register Spring DispatcherServlet
        DispatcherServlet dispatcher = new DispatcherServlet(rootContext);
        Tomcat.addServlet(tomcatContext, SERVLET_NAME, dispatcher).setLoadOnStartup(1);
        tomcatContext.addServletMappingDecoded(MAPPING, SERVLET_NAME);

        // 4. Start
        tomcat.start();
        log.info("===== Gym CRM REST API started on http://localhost:{} =====", PORT);
        tomcat.getServer().await();
    }
}