package com.parkai.backend.service;

import com.parkai.backend.dto.LoginRequest;
import com.parkai.backend.dto.AuthResponse;
import com.parkai.backend.dto.RegisterRequest;
import com.parkai.backend.dto.ResendCodeRequest;
import com.parkai.backend.dto.UserResponse;
import com.parkai.backend.dto.VerifyRequest;
import com.parkai.backend.exception.TooManyRequestsException;
import com.parkai.backend.model.User;
import com.parkai.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        User user = new User();
    
        user.setName(request.name());
        user.setEmail(request.email());
    
        user.setPassword(
                passwordEncoder.encode(request.password())
        );
    
        String code = String.format(
                "%06d",
                new Random().nextInt(999999)
        );
    
        user.setVerificationCode(code);
    
        user.setVerificationCodeExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
    
        user.setLastVerificationCodeSentAt(
                LocalDateTime.now()
        );
    
        user.setEnabled(false);
    
        User savedUser = userRepository.save(user);
    
        // TEMPORALMENTE DESACTIVADO
        // mailService.sendVerificationCode(
        //         savedUser.getEmail(),
        //         code
        // );
    
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    public AuthResponse verify(VerifyRequest request) {

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow();
    
        System.out.println("Código guardado: " + user.getVerificationCode());
        System.out.println("Código recibido: " + request.code());
        System.out.println("Expira: " + user.getVerificationCodeExpiresAt());
        System.out.println("Ahora: " + LocalDateTime.now());
    
        if (!user.getVerificationCode()
                .equals(request.code())) {
    
            throw new RuntimeException("Invalid code");
        }
    
        if (user.getVerificationCodeExpiresAt()
                .isBefore(LocalDateTime.now())) {
    
            throw new RuntimeException("Code expired");
        }
    
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
    
        userRepository.save(user);
    
        String token = jwtService.generateToken(user.getId());
    
        return new AuthResponse(token);
    }

    public void resendVerificationCode(
        ResendCodeRequest request
        ) {

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow();

        if (user.isEnabled()) {
                throw new RuntimeException(
                        "User already verified"
                );
        }

        if (user.getLastVerificationCodeSentAt() != null&& user.getLastVerificationCodeSentAt().plusSeconds(60).isAfter(LocalDateTime.now())){
                throw new TooManyRequestsException(
                        "Wait 60 seconds before requesting another code"
                );
                }

        String code = String.format(
                "%06d",
                new Random().nextInt(999999)
        );

        user.setVerificationCode(code);

        user.setVerificationCodeExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        user.setLastVerificationCodeSentAt(
        LocalDateTime.now()
        );

        userRepository.save(user);

        mailService.sendVerificationCode(
                user.getEmail(),
                code
        );
}       

        public AuthResponse login(LoginRequest request) {

    User user = userRepository
            .findByEmail(request.email())
            .orElseThrow(() ->
                    new RuntimeException("User not found"));

    if (!passwordEncoder.matches(
            request.password(),
            user.getPassword()
    )) {

        throw new RuntimeException(
                "Invalid password"
        );
    }

    if (!user.isEnabled()) {

        throw new RuntimeException(
                "User not verified"
        );
    }

    String token =
            jwtService.generateToken(user.getId());

    return new AuthResponse(token);
}

public UserResponse getCurrentUser(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }


}