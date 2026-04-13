package com.springbootexample.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DashboardService dashboardService;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
	public Map<String, Object> process(String userId, String input) {

        // 🔥 STEP 1: Call OpenAI
        String aiResponse = callOpenAI(input);

        // 🔥 STEP 2: Convert JSON string → Map
        Map<String, Object> json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(aiResponse, Map.class);
        } catch (Exception e) {
            return Map.of("reply", "AI response parsing error ❌");
        }

        String action = (String) json.get("action");

        // ================= CREATE CUSTOMER =================
        if ("CREATE_CUSTOMER".equals(action)) {

            Map<String, Object> req = new HashMap<>();
            req.put("name", json.get("name"));
            req.put("mobile", json.get("mobile"));
            req.put("openingBalance", json.getOrDefault("openingBalance", 0));
            req.put("description", json.getOrDefault("description", ""));

            customerService.createFromAi(req, userId);

            return Map.of("reply", "Customer created successfully ✅");
        }

        // ================= ADD TRANSACTION =================
        if ("ADD_TRANSACTION".equals(action)) {

            Map<String, Object> req = new HashMap<>();
            req.put("name", json.get("name"));
            req.put("amount", json.get("amount"));
            req.put("type", json.get("type"));
            req.put("description", json.getOrDefault("description", ""));

            transactionService.createFromAi(req, userId);

            return Map.of("reply", "Transaction added successfully ✅");
        }

        // ================= DASHBOARD =================
        if ("GET_DASHBOARD".equals(action)) {

            Map<String, Object> data =
                    dashboardService.getSummary(Long.parseLong(userId));

            return Map.of(
                "reply",
                "Total Customers: " + data.get("totalCustomers") +
                ", Outstanding: " + data.get("totalOutstanding")
            );
        }

        return Map.of("reply", "Sorry, I didn’t understand ❌");
    }

    // 🔥 OPENAI CALL
    private String callOpenAI(String input) {

        String prompt = """
    You are an AI assistant.

    Convert user input into JSON.

    Actions:
    CREATE_CUSTOMER
    ADD_TRANSACTION
    GET_DASHBOARD

    Return only JSON:

    {
     "action": "",
     "name": "",
     "mobile": "",
     "amount": 0,
     "type": "",
     "openingBalance": 0,
     "description": ""
    }

    User Input: "%s"
    """.formatted(input);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("temperature", 0);

        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        Map response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                request,
                Map.class
        );

        // 🔥 Extract response
        Map choice = (Map) ((List) response.get("choices")).get(0);
        Map message = (Map) choice.get("message");

        return message.get("content").toString().trim();
    }
}