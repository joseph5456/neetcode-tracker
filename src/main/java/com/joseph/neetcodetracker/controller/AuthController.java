package com.joseph.neetcodetracker.controller;

import com.joseph.neetcodetracker.dto.AuthDtos.AuthResponse;
import com.joseph.neetcodetracker.dto.AuthDtos.LoginRequest;
import com.joseph.neetcodetracker.dto.AuthDtos.RegisterRequest;
import com.joseph.neetcodetracker.entity.User;
import com.joseph.neetcodetracker.repository.UserRepository;
import com.joseph.neetcodetracker.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = new User(req.username(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        String token = jwtUtil.generateToken(req.username());
        return ResponseEntity.ok(new AuthResponse(token, req.username()));
    }
}
