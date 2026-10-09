package com.hardcoders.taskmanager.domain.model;

/**
 * Actividad del espacio de trabajo. En la interfaz se muestra como "categoría";
 * la actividad {@link #UNCATEGORIZED} representa las tareas sin categoría.
 */
public record Activity(Integer id, String name) {

    public static final String UNCATEGORIZED = "Sin categoría";

    public boolean isUncategorized() {
        return UNCATEGORIZED.equalsIgnoreCase(name);
    }

    /** Nombre visible como categoría: nulo para "Sin categoría". */
    public String categoryName() {
        return isUncategorized() ? null : name;
    }
}
