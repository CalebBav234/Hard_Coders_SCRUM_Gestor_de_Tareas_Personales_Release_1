package com.hardcoders.taskmanager.configuration;

import com.hardcoders.taskmanager.application.port.in.ManageProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageSubtasksUseCase;
import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryProjectsUseCase;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import com.hardcoders.taskmanager.application.usecase.ProjectCommandService;
import com.hardcoders.taskmanager.application.usecase.ProjectQueryService;
import com.hardcoders.taskmanager.application.usecase.SubtaskCommandService;
import com.hardcoders.taskmanager.application.usecase.TaskCommandService;
import com.hardcoders.taskmanager.application.usecase.TaskQueryService;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.SubtaskRepository;
import com.hardcoders.taskmanager.domain.port.out.TaskRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import com.hardcoders.taskmanager.domain.port.out.WorkspaceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/** Raíz de composición: conecta puertos de entrada, casos de uso y adaptadores de salida. */
@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ManageTasksUseCase manageTasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                          WorkspaceRepository workspaces, CurrentActor actor, Clock clock,
                                          PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageTasksUseCase.class,
                new TaskCommandService(tasks, subtasks, workspaces, actor, clock), transactionManager, false);
    }

    @Bean
    ManageSubtasksUseCase manageSubtasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                                WorkspaceRepository workspaces, CurrentActor actor, Clock clock,
                                                PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(ManageSubtasksUseCase.class,
                new SubtaskCommandService(tasks, subtasks, workspaces, actor, clock), transactionManager, false);
    }

    @Bean
    QueryTasksUseCase queryTasksUseCase(TaskRepository tasks, SubtaskRepository subtasks,
                                        WorkspaceRepository workspaces,
                                        PlatformTransactionManager transactionManager) {
        return TransactionalUseCases.wrap(QueryTasksUseCase.class,
                new TaskQueryService(tasks, subtasks, workspaces), transactionManager, true);
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
}
