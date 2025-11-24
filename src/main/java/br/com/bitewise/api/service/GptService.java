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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("⚠️  OpenAI API key not configured. GPT service will not be available.");
        } else {
            logger.info("✅ GPT Service initialized with model: {}", model);
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
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
        String prompt = String.format(
            "Você é um chef especializado em receitas. Gere uma receita COMPLETA em JSON com TODOS os campos preenchidos. " +
            "Ingredientes principais: %s. Use temperos e ingredientes básicos comuns se necessário. " +
            "Retorne um JSON válido com esta exata estrutura: " +
            "{ \"title\": \"string\", \"prepTime\": \"string\", \"servings\": number, \"difficulty\": \"string\", " +
            "\"ingredients\": [\"string\"], \"steps\": [\"string\"], \"tips\": [\"string\"], " +
            "\"nutrition\": { \"calories\": number, \"proteinGrams\": number, \"fatGrams\": number, \"carbsGrams\": number } }",
            ingredientList
        );

        return executeGptRequest(prompt);
    }

    public RecipeResponse modifyRecipe(String originalRecipeJson, String instruction) {
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

        return executeGptRequest(prompt);
    }
}
