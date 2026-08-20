Feature: Trainer workload summary component tests

  Background:
    Given the workload database is empty

  Scenario: Add training creates a new monthly summary (positive)
    When an ADD workload request is sent for "Bruce.Wayne" "Bruce" "Wayne" on "2024-01-15" with duration 60
    Then the response status is 200
    And the summary for "Bruce.Wayne" year 2024 month 1 is 60

  Scenario: Adding to an existing month accumulates duration (positive)
    Given an ADD workload request is sent for "Bruce.Wayne" "Bruce" "Wayne" on "2024-01-15" with duration 60
    When an ADD workload request is sent for "Bruce.Wayne" "Bruce" "Wayne" on "2024-01-20" with duration 30
    Then the response status is 200
    And the summary for "Bruce.Wayne" year 2024 month 1 is 90

  Scenario: Get summary for existing trainer returns data (positive)
    Given an ADD workload request is sent for "Clark.Kent" "Clark" "Kent" on "2023-05-10" with duration 45
    When the summary is requested for "Clark.Kent"
    Then the response status is 200
    And the returned username is "Clark.Kent"

  Scenario: Get summary for non-existing trainer returns 404 (negative)
    When the summary is requested for "Ghost.User"
    Then the response status is 404

  Scenario: Invalid workload request without username returns 400 (negative)
    When an invalid ADD workload request without username is sent
    Then the response status is 400