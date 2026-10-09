package com.hardcoders.taskmanager.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Normaliza un texto de búsqueda: sin acentos, en minúsculas y con los comodines de LIKE escapados.
 * El adaptador de persistencia aplica la misma normalización a las columnas.
 */
public final class SearchText {

    public static final int MAX_LENGTH = 160;

    private SearchText() {
    }

    public static Optional<String> likePattern(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String normalized = Normalizer.normalize(query.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return Optional.of("%" + normalized + "%");
    }
}
