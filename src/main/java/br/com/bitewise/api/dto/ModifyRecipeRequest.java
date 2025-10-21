package br.com.bitewise.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModifyRecipeRequest {
    private String originalRecipeJson;

    @NotBlank(message = "A instrução de modificação é obrigatória.")
    private String modificationInstruction;
}