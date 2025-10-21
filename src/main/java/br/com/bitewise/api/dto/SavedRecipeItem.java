package br.com.bitewise.api.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class SavedRecipeItem {
    private Long id;
    private Long recipeId;
    private String title;
    private String difficulty;
    private Instant savedAt;

    public SavedRecipeItem(Long id, Long recipeId, String title, String difficulty, Instant savedAt) {
        this.id = id;
        this.recipeId = recipeId;
        this.title = title;
        this.difficulty = difficulty;
        this.savedAt = savedAt;
    }
}