package com.hardcoders.taskmanager.application.port.in;

import com.hardcoders.taskmanager.application.result.ProjectProgressResult;

/** Puerto de entrada de US3.3: progreso en cascada del proyecto. */
public interface QueryProgressUseCase {

    ProjectProgressResult progress(Integer projectId);
}
