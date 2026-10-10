package com.hardcoders.taskmanager.adapter.in.web.task;

import com.hardcoders.taskmanager.application.port.in.QueryProgressUseCase;
import com.hardcoders.taskmanager.application.result.ProgressNodeResult;
import com.hardcoders.taskmanager.application.result.ProjectProgressResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * US3.3: progreso en cascada. El nodo raíz es el proyecto; el porcentaje se calcula sobre trabajo
 * hoja, así que una tarea con subtareas no se cuenta dos veces.
 */
@RestController
@RequestMapping("/api")
public class ProgressController {

    private final QueryProgressUseCase progress;

    public ProgressController(QueryProgressUseCase progress) {
        this.progress = progress;
    }

    @GetMapping("/projects/{projectId}/progress")
    public ProgressResponse progressOfProject(@PathVariable Integer projectId) {
        ProjectProgressResult result = progress.progress(projectId);
        return new ProgressResponse(result.projectId(), result.projectName(), result.projectStatus(),
                result.budgetBob(), result.allocatedBudgetBob(), result.estimatedDurationMinutes(),
                toNode(result.root()));
    }

    private static ProgressNodeResponse toNode(ProgressNodeResult node) {
        List<ProgressNodeResponse> children = node.children().stream()
                .map(ProgressController::toNode)
                .toList();
        return new ProgressNodeResponse(node.id(), node.name(), node.completed(), node.total(), node.leaf(),
                node.ratio(), children);
    }

    public record ProgressNodeResponse(
            Integer id,
            String name,
            long completed,
            long total,
            boolean leaf,
            double ratio,
            List<ProgressNodeResponse> children
    ) {
    }

    public record ProgressResponse(
            Integer projectId,
            String projectName,
            String projectStatus,
            BigDecimal budgetBob,
            BigDecimal allocatedBudgetBob,
            Integer estimatedDurationMinutes,
            ProgressNodeResponse root
    ) {
    }
}
