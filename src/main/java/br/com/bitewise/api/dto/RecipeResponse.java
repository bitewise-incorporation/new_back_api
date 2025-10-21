package br.com.bitewise.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeResponse {
    private String title;
    private String prepTime;
    private Integer servings;
    private String difficulty;
    private List<String> ingredients;
    private List<String> steps;
    private List<String> tips;
    private NutritionFacts nutrition;
}