package com.springbootexample.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.entity.Transaction;
import com.springbootexample.security.JwtUtil;
import com.springbootexample.service.TransactionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService service;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    // ADD TRANSACTION
    @PostMapping
    public Transaction add(@RequestBody Transaction request,
                           HttpServletRequest http) {

        return service.addTransaction(request, getUserId(http));
    }

    //  GET CUSTOMER LEDGER
    @GetMapping("/customer/{customerId}")
    public List<Transaction> getByCustomer(@PathVariable Long customerId,
                                           HttpServletRequest http) {

        return service.getCustomerTransactions(customerId, getUserId(http));
    }
    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id,
                              @RequestBody Transaction request,
                              HttpServletRequest http) {

        Long userId = getUserId(http);

        return service.updateTransaction(id, request, userId);
    }
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         HttpServletRequest request) {

        Long userId = getUserId(request);

        service.deleteTransaction(id, userId);

        return "Transaction deleted successfully";
    }
    
}