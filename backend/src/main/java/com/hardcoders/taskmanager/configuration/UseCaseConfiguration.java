package com.hardcoders.taskmanager.configuration;

import com.hardcoders.taskmanager.application.port.in.ManageAssignmentsUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryHierarchyUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProgressUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.usecase.AssignmentCommandService;
import com.hardcoders.taskmanager.application.usecase.HierarchyCommandService;
import com.hardcoders.taskmanager.application.usecase.HierarchyQueryService;
import com.hardcoders.taskmanager.application.usecase.ProgressQueryService;
import com.hardcoders.taskmanager.application.usecase.ProjectCommandService;
import com.hardcoders.taskmanager.application.usecase.ProjectQueryService;
import com.hardcoders.taskmanager.application.usecase.SubtaskCommandService;
import com.hardcoders.taskmanager.application.usecase.TaskCommandService;
import com.hardcoders.taskmanager.application.usecase.TaskQueryService;
import com.hardcoders.taskmanager.domain.port.out.AssignmentRepository;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.HierarchyRepository;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/**
 * Raíz de composición: conecta puertos de entrada, casos de uso y adaptadores de salida.
 * Cada caso de uso se envuelve en una transacción ({@link TransactionalUseCases}) porque los triggers
 * diferidos de V004 validan los historiales obligatorios al confirmar.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ManageProjectsUseCase manageProjectsUseCase(ProjectRepository projects, UserDirectory users, CurrentActor actor,
                                                Clock clock, PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageProjectsUseCase.class,
                new ProjectCommandService(projects, users, actor, clock), transactionManager, false);
    }

    @Bean
    QueryProjectsUseCase queryProjectsUseCase(ProjectRepository projects, UserDirectory users, CurrentActor actor,
                                              PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(QueryProjectsUseCase.class,
                new ProjectQueryService(projects, users, actor), transactionManager, true);
    }

    @Bean
    ManageHierarchyUseCase manageHierarchyUseCase(ProjectRepository projects, HierarchyRepository hierarchy,
                                                  CurrentActor actor, Clock clock,
                                                  PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageHierarchyUseCase.class,
                new HierarchyCommandService(projects, hierarchy, actor, clock), transactionManager, false);
    }

    @Bean
    QueryHierarchyUseCase queryHierarchyUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                                HierarchyRepository hierarchy, ProjectRepository projects,
                                                AssignmentRepository assignments, CurrentActor actor,
                                                PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(QueryHierarchyUseCase.class,
                new HierarchyQueryService(tasks, subtasks, hierarchy, projects, assignments, actor),
                transactionManager, true);
    }

    @Bean
    ManageAssignmentsUseCase manageAssignmentsUseCase(TaskRepository tasks, ProjectRepository projects,
                                                      AssignmentRepository assignments, UserDirectory users,
                                                      CurrentActor actor, Clock clock,
                                                      PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageAssignmentsUseCase.class,
                new AssignmentCommandService(tasks, projects, assignments, users, actor, clock),
                transactionManager, false);
    }

    @Bean
    ManageTasksUseCase manageTasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                          HierarchyRepository hierarchy, ProjectRepository projects,
                                          AssignmentRepository assignments, CurrentActor actor, Clock clock,
                                          PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageTasksUseCase.class,
                new TaskCommandService(tasks, subtasks, hierarchy, projects, assignments, actor, clock),
                transactionManager, false);
    }

    @Bean
    ManageSubtasksUseCase manageSubtasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                                HierarchyRepository hierarchy, ProjectRepository projects,
                                                AssignmentRepository assignments, CurrentActor actor, Clock clock,
                                                PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageSubtasksUseCase.class,
                new SubtaskCommandService(tasks, subtasks, hierarchy, projects, assignments, actor, clock),
                transactionManager, false);
    }

    @Bean
    QueryTasksUseCase queryTasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                        HierarchyRepository hierarchy, ProjectRepository projects,
                                        AssignmentRepository assignments, CurrentActor actor,
                                        PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(QueryTasksUseCase.class,
                new TaskQueryService(tasks, subtasks, hierarchy, projects, assignments, actor),
                transactionManager, true);
    }

    @Bean
    QueryProgressUseCase queryProgressUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                              HierarchyRepository hierarchy, ProjectRepository projects,
                                              CurrentActor actor,
                                              PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(QueryProgressUseCase.class,
                new ProgressQueryService(tasks, subtasks, hierarchy, projects, actor), transactionManager, true);
    }
}
