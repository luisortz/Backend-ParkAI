package com.parkai.backend.controller;

import com.parkai.backend.model.User;
import com.parkai.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> list() {
        return userRepository.findAll();
    }

    @PostMapping
    public User create(@RequestBody @Valid User user) {
        return userRepository.save(user);
    }
}
