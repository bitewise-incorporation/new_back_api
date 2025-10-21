package br.com.bitewise.api.dto;

import lombok.Data;

@Data
public class NutritionFacts {
    private Double calories;
    private Double proteinGrams;
    private Double fatGrams;
    private Double carbsGrams;
}