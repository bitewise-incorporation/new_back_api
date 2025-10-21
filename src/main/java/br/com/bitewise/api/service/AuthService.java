package br.com.bitewise.api.service;

import br.com.bitewise.api.dto.RegisterRequest;
import br.com.bitewise.api.model.User;
import br.com.bitewise.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Erro: Email já está em uso!");
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        User newUser = new User(
                registerRequest.getName(),
                registerRequest.getEmail(),
                hashedPassword
        );

        userRepository.save(newUser);
    }
}