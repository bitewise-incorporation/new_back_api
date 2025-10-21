package br.com.bitewise.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Anotação que diz ao Spring que esta classe responde a APIs REST
@RequestMapping("/api/health") // Todas as rotas aqui começarão com /api/health
public class HealthController {

    @GetMapping // Responde a uma requisição GET
    public String checkHealth() {
        return "Backend BiteWise (Spring Boot) está no ar!";
    }
}