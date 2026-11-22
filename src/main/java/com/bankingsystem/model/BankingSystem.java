package com.bankingsystem.model;

import com.bankingsystem.interfaces.InterestPayable;
import java.time.LocalDate;
import java.util.List;

/**
 * Entry point demonstrating the Part B Core Model domain classes,
 * OOP principles, and Botswana Pula (BWP) banking rules.
 */
public class BankingSystem {
    public static void main(String[] args) {
        Bank bank = new Bank("Botswana First National Bank", "Plot 54368, CBD, Gaborone", "281467");
        Branch mainBranch = new Branch("BR-001", "Gaborone Main Mall");
        ATM mainAtm = new ATM("ATM-001", "Riverwalk Mall", 50000.00);
        Employee officer = new Employee("EMP-101", "Kabo Motsumi", "Branch Manager", 28500.00);

        officer.checkIn();
        mainBranch.assignEmployee(officer);
        bank.addBranch(mainBranch);
        bank.addATM(mainAtm);
        bank.addStaff(officer);

        Authentication auth = new Authentication("tshepang.s", "Secure2026");
        auth.login();

        IndividualCustomer customer = new IndividualCustomer(
                "CUST-001",
                "Tshepang",
                "Segobaetso",
                "tshepang@example.bw",
                "+267 71000001",
                "Block 8, Gaborone",
                auth,
                "123456789",
                LocalDate.of(2002, 5, 14)
        );
        customer.openPersonalAccount();
        bank.registerCustomer(customer);

        // 1. SavingsAccount (0.05% monthly interest, no withdrawals allowed)
        SavingsAccount savings = new SavingsAccount("SAV-1001", 2000.00, mainBranch.getBranchCode(), customer.getCustomerID());
        // 2. InvestmentAccount (5% monthly interest, minimum BWP 500.00 opening deposit)
        InvestmentAccount investment = new InvestmentAccount("INV-2001", 1000.00, mainBranch.getBranchCode(), customer.getCustomerID());
        // 3. ChequeAccount (requires verified EmploymentDetails)
        EmploymentDetails employment = new EmploymentDetails("Botswana Innovation Hub", "Plot 69184, Gaborone", true);
        ChequeAccount cheque = new ChequeAccount("CHQ-3001", 500.00, mainBranch.getBranchCode(), customer.getCustomerID(), employment);

        customer.addAccount(savings);
        customer.addAccount(investment);
        customer.addAccount(cheque);

        // Overloaded deposits & salary credit
        savings.deposit(500.00);
        investment.deposit(500.00, "Top-up Deposit");
        cheque.depositSalary(12500.00);

        // Withdrawal polymorphism
        boolean savingsWithdraw = savings.withdraw(100.00); // false
        boolean investmentWithdraw = investment.withdraw(200.00); // true

        // Transaction objects
        Transaction transferTx = new Transfer("TX-001", 300.00, cheque, savings);
        transferTx.execute();
        cheque.getTransactionHistory().addTransaction(transferTx);

        // Batch monthly interest calculation via InterestPayable polymorphism
        InterestCalculator calculator = new InterestCalculator();
        int processed = calculator.batchProcessMonthlyYield(List.of(savings, investment));

        // Card linkage & payment
        DebitCard debitCard = new DebitCard("4532-XXXX-XXXX-8821", "11/29", 412, 1234, cheque, 5000.00);
        cheque.addCard(debitCard);
        debitCard.dispenseFromATM(500.00);
        mainAtm.dispenseCash(500.00);

        System.out.println(bank.generateReport());
        System.out.println("Interest accounts processed: " + processed);
        System.out.printf("Savings withdrawal permitted? %s | Balance: BWP %.2f%n", savingsWithdraw, savings.getBalance());
        System.out.printf("Investment withdrawal permitted? %s | Balance: BWP %.2f%n", investmentWithdraw, investment.getBalance());
        System.out.printf("Cheque balance: BWP %.2f | Debit card ATM remaining limit: BWP %.2f%n",
                cheque.getBalance(), debitCard.getDailyWithdrawalLimit() - debitCard.getWithdrawnToday());
    }
}
