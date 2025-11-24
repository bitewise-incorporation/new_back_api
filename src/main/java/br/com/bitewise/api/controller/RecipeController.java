package br.com.bitewise.api.controller;

import br.com.bitewise.api.dto.GenerateRecipeRequest;
import br.com.bitewise.api.dto.MessageResponse;
import br.com.bitewise.api.dto.ModifyRecipeRequest;
import br.com.bitewise.api.dto.RecipeResponse;
import br.com.bitewise.api.dto.SaveRecipeRequest;
import br.com.bitewise.api.model.Recipe;
import br.com.bitewise.api.model.SavedRecipe;
import br.com.bitewise.api.model.User;
import br.com.bitewise.api.repository.RecipeRepository;
import br.com.bitewise.api.repository.SavedRecipeRepository;
import br.com.bitewise.api.repository.UserRepository;
import br.com.bitewise.api.service.GeminiService;
import br.com.bitewise.api.service.GptService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private GptService gptService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @PostMapping("/generate")
    public ResponseEntity<?> generateRecipe(
            @Valid @RequestBody GenerateRecipeRequest request,
            @RequestParam(value = "aiModel", defaultValue = "auto") String aiModel) {
        try {
            logger.info("🚀 [RecipeController] POST /api/recipes/generate with model: {}", aiModel);
            
            // Verificar autenticação
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("❌ [RecipeController] Usuário não autenticado!");
                return ResponseEntity.status(401).body(new MessageResponse("Não autenticado"));
            }
            
            String userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
            logger.info("👤 [RecipeController] Usuário autenticado: {}", userEmail);
            logger.info("📝 [RecipeController] Ingredientes recebidos: {}", request.getIngredients());
            
            // Escolher modelo de IA
            RecipeResponse recipe;
            if ("gpt".equalsIgnoreCase(aiModel)) {
                if (!gptService.isAvailable()) {
                    logger.warn("⚠️  GPT not available, falling back to Gemini");
                    recipe = geminiService.generateRecipe(request.getIngredients());
                } else {
                    logger.info("🤖 Using GPT model for recipe generation");
                    recipe = gptService.generateRecipe(request.getIngredients());
                }
            } else if ("auto".equalsIgnoreCase(aiModel)) {
                // Try GPT first if available, fallback to Gemini
                if (gptService.isAvailable()) {
                    logger.info("🤖 Auto mode: Using GPT (available)");
                    try {
                        recipe = gptService.generateRecipe(request.getIngredients());
                    } catch (Exception e) {
                        logger.warn("⚠️  GPT failed in auto mode, falling back to Gemini: {}", e.getMessage());
                        recipe = geminiService.generateRecipe(request.getIngredients());
                    }
                } else {
                    logger.info("🤖 Auto mode: GPT not available, using Gemini");
                    recipe = geminiService.generateRecipe(request.getIngredients());
                }
            } else {
                logger.info("🤖 Using Gemini model for recipe generation");
                recipe = geminiService.generateRecipe(request.getIngredients());
            }
            
            logger.info("✅ [RecipeController] Receita gerada com sucesso!");
            logger.info("📋 [RecipeController] Título: {}", recipe.getTitle());
            
            return ResponseEntity.ok(recipe);
        } catch (HttpClientErrorException e) {
            logger.error("❌ [RecipeController] Erro HTTP IA: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(500).body(new MessageResponse("Erro ao chamar IA: " + e.getStatusCode()));
        } catch (Exception e) {
            logger.error("❌ [RecipeController] Erro geral: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new MessageResponse("Erro: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<MessageResponse> saveRecipe(@Valid @RequestBody SaveRecipeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        Recipe newRecipe = new Recipe();
        newRecipe.setTitle(request.getTitle());
        newRecipe.setPrepTime(request.getPrepTime());
        newRecipe.setServings(request.getServings());
        newRecipe.setDifficulty(request.getDifficulty());
        newRecipe.setIngredients(request.getIngredients());
        newRecipe.setSteps(request.getSteps());
        newRecipe.setTips(request.getTips());

        Recipe savedRecipe = recipeRepository.save(newRecipe);

        SavedRecipe savedLink = new SavedRecipe();
        savedLink.setUser(user);
        savedLink.setRecipe(savedRecipe);

        savedRecipeRepository.save(savedLink);

        return ResponseEntity.ok(new MessageResponse("Receita '" + savedRecipe.getTitle() + "' salva com sucesso!"));
    }

    @PostMapping("/modify")
    public ResponseEntity<?> modifyRecipe(@Valid @RequestBody ModifyRecipeRequest request) {
        try {
            RecipeResponse modifiedRecipe = geminiService.modifyRecipe(request.getOriginalRecipeJson(), request.getModificationInstruction());
            return ResponseEntity.ok(modifiedRecipe);
        } catch (Exception e) {
            logger.error("Erro ao modificar a receita: {}", e.getMessage());
            throw new RuntimeException("Falha ao modificar a receita com a IA.", e);
        }
    }
}