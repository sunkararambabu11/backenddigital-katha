package com.springbootexample.dto;

import java.util.List;
import java.util.Map;

public class DashboardSummaryDTO {

    // Core stats
    private long totalCustomers;
    private long activeCustomers;    // customers with balance > 0
    private double totalDebit;
    private double totalCredit;
    private double totalOutstanding;

    // Today's activity
    private long todayTransactionCount;
    private double todayDebit;
    private double todayCredit;

    // Lists
    private List<TopDebtorDTO> topDebtors;
    private List<RecentTransactionDTO> recentTransactions;

    // Monthly chart data
    private Map<String, Double> monthlyData;

    // Getters and setters

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getActiveCustomers() { return activeCustomers; }
    public void setActiveCustomers(long activeCustomers) { this.activeCustomers = activeCustomers; }

    public double getTotalDebit() { return totalDebit; }
    public void setTotalDebit(double totalDebit) { this.totalDebit = totalDebit; }

    public double getTotalCredit() { return totalCredit; }
    public void setTotalCredit(double totalCredit) { this.totalCredit = totalCredit; }

    public double getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(double totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public long getTodayTransactionCount() { return todayTransactionCount; }
    public void setTodayTransactionCount(long todayTransactionCount) { this.todayTransactionCount = todayTransactionCount; }

    public double getTodayDebit() { return todayDebit; }
    public void setTodayDebit(double todayDebit) { this.todayDebit = todayDebit; }

    public double getTodayCredit() { return todayCredit; }
    public void setTodayCredit(double todayCredit) { this.todayCredit = todayCredit; }

    public List<TopDebtorDTO> getTopDebtors() { return topDebtors; }
    public void setTopDebtors(List<TopDebtorDTO> topDebtors) { this.topDebtors = topDebtors; }

    public List<RecentTransactionDTO> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<RecentTransactionDTO> recentTransactions) { this.recentTransactions = recentTransactions; }

    public Map<String, Double> getMonthlyData() { return monthlyData; }
    public void setMonthlyData(Map<String, Double> monthlyData) { this.monthlyData = monthlyData; }
}
