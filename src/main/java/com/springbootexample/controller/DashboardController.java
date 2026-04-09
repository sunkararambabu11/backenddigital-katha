package com.springbootexample.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.dto.DashboardDTO;
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
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    // 🔥 Main summary
    @GetMapping
    public DashboardDTO dashboard(HttpServletRequest request) {
        return service.getDashboard(getUserId(request));
    }

    // 🔥 Top debtors
    @GetMapping("/top-debtors")
    public List<TopDebtorDTO> topDebtors(HttpServletRequest request) {
        return service.getTopDebtors(getUserId(request));
    }

    // 🔥 Recent transactions
    @GetMapping("/recent")
    public List<RecentTransactionDTO> recent(HttpServletRequest request) {
        return service.getRecentTransactions(getUserId(request));
    }

    // 🔥 Monthly chart
    @GetMapping("/monthly")
    public Map<String, Double> monthly(HttpServletRequest request) {
        return service.getMonthlyReport(getUserId(request));
    }
}