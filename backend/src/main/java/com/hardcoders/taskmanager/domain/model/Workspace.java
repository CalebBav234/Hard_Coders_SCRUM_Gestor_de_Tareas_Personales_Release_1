package com.hardcoders.taskmanager.domain.model;

/**
 * Proyecto y etapa donde trabaja el gestor de tareas personales. Mientras no exista
 * autenticación ni selección de proyecto, es el primer proyecto (y su primera etapa).
 */
public record Workspace(Integer projectId, Integer stageId) {
}
