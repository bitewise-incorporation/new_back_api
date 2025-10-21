package br.com.bitewise.api.repository;

import br.com.bitewise.api.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}