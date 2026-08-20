Feature: Workload propagation via ActiveMQ into workload service Mongo store

  Scenario: Valid workload ADD event is processed and stored in Mongo (positive)
    When a valid workload ADD event is sent for trainer "john.doe" first name "John" last name "Doe" duration 60 on "2024-03-10"
    Then the workload summary for trainer "john.doe" eventually has 60 minutes for year 2024 month 3

  Scenario: Two ADD events for the same month are accumulated (positive)
    When a valid workload ADD event is sent for trainer "jane.roe" first name "Jane" last name "Roe" duration 30 on "2024-05-10"
    And a valid workload ADD event is sent for trainer "jane.roe" first name "Jane" last name "Roe" duration 45 on "2024-05-20"
    Then the workload summary for trainer "jane.roe" eventually has 75 minutes for year 2024 month 5

  Scenario: Invalid workload event (missing trainer username) goes to DLQ (negative)
    When an invalid workload event with missing trainer username is sent
    Then no workload summary exists for trainer "" after processing