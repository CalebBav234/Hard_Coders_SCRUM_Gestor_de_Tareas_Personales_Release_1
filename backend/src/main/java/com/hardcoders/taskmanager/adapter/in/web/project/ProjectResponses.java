package com.hardcoders.taskmanager.adapter.in.web.project;

import com.hardcoders.taskmanager.application.result.PlanRevisionResult;
import com.hardcoders.taskmanager.application.result.ProjectResult;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato HTTP de proyectos. Montos en BOB, duraciones en minutos, {@code version} = token de concurrencia. */
public final class ProjectResponses {

    private ProjectResponses() {
    }

    public record UserSummaryResponse(Integer userId, String fullName, String email, boolean chief, boolean active) {
        public static UserSummaryResponse from(UserSummaryResult result) {
            return new UserSummaryResponse(result.userId(), result.fullName(), result.email(), result.chief(),
                    result.active());
        }
    }

    public record ProjectResponse(
            Integer projectId,
            String name,
            String description,
            String status,
            boolean closed,
            BigDecimal budgetBob,
            BigDecimal allocatedBudgetBob,
            int estimatedDurationMinutes,
            UserSummaryResponse manager,
            UserSummaryResponse createdBy,
            int currentRevision,
            Instant createdAt,
            Instant updatedAt,
            long version,
            boolean canEditDetails,
            boolean canEditPlan,
            boolean canDelegate,
            boolean canManageHierarchy,
            List<String> allowedStatuses
    ) {
        public static ProjectResponse from(ProjectResult result) {
            return new ProjectResponse(result.projectId(), result.name(), result.description(), result.status(),
                    result.closed(), result.budgetBob(), result.allocatedBudgetBob(),
                    result.estimatedDurationMinutes(), UserSummaryResponse.from(result.manager()),
                    UserSummaryResponse.from(result.createdBy()), result.currentRevision(), result.createdAt(),
                    result.updatedAt(), result.version(), result.canEditDetails(), result.canEditPlan(),
                    result.canDelegate(), result.canManageHierarchy(), result.allowedStatuses());
        }
    }

    public record PlanRevisionResponse(
            int revisionNumber,
            BigDecimal budgetBob,
            int estimatedDurationMinutes,
            Instant changedAt,
            UserSummaryResponse changedBy,
            String reason
    ) {
        public static PlanRevisionResponse from(PlanRevisionResult result) {
            return new PlanRevisionResponse(result.revisionNumber(), result.budgetBob(),
                    result.estimatedDurationMinutes(), result.changedAt(),
                    UserSummaryResponse.from(result.changedBy()), result.reason());
        }
    }
}
