package com.epam.gym.workload.cucumber.integration;

import com.epam.gym.workload.document.MonthSummary;
import com.epam.gym.workload.document.TrainerWorkloadDocument;
import com.epam.gym.workload.dto.ActionType;
import com.epam.gym.workload.dto.WorkloadRequest;
import com.epam.gym.workload.repo.TrainerWorkloadRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import java.security.Key;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WorkloadPropagationSteps {

    private final JmsTemplate jmsTemplate;
    private final TrainerWorkloadRepository repository;

    @Value("${gym.messaging.workload-queue}")
    private String workloadQueue;

    @Value("${gym.messaging.workload-dlq}")
    private String deadLetterQueue;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public WorkloadPropagationSteps(JmsTemplate jmsTemplate,
                                    TrainerWorkloadRepository repository) {
        this.jmsTemplate = jmsTemplate;
        this.repository = repository;
    }

    @Before
    public void cleanUp() {
        repository.deleteAll();
        drainQueue(deadLetterQueue);
        drainQueue(workloadQueue);
    }

    // ---------- Send valid event ----------
    @When("a valid workload ADD event is sent for trainer {string} first name {string} last name {string} duration {int} on {string}")
    public void sendValidEvent(String username, String firstName, String lastName,
                               int duration, String date) {
        WorkloadRequest request = new WorkloadRequest(
                username, firstName, lastName, true,
                LocalDate.parse(date), duration, ActionType.ADD);

        String token = generateToken(username);

        jmsTemplate.convertAndSend(workloadQueue, request, message -> {
            message.setStringProperty("Authorization", "Bearer " + token);
            message.setStringProperty("transactionId", "it-" + System.currentTimeMillis());
            return message;
        });
    }

    // ---------- Send invalid event (missing username) ----------
    @When("an invalid workload event with missing trainer username is sent")
    public void sendInvalidEvent() {
        WorkloadRequest request = new WorkloadRequest(
                null, "No", "Name", true,
                LocalDate.parse("2024-03-10"), 60, ActionType.ADD);

        String token = generateToken("someone");

        jmsTemplate.convertAndSend(workloadQueue, request, message -> {
            message.setStringProperty("Authorization", "Bearer " + token);
            message.setStringProperty("transactionId", "it-invalid-" + System.currentTimeMillis());
            return message;
        });
    }

    // ---------- Assert summary present ----------
    @Then("the workload summary for trainer {string} eventually has {int} minutes for year {int} month {int}")
    public void summaryHasMinutes(String username, int minutes, int year, int month) {
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Optional<TrainerWorkloadDocument> docOpt =
                            repository.findByTrainerUsername(username);
                    assertThat(docOpt).isPresent();

                    long actual = extractDuration(docOpt.get(), year, month);
                    assertThat(actual).isEqualTo(minutes);
                });
    }

    // ---------- Assert message routed to DLQ (NEW, stronger check) ----------
    @Then("the invalid message eventually lands in the dead-letter queue")
    public void invalidMessageInDlq() {
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Object dlqMessage = jmsTemplate.receiveAndConvert(deadLetterQueue);
                    assertThat(dlqMessage)
                            .as("Expected an invalid message in the dead-letter queue")
                            .isNotNull();
                });
    }

    // ---------- Assert nothing persisted ----------
    @Then("no workload summary exists for trainer {string} after processing")
    public void noSummaryExists(String username) {
        await().during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(6))
                .untilAsserted(() ->
                        assertThat(repository.findByTrainerUsername(username)).isEmpty());
    }

    // ---------- helpers ----------
    private long extractDuration(TrainerWorkloadDocument doc, int year, int month) {
        return doc.getYears().stream()
                .filter(y -> y.getYear() == year)
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> m.getMonth() == month)
                .mapToLong(MonthSummary::getSummaryDuration)
                .sum();
    }

    private void drainQueue(String queue) {
        // Remove any leftover messages so scenarios don't interfere
        jmsTemplate.setReceiveTimeout(200);
        while (jmsTemplate.receive(queue) != null) {
        }
    }

    private String generateToken(String subject) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}