package com.hardcoders.taskmanager.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuario de la aplicación. Tabla {@code task_manager.app_user}. */
@Entity
@Table(name = "app_user", schema = "task_manager")
public class AppUserEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "given_names", nullable = false, length = 100)
    private String givenNames;

    @Column(name = "family_names", nullable = false, length = 100)
    private String familyNames;

    @Column(name = "is_chief", nullable = false)
    private Boolean isChief = Boolean.FALSE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGivenNames() {
        return givenNames;
    }

    public void setGivenNames(String givenNames) {
        this.givenNames = givenNames;
    }

    public String getFamilyNames() {
        return familyNames;
    }

    public void setFamilyNames(String familyNames) {
        this.familyNames = familyNames;
    }

    public Boolean getIsChief() {
        return isChief;
    }

    public void setIsChief(Boolean isChief) {
        this.isChief = isChief;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
