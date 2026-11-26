# language: en
@PP-01
Feature: Main process for creating work hierarchy
  As a project chief
  I want to create a complete work hierarchy Project - Stage - Activity - Task - Subtask
  So that I can organize my team's work

  @PP-01
  Scenario: Create complete work hierarchy (Happy Path)
    Given an authenticated chief user with email "chief.ec4@test.gestor.local" and password "ChiefPass123!"
    When I create a project with name "Proyecto EC4" and description "Proyecto de prueba para EC4"
    And I create a stage with name "Etapa EC4" in the project
    And I create an activity with name "Actividad EC4" in the stage
    And I create a task with title "Tarea EC4" in the activity
    And I create a subtask with title "Subtarea EC4" in the task
    Then the hierarchy Project - Stage - Activity - Task - Subtask is persisted
    And the project has 1 stage
    And the stage has 1 activity
    And the activity has 1 task
    And the task has 1 subtask
