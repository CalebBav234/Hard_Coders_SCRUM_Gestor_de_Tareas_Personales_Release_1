package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Actor provisional mientras no exista autenticación (DEC-003, JWT): todas las operaciones se
 * atribuyen a un usuario local que se crea la primera vez que hace falta. Su hash de contraseña es
 * inválido, por lo que nunca podrá iniciar sesión. Reemplazar por el adaptador de seguridad.
 */
@Component
class LocalUserActorAdapter implements CurrentActor {

    @PersistenceContext
    private EntityManager entityManager;

    private final String email;

    LocalUserActorAdapter(@Value("${app.local-user.email:usuario.local@gestor-tareas.local}") String email) {
        this.email = email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public Integer userId() {
        // Requiere una transacción de escritura: la abre el decorador de los casos de uso.
        entityManager.createNativeQuery("""
                        INSERT INTO task_manager.app_user
                            (email, password_hash, given_names, family_names, is_chief, is_active)
                        VALUES (:email, '!', 'Usuario', 'Local', true, true)
                        ON CONFLICT (email) DO NOTHING
                        """)
                .setParameter("email", email)
                .executeUpdate();
        Number id = (Number) entityManager.createNativeQuery(
                        "SELECT user_id FROM task_manager.app_user WHERE email = :email")
                .setParameter("email", email)
                .getSingleResult();
        return id.intValue();
    }
}
