# language: en
@ASG
Feature: Task assignment CRUD (task_assignment)
  As a project manager or chief
  I want to create, consult, update and revoke task assignments
  So that I can manage which workers execute each task

  Background:
    Given an authenticated chief user

  @ASG @ASG-INS-01
  Scenario: Create an assignment with explicit reason (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    When I assign the worker to the task with reason "  Experiencia en cimentación  "
    Then the assignment is created with status 200
    And the response reason is normalized to "Experiencia en cimentación"
    And the assignment is stored in PostgreSQL with the normalized reason

  @ASG @ASG-INS-02
  Scenario: Create an assignment without reason uses default (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    When I assign the worker to the task without a reason
    Then the assignment is created with status 200
    And the response reason is the default "Asignación de Personal"
    And the assignment is stored in PostgreSQL with the default reason

  @ASG @ASG-INS-03
  Scenario: Reject assignment with reason over 500 characters (Unhappy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    When I assign the worker to the task with a reason of 501 characters
    Then the response is 400
    And no assignment is stored in PostgreSQL for that worker

  @ASG @ASG-UPD-01
  Scenario: Update the reason of an existing assignment (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    And the worker is assigned to the task with reason "Motivo original"
    When I update the assignment reason to "Motivo actualizado"
    Then the response is 200
    And the response reason is "Motivo actualizado"
    And the assignment keeps the same task, worker and audit fields

  @ASG @ASG-UPD-02
  Scenario: Update assignment reason to empty uses default (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    And the worker is assigned to the task with reason "Motivo previo"
    When I update the assignment reason to an empty value
    Then the response is 200
    And the response reason is the default "Asignación de Personal"
    And the assignment keeps the same task, worker and audit fields

  @ASG @ASG-UPD-03
  Scenario: Reject reason over 500 characters on update and preserve previous reason (Unhappy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    And the worker is assigned to the task with reason "Motivo vigente"
    When I update the assignment reason to a value of 501 characters
    Then the response is 400
    And the assignment still has reason "Motivo vigente" in PostgreSQL

  @ASG @ASG-DEL-01
  Scenario: Revoke an existing assignment (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    And the worker is assigned to the task
    When I revoke the worker's assignment from the task
    Then the response is 204
    And the assignment no longer exists in PostgreSQL

  @ASG @ASG-SEL-01
  Scenario: Consult an individual assignment by composite key (Happy Path)
    Given a task exists for the authenticated chief's project
    And an active worker exists
    And the worker is assigned to the task with reason "Motivo de consulta"
    When I consult the assignment by task and worker
    Then the response is 200
    And the response shows the worker, task and reason

  @ASG @ASG-SEL-02
  Scenario: List assignments of a task (Happy Path)
    Given a task exists for the authenticated chief's project
    And two active workers exist
    And both workers are assigned to the task
    When I list assignments of the task
    Then the response is 200
    And the list contains both assignments
