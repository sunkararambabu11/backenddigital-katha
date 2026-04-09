package com.springbootexample.dto;

public class DashboardDTO {
 private long totalCustomers;
 private  double totalDebit;
 public long getTotalCustomers() {
	return totalCustomers;
}
public void setTotalCustomers(long totalCustomers) {
	this.totalCustomers = totalCustomers;
}
public double getTotalDebit() {
	return totalDebit;
}
public void setTotalDebit(double totalDebit) {
	this.totalDebit = totalDebit;
}
public double getTotalCredit() {
	return totalCredit;
}
public void setTotalCredit(double totalCredit) {
	this.totalCredit = totalCredit;
}
public double getTotalOutstanding() {
	return totalOutstanding;
}
public void setTotalOutstanding(double totalOutstanding) {
	this.totalOutstanding = totalOutstanding;
}
private double totalCredit;
 private double totalOutstanding;
 
}
