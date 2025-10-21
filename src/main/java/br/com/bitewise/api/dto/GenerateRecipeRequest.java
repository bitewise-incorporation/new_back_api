package br.com.bitewise.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GenerateRecipeRequest {

    @NotEmpty(message = "A lista de ingredientes não pode ser vazia.")
    @Size(min = 3, message = "É necessário fornecer pelo menos 3 ingredientes.")
    private List<String> ingredients;
}