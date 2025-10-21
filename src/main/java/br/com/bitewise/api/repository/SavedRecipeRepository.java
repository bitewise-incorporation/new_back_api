package br.com.bitewise.api.repository;

import br.com.bitewise.api.model.SavedRecipe;
import br.com.bitewise.api.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {

    // Encontra todas as receitas salvas por um usuário
    List<SavedRecipe> findByUser(User user);

    // Verifica se uma receita específica já foi salva por um usuário
    Optional<SavedRecipe> findByUserAndRecipeId(User user, Long recipeId);
}