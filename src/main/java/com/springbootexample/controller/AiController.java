package com.springbootexample.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import com.springbootexample.service.AiService;
import com.springbootexample.security.JwtUtil;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private JwtUtil jwtUtil;
    // Extract userId from token
    private Long getUserId(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing");
        }

        String token = header.replace("Bearer ", "");

        return jwtUtil.extractUserId(token);
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(
            @RequestBody Map<String, String> req,
            HttpServletRequest request) {
        Long userId = getUserId(request); //  dynamic
        return aiService.process(String.valueOf(userId), req.get("message"));
    }
}