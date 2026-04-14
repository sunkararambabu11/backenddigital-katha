package com.springbootexample.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.springbootexample.dto.LoginRequest;
import com.springbootexample.dto.LoginResponse;
import com.springbootexample.dto.RegisterRequest;
import com.springbootexample.entity.User;
import com.springbootexample.repository.UserRepository;
import com.springbootexample.security.JwtUtil;

@Service
public class AuthServiceImpl implements AuthService {
	@Autowired
	private JwtUtil jwtUtil;
	 @Autowired
	    private UserRepository userRepository;

	@Override
	public User register(RegisterRequest request) {
		if (request.getPassword() == null || request.getConfirmPassword() == null ||
			    !request.getPassword().equals(request.getConfirmPassword())) {

			    throw new RuntimeException("Passwords do not match");
			}
		
		userRepository.findByMobile(request.getMobile())
		.ifPresent(u -> {
			throw new RuntimeException("Mobile already registered");
		});
		
		User user = new User();
		user.setShopName(request.getShopName());
		user.setEmail(request.getEmail());
		user.setMobile(request.getMobile());
		user.setOwnerName(request.getOwnerName());
		user.setPassword(request.getPassword());
		return userRepository.save(user);
	}

	@Override
	public LoginResponse login(LoginRequest request) {

	    String input = request.getUsername().trim();

	    User user;

	    // 🔥 Check if input is mobile (only digits)
	    if (input.matches("\\d+")) {
	        user = userRepository.findByMobile(input)
	                .orElseThrow(() -> new RuntimeException("User not found"));
	    } 
	    // 🔥 Otherwise treat as email
	    else {
	        user = userRepository.findByEmail(input)
	                .orElseThrow(() -> new RuntimeException("User not found"));
	    }

	    // Password check
	    if (!user.getPassword().equals(request.getPassword())) {
	        throw new RuntimeException("Invalid password");
	    }

	    String token = jwtUtil.generateToken(user.getId());

	    LoginResponse response = new LoginResponse();
	    response.setUserId(user.getId());
	    response.setShopName(user.getShopName());
	    response.setToken(token);
	    response.setMessage("Login successfully");

	    return response;
	}
}