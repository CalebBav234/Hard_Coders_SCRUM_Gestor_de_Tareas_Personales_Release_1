package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.PlanRevision;
import com.hardcoders.taskmanager.domain.model.Project;
import com.hardcoders.taskmanager.domain.model.ProjectStatus;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.policy.DelegationPolicy;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.policy.WbsHierarchyPolicy;
import com.hardcoders.taskmanager.domain.port.out.ProjectRepository;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Arma los resultados de proyectos con nombres de usuarios y permisos del actor. */
final class ProjectResultAssembler {

    private final ProjectRepository projects;
    private final UserDirectory users;

    ProjectResultAssembler(ProjectRepository projects, UserDirectory users) {
        this.projects = projects;
        this.users = users;
    }

    List<ProjectResult> toResults(Actor actor, Collection<Project> found) {
        Set<Integer> ids = new HashSet<>();
        found.forEach(project -> {
            ids.add(project.managerUserId());
            ids.add(project.createdByUserId());
        });
        Map<Integer, UserAccount> accounts = users.findByIds(ids);
        return found.stream().map(project -> toResult(actor, project, accounts)).toList();
    }

    ProjectResult toResult(Actor actor, Project project) {
        return toResults(actor, List.of(project)).getFirst();
    }

    private ProjectResult toResult(Actor actor, Project project, Map<Integer, UserAccount> accounts) {
        boolean chief = actor.isActiveChief();
        boolean open = !project.status().isClosed();
        List<String> statuses = chief
                ? project.status().allowedTargets().stream().map(ProjectStatus::name).sorted().toList()
                : List.of();
        return new ProjectResult(
                project.id(),
                project.name(),
                project.description(),
                project.status().name(),
                project.status().isClosed(),
                project.budgetBob(),
                projects.allocatedBudget(project.id()),
                project.estimatedDurationMinutes(),
                summary(accounts.get(project.managerUserId()), project.managerUserId()),
                summary(accounts.get(project.createdByUserId()), project.createdByUserId()),
                project.lastRevisionNumber(),
                project.createdAt(),
                project.updatedAt(),
                project.version(),
                open && ProjectAccessPolicy.canView(actor, project),
                open && chief,
                open && DelegationPolicy.canDelegate(actor),
                open && WbsHierarchyPolicy.canManage(actor, project),
                statuses);
    }

    List<PlanRevisionResult> toRevisionResults(List<PlanRevision> revisions) {
        Map<Integer, UserAccount> accounts = users.findByIds(
                revisions.stream().map(PlanRevision::changedByUserId).distinct().toList());
        return revisions.stream()
                .map(revision -> new PlanRevisionResult(
                        revision.revisionNumber(),
                        revision.budgetBob(),
                        revision.estimatedDurationMinutes(),
                        revision.changedAt(),
                        summary(accounts.get(revision.changedByUserId()), revision.changedByUserId()),
                        revision.reason()))
                .toList();
    }

    static UserSummaryResult summary(UserAccount account) {
        return new UserSummaryResult(account.userId(), account.fullName(), account.email(), account.chief(),
                account.active());
    }

    private static UserSummaryResult summary(UserAccount account, Integer fallbackId) {
        return account == null
                ? new UserSummaryResult(fallbackId, "Usuario " + fallbackId, null, false, false)
                : summary(account);
    }

}
