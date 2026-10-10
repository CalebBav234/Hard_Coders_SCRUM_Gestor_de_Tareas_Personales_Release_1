package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adaptador de salida de solo lectura sobre {@code app_user}. */
@Component
class UserDirectoryAdapter implements UserDirectory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserAccount> findById(Integer userId) {
        return Optional.ofNullable(entityManager.find(AppUserEntity.class, userId)).map(UserDirectoryAdapter::toDomain);
    }

    @Override
    public Map<Integer, UserAccount> findByIds(Collection<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery("select u from AppUserEntity u where u.userId in :ids", AppUserEntity.class)
                .setParameter("ids", userIds)
                .getResultStream()
                .map(UserDirectoryAdapter::toDomain)
                .collect(Collectors.toMap(UserAccount::userId, Function.identity()));
    }

    @Override
    public List<UserAccount> findActive() {
        return entityManager.createQuery("""
                        select u from AppUserEntity u
                        where u.isActive = true
                        order by lower(u.givenNames), lower(u.familyNames), u.userId
                        """, AppUserEntity.class)
                .getResultStream()
                .map(UserDirectoryAdapter::toDomain)
                .toList();
    }

    static UserAccount toDomain(AppUserEntity entity) {
        return new UserAccount(entity.getUserId(), entity.getEmail(), entity.getGivenNames(), entity.getFamilyNames(),
                Boolean.TRUE.equals(entity.getIsChief()), Boolean.TRUE.equals(entity.getIsActive()));
    }
}
