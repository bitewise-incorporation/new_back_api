package br.com.bitewise.api.service;

import br.com.bitewise.api.dto.RecipeResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${google.api.key:}")
    private String apiKey;

    @Value("${google.cloud.project-id:}")
    private String projectId;

    @Value("${google.cloud.location:}")
    private String location;

    @Value("${gemini.api.base-url:}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            String msg = "Propriedade 'gemini.api.base-url' não encontrada. Adicione em application.properties ou via variável de ambiente.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            String msg = "Propriedade 'google.api.key' não encontrada. Adicione em application.properties ou via variável de ambiente.";
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        logger.info("Inicializando GeminiService (REST) com Project ID: {} e Location: {}", projectId, location);
        logger.debug("Comprimento da API key injetada: {}", apiKey == null ? 0 : apiKey.length());
    }

    private RecipeResponse executeGeminiRequest(String prompt) {
        String cleanKey = apiKey == null ? "" : apiKey.replaceAll("[\\[\\]\\(\\)\\s]", "").trim();

        URI uri;
        try {
            uri = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .queryParam("key", cleanKey)
                    .build()
                    .toUri();
        } catch (Exception e) {
            logger.error("Erro ao construir URI da API Gemini: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao construir URI da API Gemini", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.7);

        Map<String, Object> nutritionSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "calories", Map.of("type", "number"),
                        "proteinGrams", Map.of("type", "number"),
                        "fatGrams", Map.of("type", "number"),
                        "carbsGrams", Map.of("type", "number")
                )
        );

        Map<String, Object> responseSchema = new HashMap<>();
        responseSchema.put("type", "OBJECT");
        responseSchema.put("properties", Map.of(
                "title", Map.of("type", "string"),
                "prepTime", Map.of("type", "string"),
                "servings", Map.of("type", "integer"),
                "difficulty", Map.of("type", "string"),
                "ingredients", Map.of("type", "array", "items", Map.of("type", "string")),
                "steps", Map.of("type", "array", "items", Map.of("type", "string")),
                "tips", Map.of("type", "array", "items", Map.of("type", "string")),
                "nutrition", nutritionSchema
        ));
        generationConfig.put("responseSchema", responseSchema);

        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String maskedUri = uri.toString().replaceAll("key=[^&]+", "key=***");
        logger.info("Chamando Gemini: {} (key length={})", maskedUri, cleanKey.length());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uri, entity, String.class);
            String responseBody = response.getBody();
            logger.debug("Resposta crua da Gemini API: {}", responseBody);

            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("Gemini API retornou resposta vazia.");
                throw new RuntimeException("Falha ao gerar receita: IA retornou resposta vazia.");
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {

                JsonNode promptFeedback = rootNode.path("promptFeedback");
                if (promptFeedback.path("blockReason").isTextual()) {
                    logger.error("Prompt bloqueado pela política de segurança. Motivo: {}", promptFeedback.path("blockReason").asText());
                    throw new RuntimeException("Falha ao gerar receita: O prompt foi bloqueado pela política de segurança da IA.");
                }

                logger.error("Resposta da Gemini API não contém 'candidates': {}", responseBody);
                throw new RuntimeException("Falha ao gerar receita: resposta inesperada da IA.");
            }

            JsonNode contentNode = candidates.get(0).path("content");
            JsonNode parts = contentNode.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                logger.error("Resposta da Gemini API não contém 'parts' dentro de 'content': {}", responseBody);
                throw new RuntimeException("Falha ao gerar receita: estrutura de resposta inesperada.");
            }
            String jsonRecipeText = parts.get(0).path("text").asText();

            if (jsonRecipeText == null || jsonRecipeText.trim().isEmpty()) {
                logger.error("Gemini retornou texto vazio dentro de 'parts': {}", responseBody);
                throw new RuntimeException("Falha ao gerar receita: IA retornou conteúdo vazio.");
            }

            jsonRecipeText = jsonRecipeText.replace("```json", "").replace("```", "").trim();

            logger.debug("JSON da receita extraído: {}", jsonRecipeText);
            return objectMapper.readValue(jsonRecipeText, RecipeResponse.class);

        } catch (HttpClientErrorException e) {
            logger.error("Erro HTTP ao chamar a Gemini API: Status={}, Resposta={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao comunicar com a IA: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            logger.error("Erro ao processar JSON da Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar resposta da IA.", e);
        } catch (Exception e) {
            logger.error("Erro inesperado ao gerar receita: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar receita com a IA.", e);
        }
    }


    public RecipeResponse generateRecipe(List<String> ingredients) {
        String ingredientList = String.join(", ", ingredients);
        String prompt = String.format(
                "Gere uma receita completa. É OBRIGATÓRIO preencher TODOS os campos. Use APENAS os seguintes ingredientes principais: %s. Use temperos e ingredientes básicos comuns se necessário.",
                ingredientList
        );

        return executeGeminiRequest(prompt);
    }

    public RecipeResponse modifyRecipe(String originalRecipeJson, String instruction) {
        // 1. Extrair a lista de ingredientes do JSON original
        String originalIngredients = "Falha ao extrair ingredientes.";
        try {
            JsonNode root = objectMapper.readTree(originalRecipeJson);
            JsonNode ingredientsNode = root.path("ingredients");
            if (ingredientsNode.isArray()) {
                originalIngredients = "Lista de Ingredientes Originais: " + objectMapper.writeValueAsString(ingredientsNode);
            }
        } catch (JsonProcessingException e) {
            logger.error("Falha ao ler JSON da receita original para modificação: {}", e.getMessage());
        }

        // 2. Novo Prompt Otimizado: Foca na modificação e na saída JSON
        String prompt = String.format(
                "Você é um Chef IA especializado em modificação de receitas. Sua tarefa é criar uma NOVA receita JSON que atenda à instrução de modificação. Mantenha a estrutura JSON IDÊNTICA. Instrução: '%s'. A nova receita deve se basear no conceito de: %s. É OBRIGATÓRIO preencher TODOS os campos e fornecer a nova análise nutricional.",
                instruction,
                originalIngredients
        );

        return executeGeminiRequest(prompt);
    }
}