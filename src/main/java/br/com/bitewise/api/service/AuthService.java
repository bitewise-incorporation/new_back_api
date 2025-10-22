package br.com.bitewise.api.service;

import br.com.bitewise.api.model.User;
import br.com.bitewise.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String name, String email, String password) throws Exception {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new Exception("Erro: Email já está em uso!");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(name, email, encodedPassword);
        userRepository.save(newUser);
    }
}