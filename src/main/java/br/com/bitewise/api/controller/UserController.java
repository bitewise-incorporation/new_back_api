package br.com.bitewise.api.controller;

import br.com.bitewise.api.dto.MessageResponse;
import br.com.bitewise.api.dto.SavedRecipeItem;
import br.com.bitewise.api.dto.UpdateProfileRequest;
import br.com.bitewise.api.dto.UserProfileResponse;
import br.com.bitewise.api.model.SavedRecipe;
import br.com.bitewise.api.model.User;
import br.com.bitewise.api.repository.SavedRecipeRepository;
import br.com.bitewise.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco."));

        UserProfileResponse response = new UserProfileResponse(user.getId(), user.getName(), user.getEmail());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = ((UserDetails) authentication.getPrincipal()).getUsername();

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!currentEmail.equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Erro: O novo email já está sendo usado por outra conta.");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse(updatedUser.getId(), updatedUser.getName(), updatedUser.getEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/saved-recipes")
    public ResponseEntity<List<SavedRecipeItem>> getSavedRecipes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco."));

        List<SavedRecipe> savedRecipes = savedRecipeRepository.findByUser(user);

        List<SavedRecipeItem> response = savedRecipes.stream()
                .map(link -> new SavedRecipeItem(
                        link.getId(),
                        link.getRecipe().getId(),
                        link.getRecipe().getTitle(),
                        link.getRecipe().getDifficulty(),
                        link.getSavedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/saved-recipes/{id}")
    public ResponseEntity<MessageResponse> deleteSavedRecipe(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco."));

        SavedRecipe savedRecipe = savedRecipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receita salva não encontrada com ID: " + id));

        if (!savedRecipe.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Acesso negado. Esta receita não pertence ao usuário logado.");
        }

        savedRecipeRepository.delete(savedRecipe);

        return ResponseEntity.ok(new MessageResponse("Receita salva removida com sucesso."));
    }
}