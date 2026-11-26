package com.hardcoders.taskmanager.configuration;

import com.hardcoders.taskmanager.application.port.in.ManageTasksUseCase;
import com.hardcoders.taskmanager.application.result.TaskResult;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas del decorador transaccional: passthrough de Object, ejecución en transacción y propagación de excepciones. */
class TransactionalUseCasesTest {

    @SuppressWarnings("unchecked")
    private static <T> T wrap(Class<T> port, Object target, PlatformTransactionManager tm, boolean readOnly) {
        return (T) com.hardcoders.taskmanager.configuration.TransactionalUseCases.wrap(port, (T) target, tm, readOnly);
    }

    @Test
    void objectMethodsAreInvokedDirectlyOutsideTransaction() {
        String target = "hello";
        ManageTasksUseCase port = wrap(ManageTasksUseCase.class, target,
                mock(PlatformTransactionManager.class), false);
        assertThat(port.hashCode()).isEqualTo(target.hashCode());
        assertThat(port.toString()).isEqualTo("hello");
    }

    @Test
    void nonObjectMethodsExecuteInsideTransactionTemplate() {
        class Impl implements ManageTasksUseCase {
            @Override public TaskResult create(com.hardcoders.taskmanager.application.command.CreateTaskCommand c) { return null; }
            @Override public TaskResult update(com.hardcoders.taskmanager.application.command.UpdateTaskCommand c) { return null; }
            @Override public TaskResult changeActivity(com.hardcoders.taskmanager.application.command.ChangeActivityCommand c) { return null; }
            @Override public TaskResult updatePlan(com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand c) { return null; }
            @Override public TaskResult start(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult pause(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult complete(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult reopen(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public void cancel(com.hardcoders.taskmanager.application.command.VersionedCommand c) {}
            @Override public void reorderTasks(Integer activityId, List<Integer> taskIds) {}
        }
        Impl impl = new Impl();
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> cb = (TransactionCallback<Object>) invocation.getArgument(0);
                    return cb.doInTransaction(mock(TransactionStatus.class));
                });
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        ManageTasksUseCase port = wrap(ManageTasksUseCase.class, impl, tm, false);
        assertThat(port.toString()).isNotNull();
    }

    @Test
    void runtimeExceptionPropagatesThroughWrap() {
        class Impl implements ManageTasksUseCase {
            @Override public TaskResult create(com.hardcoders.taskmanager.application.command.CreateTaskCommand c) { return null; }
            @Override public TaskResult update(com.hardcoders.taskmanager.application.command.UpdateTaskCommand c) { return null; }
            @Override public TaskResult changeActivity(com.hardcoders.taskmanager.application.command.ChangeActivityCommand c) { return null; }
            @Override public TaskResult updatePlan(com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand c) { return null; }
            @Override public TaskResult start(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { throw new RuntimeException("boom"); }
            @Override public TaskResult pause(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult complete(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult reopen(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public void cancel(com.hardcoders.taskmanager.application.command.VersionedCommand c) {}
            @Override public void reorderTasks(Integer activityId, List<Integer> taskIds) {}
        }
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> cb = (TransactionCallback<Object>) invocation.getArgument(0);
                    return cb.doInTransaction(mock(TransactionStatus.class));
                });
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        ManageTasksUseCase port = wrap(ManageTasksUseCase.class, new Impl(), tm, false);
        assertThatThrownBy(() -> port.start(null))
                .isInstanceOf(RuntimeException.class)
                .message().contains("boom");
    }

    @Test
    void errorPropagatesThroughWrap() {
        class Impl implements ManageTasksUseCase {
            @Override public TaskResult create(com.hardcoders.taskmanager.application.command.CreateTaskCommand c) { return null; }
            @Override public TaskResult update(com.hardcoders.taskmanager.application.command.UpdateTaskCommand c) { return null; }
            @Override public TaskResult changeActivity(com.hardcoders.taskmanager.application.command.ChangeActivityCommand c) { return null; }
            @Override public TaskResult updatePlan(com.hardcoders.taskmanager.application.command.TaskCommands.UpdateTaskPlanCommand c) { return null; }
            @Override public TaskResult start(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { throw new NoSuchMethodError("nope"); }
            @Override public TaskResult pause(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult complete(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public TaskResult reopen(com.hardcoders.taskmanager.application.command.TaskCommands.TransitionCommand c) { return null; }
            @Override public void cancel(com.hardcoders.taskmanager.application.command.VersionedCommand c) {}
            @Override public void reorderTasks(Integer activityId, List<Integer> taskIds) {}
        }
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> cb = (TransactionCallback<Object>) invocation.getArgument(0);
                    return cb.doInTransaction(mock(TransactionStatus.class));
                });
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        ManageTasksUseCase port = wrap(ManageTasksUseCase.class, new Impl(), tm, false);
        assertThatThrownBy(() -> port.start(null))
                .isInstanceOf(NoSuchMethodError.class);
    }
}
