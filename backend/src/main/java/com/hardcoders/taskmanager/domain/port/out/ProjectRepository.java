package com.hardcoders.taskmanager.domain.port.out;

import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Puerto de salida para proyectos y su historial de plan. */
public interface ProjectRepository {

    Optional<Project> findById(Integer projectId);

    /** Proyecto bloqueado para modificarlo en la transacción actual. */
    Optional<Project> findForUpdate(Integer projectId);

    /** Todos los proyectos (vista del Jefe), del más reciente al más antiguo. */
    List<Project> findAll();

    /** Proyectos que dirige el usuario (vista del Encargado). */
    List<Project> findManagedBy(Integer managerUserId);

    /** Suma de {@code allocated_budget_bob} de las tareas no canceladas del proyecto. */
    BigDecimal allocatedBudget(Integer projectId);

    /** Revisiones del plan, de la más reciente a la más antigua. */
    List<PlanRevision> planHistory(Integer projectId);

    /** Inserta o actualiza el proyecto y agrega sus revisiones pendientes; devuelve el estado persistido. */
    Project save(Project project);
}
