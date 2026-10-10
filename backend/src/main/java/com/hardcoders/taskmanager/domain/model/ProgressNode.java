package com.hardcoders.taskmanager.domain.model;

import java.time.Instant;
import com.hardcoders.taskmanager.domain.policy.ProgressPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Progreso en cascada de un nodo de la jerarquía (US3.3).
 * {@code completed}/{@code total} cuentan solo trabajo hoja para no duplicar:
 * una tarea con subtareas aporta sus subtareas; sin subtareas, se cuenta ella misma.
 */
public final class ProgressNode {

    private final Integer id;
    private final String name;
    private final long completed;
    private final long total;
    private final boolean leaf;
    private final List<ProgressNode> children;

    public ProgressNode(Integer id, String name, long completed, long total, boolean leaf, List<ProgressNode> children) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.completed = completed;
        this.total = total;
        this.leaf = leaf;
        this.children = List.copyOf(children);
    }

    public Integer id() {
        return id;
    }

    public String name() {
        return name;
    }

    public long completed() {
        return completed;
    }

    public long total() {
        return total;
    }

    /** Nodo que no tiene hijos: su propio estado cuenta como unidad de trabajo. */
    public boolean leaf() {
        return leaf;
    }

    public List<ProgressNode> children() {
        return children;
    }

    /** 0.0 cuando no hay trabajo hoja (AC-US3.3-02). */
    public double ratio() {
        return ProgressPolicy.leafWorkRatio(completed, total);
    }
}
