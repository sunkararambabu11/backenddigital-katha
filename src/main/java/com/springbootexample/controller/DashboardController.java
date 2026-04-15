package com.springbootexample.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.dto.DashboardDTO;
import com.springbootexample.dto.DashboardSummaryDTO;
import com.springbootexample.dto.RecentTransactionDTO;
import com.springbootexample.dto.TopDebtorDTO;
import com.springbootexample.security.JwtUtil;
import com.springbootexample.service.DashboardService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService service;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = header.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    // Combined summary — single API call for the entire dashboard
    @GetMapping("/summary")
    public DashboardSummaryDTO summary(HttpServletRequest request) {
        return service.getFullSummary(getUserId(request));
    }

    // Original basic stats (kept for backward compatibility)
    @GetMapping
    public DashboardDTO dashboard(HttpServletRequest request) {
        return service.getDashboard(getUserId(request));
    }

    // Top debtors
    @GetMapping("/top-debtors")
    public List<TopDebtorDTO> topDebtors(HttpServletRequest request) {
        return service.getTopDebtors(getUserId(request));
    }

    // Recent transactions
    @GetMapping("/recent")
    public List<RecentTransactionDTO> recent(HttpServletRequest request) {
        return service.getRecentTransactions(getUserId(request));
    }

    // Monthly chart
    @GetMapping("/monthly")
    public Map<String, Double> monthly(HttpServletRequest request) {
        return service.getMonthlyReport(getUserId(request));
    }
}
