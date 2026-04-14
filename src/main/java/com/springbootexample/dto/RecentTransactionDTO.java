package com.springbootexample.dto;

public class RecentTransactionDTO {
	private String name;
    private String type;
    private Double amount;
    private String date;
    private String description;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public Double getAmount() { return amount; }
	public void setAmount(Double amount) { this.amount = amount; }

	public String getDate() { return date; }
	public void setDate(String date) { this.date = date; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
}
