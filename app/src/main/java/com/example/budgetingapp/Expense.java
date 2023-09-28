package com.example.budgetingapp;

public class Expense {

    String username,budgetName,expenseDesc, expenseDate, id;
    double expenseAmount;

    public Expense(String username, String budgetName, String expenseDesc, double expenseAmount, String expenseDate, String id) {
        this.username=username;
        this.budgetName=budgetName;
        this.expenseDesc=expenseDesc;
        this.expenseAmount=expenseAmount;
        this.expenseDate=expenseDate;
        this.id=id;
    }
    public String getExpenseBudgetName() {
        return budgetName;
    }
    public String getExpenseDesc() {
        return expenseDesc;
    }
    public Double getExpenseAmount() {
        return expenseAmount;
    }
    public String getExpenseDate() {
        return expenseDate;
    }
    public String getId() {
        return id;
    }
}
