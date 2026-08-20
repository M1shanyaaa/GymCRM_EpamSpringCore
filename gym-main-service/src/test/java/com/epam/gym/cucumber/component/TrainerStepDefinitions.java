package com.epam.gym.cucumber.component;

import com.epam.gym.dto.request.AddTrainingRequest;
import com.epam.gym.dto.request.LoginRequest;
import com.epam.gym.dto.request.TraineeRegistrationRequest;
import com.epam.gym.dto.request.TrainerRegistrationRequest;
import com.epam.gym.dto.response.CredentialsResponse;
import com.epam.gym.dto.response.JwtResponse;
import com.epam.gym.messaging.WorkloadMessageProducer;
import com.epam.gym.model.TrainingTypeName;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class TrainerStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final WorkloadMessageProducer workloadMessageProducer;
    private final JdbcTemplate jdbcTemplate;

    public TrainerStepDefinitions(TestRestTemplate restTemplate,
                                  WorkloadMessageProducer workloadMessageProducer,
                                  JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.workloadMessageProducer = workloadMessageProducer;
        this.jdbcTemplate = jdbcTemplate;
    }

    private ResponseEntity<?> lastResponse;
    private CredentialsResponse traineeCredentials;
    private CredentialsResponse trainerCredentials;
    private CredentialsResponse lastRegisteredCredentials;
    private String jwtToken;

    @Before(order = 0)
    public void seedTrainingTypes() {
        for (TrainingTypeName type : TrainingTypeName.values()) {
            jdbcTemplate.update(
                    "MERGE INTO training_types (training_type_name) KEY(training_type_name) VALUES (?)",
                    type.name()
            );
        }
    }

    @Before(order = 1)
    public void reset() {
        Mockito.reset(workloadMessageProducer);
        lastResponse = null;
        traineeCredentials = null;
        trainerCredentials = null;
        lastRegisteredCredentials = null;
        jwtToken = null;
    }

    @When("a trainer is registered with first name {string} last name {string} specialization {string}")
    public void registerTrainer(String firstName, String lastName, String specialization) {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest(
                firstName, lastName, TrainingTypeName.valueOf(specialization));

        ResponseEntity<CredentialsResponse> response =
                restTemplate.postForEntity("/api/trainers", request, CredentialsResponse.class);
        lastResponse = response;
        if (response.getStatusCode().is2xxSuccessful()) {
            trainerCredentials = response.getBody();
            lastRegisteredCredentials = response.getBody();
        }
    }

    @When("a trainer is registered with a blank first name")
    public void registerTrainerBlankName() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "");                          // blank -> @NotBlank fails -> 400
        body.put("lastName", "Smith");
        body.put("specialization", TrainingTypeName.FITNESS.name());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        lastResponse = restTemplate.postForEntity(
                "/api/trainers", new HttpEntity<>(body, headers), String.class);
    }

    // ---------- Register trainee ----------
    @Given("a trainee is registered with first name {string} last name {string}")
    public void registerTrainee(String firstName, String lastName) {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest(
                firstName, lastName, null, null); // dateOfBirth, address optional

        ResponseEntity<CredentialsResponse> response =
                restTemplate.postForEntity("/api/trainees", request, CredentialsResponse.class);
        lastResponse = response;
        if (response.getStatusCode().is2xxSuccessful()) {
            traineeCredentials = response.getBody();
        }
    }

    // ---------- Login ----------
    @When("login is attempted with the generated credentials")
    public void loginWithGeneratedCredentials() {
        LoginRequest request = new LoginRequest(
                lastRegisteredCredentials.username(),
                lastRegisteredCredentials.password());
        lastResponse = restTemplate.postForEntity("/api/login", request, JwtResponse.class);
    }

    @Given("the trainee is logged in")
    public void traineeLoggedIn() {
        LoginRequest request = new LoginRequest(
                traineeCredentials.username(), traineeCredentials.password());
        ResponseEntity<JwtResponse> response =
                restTemplate.postForEntity("/api/login", request, JwtResponse.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        jwtToken = response.getBody().token();
    }

    @When("login is attempted with a wrong password")
    public void loginWrongPassword() {
        LoginRequest request = new LoginRequest(
                lastRegisteredCredentials.username(), "definitely-wrong-password");
        lastResponse = restTemplate.postForEntity("/api/login", request, String.class);
    }

    // ---------- Add training ----------
    @When("a training is added for the trainee and trainer on {string} with duration {int}")
    public void addTraining(String date, int duration) {
        AddTrainingRequest request = new AddTrainingRequest(
                traineeCredentials.username(),
                trainerCredentials.username(),
                "Morning session",
                LocalDate.parse(date),
                duration);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken); // real JWT obtained from login

        lastResponse = restTemplate.exchange(
                "/api/trainings", HttpMethod.POST,
                new HttpEntity<>(request, headers), Void.class);
    }

    // ---------- Then ----------
    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @Then("credentials with username and password are returned")
    public void credentialsReturned() {
        assertThat(lastRegisteredCredentials).isNotNull();
        assertThat(lastRegisteredCredentials.username()).isNotBlank();
        assertThat(lastRegisteredCredentials.password()).isNotBlank();
    }

    @Then("a JWT token is returned")
    public void jwtReturned() {
        @SuppressWarnings("unchecked")
        ResponseEntity<JwtResponse> resp = (ResponseEntity<JwtResponse>) lastResponse;
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isNotBlank();
    }

    @Then("a workload event was sent to the message producer")
    public void workloadEventSent() {
        verify(workloadMessageProducer).sendWorkload(any());
    }
}