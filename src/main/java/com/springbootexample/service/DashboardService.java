package com.springbootexample.service;

import java.util.List;
import java.util.Map;

import com.springbootexample.dto.DashboardDTO;
import com.springbootexample.dto.RecentTransactionDTO;
import com.springbootexample.dto.TopDebtorDTO;

public interface DashboardService {
    DashboardDTO getDashboard(Long userId);

    List<TopDebtorDTO> getTopDebtors(Long userId);
   

    List<RecentTransactionDTO> getRecentTransactions(Long userId);

    Map<String, Double> getMonthlyReport(Long userId);

}
