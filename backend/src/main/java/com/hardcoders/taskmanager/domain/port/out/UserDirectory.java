package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.UserAccount;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Puerto de salida de solo lectura sobre {@code app_user}. */
public interface UserDirectory {

    Optional<UserAccount> findById(Integer userId);

    Map<Integer, UserAccount> findByIds(Collection<Integer> userIds);

    /** Usuarios activos que pueden dirigir un proyecto, ordenados por nombre. */
    List<UserAccount> findActive();
}
