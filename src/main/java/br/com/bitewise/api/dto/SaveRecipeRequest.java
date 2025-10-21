package br.com.bitewise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SaveRecipeRequest {
    // Campos da Receita
    @NotBlank
    private String title;

    @NotBlank
    private String prepTime;

    @NotNull
    private Integer servings;

    @NotBlank
    private String difficulty;

    @NotEmpty
    private List<String> ingredients;

    @NotEmpty
    private List<String> steps;

    private List<String> tips;
}