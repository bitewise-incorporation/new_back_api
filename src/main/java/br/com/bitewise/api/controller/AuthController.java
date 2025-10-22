package br.com.bitewise.api.controller;

import br.com.bitewise.api.dto.AuthResponse;
import br.com.bitewise.api.dto.LoginRequest;
import br.com.bitewise.api.dto.MessageResponse;
import br.com.bitewise.api.dto.RegisterRequest;
import br.com.bitewise.api.service.AuthService;
import br.com.bitewise.api.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // envia o DTO inteiro para o service (preserva os dados e a lógica de registro)
            authService.register(registerRequest);
            return ResponseEntity.ok(new MessageResponse("Usuário registrado com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

        String token = jwtUtil.generateToken(userDetails.getUsername());

        // usa o construtor com 2 parâmetros do AuthResponse (token e tipo)
        return ResponseEntity.ok(new AuthResponse(token, "Bearer"));
    }
}
