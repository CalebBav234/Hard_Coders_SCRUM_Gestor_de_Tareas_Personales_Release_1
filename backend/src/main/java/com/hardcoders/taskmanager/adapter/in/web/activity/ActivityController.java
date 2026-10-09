package com.hardcoders.taskmanager.adapter.in.web.activity;

import com.hardcoders.taskmanager.adapter.in.web.task.TaskResponses.ActivityResponse;
import com.hardcoders.taskmanager.application.port.in.QueryTasksUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Actividades del espacio de trabajo; la interfaz las presenta como "categorías". */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final QueryTasksUseCase queries;

    public ActivityController(QueryTasksUseCase queries) {
        this.queries = queries;
    }

    @GetMapping
    public List<ActivityResponse> list() {
        return queries.activities().stream().map(ActivityResponse::from).toList();
    }
}
