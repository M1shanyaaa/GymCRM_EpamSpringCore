package com.epam.gym.workload.cucumber.component;

import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.dto.response.TrainerWorkloadResponse;
import com.epam.gym.workload.repo.TrainerWorkloadRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkloadStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final TrainerWorkloadRepository repository;

    public WorkloadStepDefinitions(TestRestTemplate restTemplate,
                                   TrainerWorkloadRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    private ResponseEntity<?> lastResponse;
    private TrainerWorkloadResponse summaryResponse;

    @Before
    public void cleanDatabase() {
        repository.deleteAll();
        lastResponse = null;
        summaryResponse = null;
    }

    @Given("the workload database is empty")
    public void theWorkloadDatabaseIsEmpty() {
        repository.deleteAll();
    }

    // ONE definition — matched by text for both Given and When in the feature
    @When("an ADD workload request is sent for {string} {string} {string} on {string} with duration {int}")
    public void sendAddWorkload(String username, String firstName, String lastName,
                                String date, int duration) {
        WorkloadRequest request = new WorkloadRequest(
                username, firstName, lastName, true,
                LocalDate.parse(date), duration, ActionType.ADD);

        lastResponse = restTemplate.postForEntity("/api/workload", request, Void.class);
    }

    @When("an invalid ADD workload request without username is sent")
    public void sendInvalidWorkload() {
        Map<String, Object> body = new HashMap<>();
        body.put("trainerFirstName", "Bruce");
        body.put("trainerLastName", "Wayne");
        body.put("isActive", true);
        body.put("trainingDate", "2024-01-15");
        body.put("trainingDuration", 60);
        body.put("actionType", "ADD");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        lastResponse = restTemplate.postForEntity("/api/workload", entity, String.class);
    }

    @When("the summary is requested for {string}")
    public void requestSummary(String username) {
        ResponseEntity<TrainerWorkloadResponse> response = restTemplate.exchange(
                "/api/workload/" + username + "/summary",
                HttpMethod.GET, null, TrainerWorkloadResponse.class);
        lastResponse = response;
        if (response.getStatusCode().is2xxSuccessful()) {
            summaryResponse = response.getBody();
        }
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("the summary for {string} year {int} month {int} is {int}")
    public void summaryDurationIs(String username, int year, int month, int expectedDuration) {
        TrainerWorkloadResponse response = restTemplate.getForObject(
                "/api/workload/" + username + "/summary", TrainerWorkloadResponse.class);

        assertThat(response).isNotNull();
        long actual = response.getYears().stream()
                .filter(y -> y.getYear() == year)
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> monthMatches(m.getMonth(), month))
                .mapToLong(m -> m.getTrainingSummaryDuration())
                .sum();

        assertThat(actual).isEqualTo(expectedDuration);
    }

    @Then("the returned username is {string}")
    public void returnedUsernameIs(String username) {
        assertThat(summaryResponse).isNotNull();
        assertThat(summaryResponse.getUsername()).isEqualTo(username);
    }

    private boolean monthMatches(String responseMonth, int monthNumber) {
        if (responseMonth == null) return false;
        return Month.of(monthNumber).name().equalsIgnoreCase(responseMonth);
    }
}