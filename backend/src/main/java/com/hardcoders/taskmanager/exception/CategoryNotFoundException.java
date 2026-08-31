package com.hardcoders.taskmanager.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Categoría no encontrada: " + id);
    }
}
