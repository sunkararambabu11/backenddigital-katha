package com.springbootexample.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.dto.LoginRequest;
import com.springbootexample.dto.LoginResponse;
import com.springbootexample.dto.ProfileResponse;
import com.springbootexample.dto.RegisterRequest;
import com.springbootexample.entity.User;
import com.springbootexample.repository.UserRepository;
import com.springbootexample.security.JwtUtil;
import com.springbootexample.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private  UserRepository userRepository;
	    @Autowired
	    private AuthService authService;  //  interface use
		@Autowired
     private JwtUtil jwtUtil;

	    @PostMapping("/register")
	    public User register(@RequestBody RegisterRequest request) {
	        return authService.register(request);
	    }
	    @PostMapping("/login")
	    public LoginResponse login(@RequestBody LoginRequest request) {
	        return authService.login(request);
	    }

	

 private Long getUserId(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing");
        }

        String token = header.substring(7);

        return jwtUtil.extractUserId(token);
    }

	@GetMapping("/profile")
    public ProfileResponse profile(HttpServletRequest request) {

        Long userId = getUserId(request);

        return authService.getProfile(userId);
    }
}
		

