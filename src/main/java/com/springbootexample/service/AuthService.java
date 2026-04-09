package com.springbootexample.service;

import com.springbootexample.dto.LoginRequest;
import com.springbootexample.dto.LoginResponse;
import com.springbootexample.dto.RegisterRequest;
import com.springbootexample.entity.User;

public interface AuthService {
	 User register(RegisterRequest request);
	 LoginResponse login(LoginRequest request);
}
