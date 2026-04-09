package com.springbootexample.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.dto.LoginRequest;
import com.springbootexample.dto.LoginResponse;
import com.springbootexample.dto.RegisterRequest;
import com.springbootexample.entity.User;
import com.springbootexample.service.AuthService;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	    @Autowired
	    private AuthService authService; // 🔥 interface use

	    @PostMapping("/register")
	    public User register(@RequestBody RegisterRequest request) {
	        return authService.register(request);
	    }
	    @PostMapping("/login")
	    public LoginResponse login(@RequestBody LoginRequest request) {
	        return authService.login(request);
	    }
	
}
