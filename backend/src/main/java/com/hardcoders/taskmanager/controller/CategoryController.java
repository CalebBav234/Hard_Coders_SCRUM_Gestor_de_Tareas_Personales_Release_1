package com.hardcoders.taskmanager.controller;

import com.hardcoders.taskmanager.dto.CategorySummary;
import com.hardcoders.taskmanager.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategorySummary> list() {
        return categoryService.list();
    }
}
