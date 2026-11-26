package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.repository.AppUserJpaRepository;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.UserWriter;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        return toDomain(saved);
    }

    @Override
    public Optional<UserAccount> update(Integer userId, String email, String givenNames, String familyNames) {
        return repository.findById(userId).map(entity -> {
            entity.setEmail(email);
            entity.setGivenNames(givenNames);
            entity.setFamilyNames(familyNames);
            return toDomain(repository.save(entity));
        });
    }

    @Override
    public Optional<UserAccount> deactivate(Integer userId) {
        return repository.findById(userId).map(entity -> {
            entity.setIsActive(Boolean.FALSE);
            return toDomain(repository.save(entity));
        });
    }

    static UserAccount toDomain(AppUserEntity entity) {
        return new UserAccount(entity.getUserId(), entity.getEmail(), entity.getGivenNames(), entity.getFamilyNames(),
                Boolean.TRUE.equals(entity.getIsChief()), Boolean.TRUE.equals(entity.getIsActive()));
    }
}
