package com.epam.gym.workload.cucumber.integration;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
        value = "com.epam.gym.workload.cucumber.integration")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-integration-report.html")
public class CucumberIntegrationRunnerTest {
}