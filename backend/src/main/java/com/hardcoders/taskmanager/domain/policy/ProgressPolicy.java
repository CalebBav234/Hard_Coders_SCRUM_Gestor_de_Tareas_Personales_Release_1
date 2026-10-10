package com.hardcoders.taskmanager.domain.policy;

/**
 * US3.3: progreso determinista por trabajo hoja. {@code Progress = completedLeaf / totalLeaf}.
 * Sin trabajo hoja el progreso es 0 % (AC-US3.3-02) y nunca se auto-cierra el proyecto
 * (AC-US3.3-04). Los contadores son {@code long} para evitar desbordes (AC-US3.3-03).
 */
public final class ProgressPolicy {

    private ProgressPolicy() {
    }

    public static double leafWorkRatio(long completed, long total) {
        if (total <= 0) {
            return 0.0;
        }
        long done = Math.max(0, Math.min(completed, total));
        return Math.min(1.0, (double) done / (double) total);
    }

    /** Suma acotada: no desborda aunque los subárboles devuelvan valores incoherentes. */
    public static long add(long left, long right) {
        long result = left + right;
        if (result < 0) {
            return Long.MAX_VALUE;
        }
        return result;
    }
}
