package com.example.budgetingapp;

public class Budget {

    String username,budgetName,budgetEndDate;
    double budgetAmount, budgetAmountRemaining;

    public Budget(String username, String budgetName, double budgetAmount, String budgetEndDate, double budgetAmountRemaining) {
        this.username=username;
        this.budgetName=budgetName;
        this.budgetAmount=budgetAmount;
        this.budgetEndDate=budgetEndDate;
        this.budgetAmountRemaining=budgetAmountRemaining;
    }
    public String getBudgetUser() {
        return username;
    }
    public String getBudgetName() {
        return budgetName;
    }
    public Double getBudgetAmount() {
        return budgetAmount;
    }
    public String getBudgetEndDate() {
        return budgetEndDate;
    }
    public Double getBudgetAmountRemaining() {
        return budgetAmountRemaining;
    }
}
