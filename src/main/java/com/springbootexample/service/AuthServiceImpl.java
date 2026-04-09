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
		User user = userRepository
	            .findByMobileOrEmail(request.getUsername(), request.getUsername())
	            .orElseThrow(() -> new RuntimeException("User not found"));
		
		
		 if(!user.getPassword().equals(request.getPassword())) {
			  throw new RuntimeException("Passwords do not match");
		 }
		 
		 
		 String token = jwtUtil.generateToken(user.getId());
		 
		LoginResponse responce = new LoginResponse();
		responce.setUserId(user.getId());
		responce.setShopName(user.getShopName());
		responce.setToken(jwtUtil.generateToken(user.getId()));
		responce.setMessage("login sucessfully");
		return responce;
	}
	
	

}
