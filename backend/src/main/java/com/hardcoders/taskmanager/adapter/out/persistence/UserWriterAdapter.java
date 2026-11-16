package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.repository.AppUserJpaRepository;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.UserWriter;
import org.springframework.stereotype.Component;

/** Adaptador de escritura para creación de usuarios (EC3-01). */
@Component
class UserWriterAdapter implements UserWriter {

    private final AppUserJpaRepository repository;

    UserWriterAdapter(AppUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return repository.existsByEmail(normalizedEmail);
    }

    @Override
    public UserAccount save(String normalizedEmail, String passwordHash, String givenNames, String familyNames) {
        AppUserEntity entity = new AppUserEntity();
        entity.setEmail(normalizedEmail);
        entity.setPasswordHash(passwordHash);
        entity.setGivenNames(givenNames);
        entity.setFamilyNames(familyNames);
        entity.setIsChief(Boolean.FALSE);
        entity.setIsActive(Boolean.TRUE);
        AppUserEntity saved = repository.save(entity);
        return new UserAccount(saved.getUserId(), saved.getEmail(), saved.getGivenNames(), saved.getFamilyNames(),
                Boolean.TRUE.equals(saved.getIsChief()), Boolean.TRUE.equals(saved.getIsActive()));
    }
}
