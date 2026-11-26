# language: en
@USR
Feature: User management CRUD (app_user)
  As a system chief
  I want to create, consult, update and deactivate users
  So that I can administer access to the task manager

  Background:
    Given an authenticated chief user

  @USR @USR-INS-01
  Scenario: Create a valid user (Happy Path)
    When I create a valid user with given names "Ana" and family names "Pérez"
    Then the user is created with status 201
    And the response has no password_hash, password or token
    And the user is stored in PostgreSQL as active and non-chief

  @USR @USR-INS-02
  Scenario: Create a second valid and distinct user (Happy Path)
    When I create a valid user with given names "Luis" and family names "Gómez"
    Then the user is created with status 201
    And the response has no password_hash, password or token
    And the user is stored in PostgreSQL as active and non-chief

  @USR @USR-INS-03
  Scenario: Reject creation with invalid email (Unhappy Path)
    When I create a user with email "invalido" and valid names and password
    Then the response is 400
    And no user is stored in PostgreSQL

  @USR @USR-UPD-01
  Scenario: Update names and family names (Happy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    When I update the user's names to "María" and "López"
    Then the response is 200
    And the user's names are "María" and "López" in PostgreSQL

  @USR @USR-UPD-02
  Scenario: Update email to a valid unique email (Happy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    When I update the user's email to "MARIA.NUEVA@GESTOR.LOCAL"
    Then the response is 200
    And the user's email is stored normalized in PostgreSQL

  @USR @USR-UPD-03
  Scenario: Reject update with invalid email and preserve previous data (Unhappy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    When I update the user's email to "invalido"
    Then the response is 400
    And the user's previous names are preserved in PostgreSQL

  @USR @USR-DEL-01
  Scenario: Deactivate a non-referenced test user (Happy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    When I deactivate the user
    Then the response is 200
    And the user is inactive in PostgreSQL
    And the user row is preserved and not physically deleted

  @USR @USR-SEL-01
  Scenario: Consult an existing individual user without secrets (Happy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    When I consult the user by id
    Then the response is 200
    And the response shows given names, family names, email and status
    And the response has no password_hash, password or token

  @USR @USR-SEL-02
  Scenario: List users representing status without secrets (Happy Path)
    Given a user exists with given names "Ana" and family names "Pérez"
    And a second user exists with given names "Luis" and family names "Gómez"
    When I list users
    Then the response is 200
    And the list contains the created users
    And no entry exposes password_hash, password or token
