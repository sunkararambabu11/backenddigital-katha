package com.springbootexample.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.springbootexample.entity.Customer;
import com.springbootexample.security.JwtUtil;
import com.springbootexample.service.CustomerService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService service;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔥 Common method to extract userId
    private Long getUserId(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing ❌");
        }

        String token = header.replace("Bearer ", "");

        return jwtUtil.extractUserId(token);
    }

    // ✅ CREATE CUSTOMER
    @PostMapping
    public Customer create(@RequestBody Customer request,
                           HttpServletRequest http) {

        Long userId = getUserId(http);

        return service.create(request, userId);
    }

    // ✅ GET ALL CUSTOMERS
    @GetMapping
    public List<Customer> getAll(HttpServletRequest http) {

        Long userId = getUserId(http);

        return service.getAllcustomersData(userId);
    }

    // ✅ GET CUSTOMER BY ID
    @GetMapping("/{id}")
    public Customer getById(@PathVariable Long id,
                           HttpServletRequest http) {

        Long userId = getUserId(http);

        return service.getCustomerById(id, userId);
    }

    // ✅ UPDATE CUSTOMER
    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id,
                           @RequestBody Customer request,
                           HttpServletRequest http) {

        Long userId = getUserId(http);

        return service.updateCustomer(id, request, userId);
    }

    // ✅ DELETE CUSTOMER
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       HttpServletRequest http) {

        Long userId = getUserId(http);

        service.deletecustomer(id, userId);
    }
}
    