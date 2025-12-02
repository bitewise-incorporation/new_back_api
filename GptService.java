package br.com.bitewise.api.service;

import br.com.bitewise.api.dto.RecipeResponse;
import br.com.bitewise.api.dto.NutritionFacts;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private static final Logger logger = LoggerFactory.getLogger(GptService.class);

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4-turbo}")
    private String model;

    @Value("${openai.image.api.url:https://api.openai.com/v1/images/generations}")
    private String imageApiUrl;

    @Value("${openai.image.model:dall-e-3}")
    private String imageModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        logger.info("🔧 [GptService] Initializing...");
        logger.info("🔧 [GptService] apiKey from config: {} (length: {})", apiKey == null ? "null" : "***REDACTED***", apiKey == null ? 0 : apiKey.trim().length());
        logger.info("🔧 [GptService] apiUrl from config: {}", apiUrl);
        logger.info("🔧 [GptService] model from config: {}", model);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("❌ [GptService] OpenAI API key NOT configured. GPT service will NOT be available.");
        } else {
            logger.info("✅ [GptService] OpenAI Service initialized successfully with model: {}", model);
        }
    }

    public boolean isAvailable() {
        boolean available = apiKey != null && !apiKey.trim().isEmpty();
        logger.debug("🔍 [GptService] isAvailable() called - result: {}", available);
        return available;
    }

    private RecipeResponse executeGptRequest(String prompt) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        // Build the request
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", 0.7);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        logger.info("🔄 Calling OpenAI API with model: {}", model);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            String responseBody = response.getBody();
            logger.debug("Raw response from OpenAI API: {}", responseBody);

            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("OpenAI API returned empty response");
                throw new RuntimeException("Falha ao gerar receita: IA retornou resposta vazia.");
            }

            // Parse OpenAI response
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                logger.error("OpenAI response does not contain choices: {}", responseBody);
                throw new RuntimeException("Falha ao gerar receita: estrutura de resposta inesperada.");
            }

            String contentText = choices.get(0).path("message").path("content").asText();

            if (contentText == null || contentText.trim().isEmpty()) {
                logger.error("OpenAI returned empty content");
                throw new RuntimeException("Falha ao gerar receita: IA retornou conteúdo vazio.");
            }

            // Parse the JSON recipe from response
            logger.debug("Recipe JSON extracted: {}", contentText);
            RecipeResponse recipe = objectMapper.readValue(contentText, RecipeResponse.class);
            
            // Ensure nutrition facts are set
            if (recipe.getNutrition() == null) {
                recipe.setNutrition(new NutritionFacts());
            }

            return recipe;

        } catch (HttpClientErrorException e) {
            logger.error("❌ OpenAI API error: Status={}, Response={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao comunicar com OpenAI: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            logger.error("❌ Error parsing OpenAI response: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar resposta da IA.", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error generating recipe with GPT: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar receita com a IA.", e);
        }
    }

    public RecipeResponse generateRecipe(List<String> ingredients) {
        String ingredientList = String.join(", ", ingredients);
        logger.info("🍳 [GptService] RECIPE GENERATION STARTED with ingredients: {}", ingredientList);
        
        String prompt = String.format(
            "Você é um chef especializado em receitas. Gere uma receita COMPLETA em JSON com TODOS os campos preenchidos. " +
            "Ingredientes principais: %s. Use temperos e ingredientes básicos comuns se necessário. " +
            "Retorne um JSON válido com esta exata estrutura: " +
            "{ \"title\": \"string\", \"prepTime\": \"string\", \"servings\": number, \"difficulty\": \"string\", " +
            "\"ingredients\": [\"string\"], \"steps\": [\"string\"], \"tips\": [\"string\"], " +
            "\"nutrition\": { \"calories\": number, \"proteinGrams\": number, \"fatGrams\": number, \"carbsGrams\": number } }",
            ingredientList
        );

        RecipeResponse recipe = executeGptRequest(prompt);
        logger.info("📖 [GptService] Recipe generated: {}", recipe.getTitle());
        
        // Generate image for the recipe
        logger.info("📸 [GptService] Starting image generation for: {}", recipe.getTitle());
        String imageUrl = generateImage(recipe.getTitle());
        recipe.setImage(imageUrl);
        
        if (imageUrl != null) {
            logger.info("✅ [GptService] Recipe generation COMPLETED with image");
        } else {
            logger.warn("⚠️  [GptService] Recipe generated but image generation failed (returning recipe without image)");
        }
        
        return recipe;
    }

    public RecipeResponse modifyRecipe(String originalRecipeJson, String instruction) {
        logger.info("🔄 [GptService] RECIPE MODIFICATION STARTED - Instruction: {}", instruction);
        
        String prompt = String.format(
            "Você é um chef IA especializado em modificação de receitas. Sua tarefa é criar uma NOVA receita JSON " +
            "que atenda à instrução de modificação. Mantenha a estrutura JSON IDÊNTICA. " +
            "Instrução: '%s'. Receita original: %s. " +
            "Retorne um JSON válido com esta exata estrutura: " +
            "{ \"title\": \"string\", \"prepTime\": \"string\", \"servings\": number, \"difficulty\": \"string\", " +
            "\"ingredients\": [\"string\"], \"steps\": [\"string\"], \"tips\": [\"string\"], " +
            "\"nutrition\": { \"calories\": number, \"proteinGrams\": number, \"fatGrams\": number, \"carbsGrams\": number } }",
            instruction,
            originalRecipeJson
        );

        RecipeResponse recipe = executeGptRequest(prompt);
        logger.info("📖 [GptService] Modified recipe generated: {}", recipe.getTitle());
        
        // Generate image for the modified recipe
        logger.info("📸 [GptService] Starting image generation for modified recipe: {}", recipe.getTitle());
        String imageUrl = generateImage(recipe.getTitle());
        recipe.setImage(imageUrl);
        
        if (imageUrl != null) {
            logger.info("✅ [GptService] Recipe modification COMPLETED with image");
        } else {
            logger.warn("⚠️  [GptService] Recipe modified but image generation failed (returning recipe without image)");
        }
        
        return recipe;
    }

    /**
     * Generate an image for a recipe using OpenAI DALL-E API
     */
    private String generateImage(String recipeTitle) {
        try {
            logger.info("🎨 [GptService] IMAGE GENERATION STARTED for recipe: {}", recipeTitle);
            
            if (!isAvailable()) {
                logger.warn("⚠️  [GptService] OpenAI API key not available, skipping image generation");
                return null;
            }

            // Build the image prompt (ENGLISH - as required)
            String imagePrompt = String.format(
                "Ultra high-quality, hyper-realistic professional food photography of: %s. " +
                "Shot with a professional DSLR camera with perfect lighting, golden hour illumination, and macro lens. " +
                "Extremely detailed textures, perfect composition, vibrant colors, appetizing presentation. " +
                "Sharp focus on the main dish, blurred background with neutral tones. " +
                "Garnished beautifully, steam rising if applicable, fresh ingredients visible. " +
                "High resolution, studio lighting, professional color grading, Michelin-star restaurant quality. " +
                "No cartoon, no drawings, no watermarks. Pure photorealism with cinematic depth of field.",
                recipeTitle
            );
            
            logger.debug("📸 [GptService] Image prompt: {}", imagePrompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", imageModel);
            requestBody.put("prompt", imagePrompt);
            requestBody.put("n", 1);
            requestBody.put("size", "1024x1024");
            requestBody.put("quality", "standard");
            requestBody.put("response_format", "b64_json"); // Request base64 encoded image

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            logger.info("🔄 [GptService] Calling OpenAI DALL-E API - Model: {}, Size: 1024x1024, Quality: standard", imageModel);

            ResponseEntity<String> response = restTemplate.postForEntity(imageApiUrl, entity, String.class);
            String responseBody = response.getBody();
            logger.debug("Response status from DALL-E: {}", response.getStatusCode());

            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("❌ [GptService] DALL-E API returned empty response");
                return null;
            }

            // Parse response and extract base64 image
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dataArray = rootNode.path("data");

            if (!dataArray.isArray() || dataArray.isEmpty()) {
                logger.error("❌ [GptService] DALL-E response does not contain data array: {}", responseBody);
                return null;
            }

            String base64Image = dataArray.get(0).path("b64_json").asText();
            if (base64Image == null || base64Image.isEmpty()) {
                logger.error("❌ [GptService] DALL-E response does not contain b64_json field");
                return null;
            }

            logger.info("✅ [GptService] IMAGE GENERATION COMPLETED successfully for: {} (base64 size: {} bytes)", recipeTitle, base64Image.length());
            return "data:image/png;base64," + base64Image;

        } catch (HttpClientErrorException e) {
            logger.error("❌ [GptService] DALL-E API HTTP error: Status={}, Message={}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (JsonProcessingException e) {
            logger.error("❌ [GptService] Error parsing DALL-E JSON response: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("❌ [GptService] Unexpected error during image generation: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Public method to generate image for a recipe - used by other services for fallback
     */
    public String generateImageForRecipe(String recipeTitle) {
        logger.info("📸 [GptService] PUBLIC IMAGE GENERATION CALLED for: {}", recipeTitle);
        return generateImage(recipeTitle);
    }
}