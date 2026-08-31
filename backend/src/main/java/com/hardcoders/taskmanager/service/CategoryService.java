package com.hardcoders.taskmanager.service;

import com.hardcoders.taskmanager.dto.CategorySummary;
import com.hardcoders.taskmanager.repository.CategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> list() {
        return categoryRepository.findVisibleSummaries();
    }

    @Transactional
    public Long findOrCreateVisibleByName(String name) {
        String trimmed = name == null ? null : name.strip();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        Long existing = categoryRepository.findVisibleIdByName(trimmed);
        if (existing != null) {
            return existing;
        }
        try {
            return categoryRepository.insertVisibleAndReturnId(trimmed);
        } catch (DataIntegrityViolationException e) {
            Long after = categoryRepository.findVisibleIdByName(trimmed);
            if (after != null) {
                return after;
            }
            throw e;
        }
    }
}
