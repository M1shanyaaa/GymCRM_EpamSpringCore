Feature: Main service component tests (trainer, login, training)

  Scenario: Register a new trainer returns credentials (positive)
    When a trainer is registered with first name "John" last name "Smith" specialization "FITNESS"
    Then the response status is 200
    And credentials with username and password are returned

  Scenario: Login with valid credentials returns a JWT token (positive)
    Given a trainer is registered with first name "Jane" last name "Doe" specialization "YOGA"
    When login is attempted with the generated credentials
    Then the response status is 200
    And a JWT token is returned

  Scenario: Add training sends a workload event to the producer (positive)
    Given a trainee is registered with first name "Tom" last name "Trainee"
    And a trainer is registered with first name "Coach" last name "One" specialization "FITNESS"
    And the trainee is logged in
    When a training is added for the trainee and trainer on "2024-03-10" with duration 60
    Then the response status is 200
    And a workload event was sent to the message producer

  Scenario: Login with wrong password returns 401 (negative)
    Given a trainer is registered with first name "Bad" last name "Login" specialization "YOGA"
    When login is attempted with a wrong password
    Then the response status is 401

  Scenario: Register trainer with blank first name returns 400 (negative)
    When a trainer is registered with a blank first name
    Then the response status is 400