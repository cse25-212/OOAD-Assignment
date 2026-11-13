package com.bankingsystem;

import com.bankingsystem.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoreModelTest {

    @Test
    void investmentAccountRequiresMinimumOpeningDepositOf500() {
        assertThrows(IllegalArgumentException.class, () ->
                new InvestmentAccount("INV-FAIL", 499.99, "BR-001", "CUST-001")
        );

        InvestmentAccount valid = new InvestmentAccount("INV-OK", 500.00, "BR-001", "CUST-001");
        assertEquals(500.00, valid.getBalance(), 0.001);
    }

    @Test
    void savingsAccountDisallowsWithdrawalsAndPaysPointZeroFivePercentMonthlyInterest() {
        SavingsAccount savings = new SavingsAccount("SAV-001", 10000.00, "BR-001", "CUST-001");
        assertFalse(savings.withdraw(100.00));
        assertEquals(10000.00, savings.getBalance(), 0.001);

        double interest = savings.applyInterest();
        assertEquals(5.00, interest, 0.001);
        assertEquals(10005.00, savings.getBalance(), 0.001);
    }

    @Test
    void investmentAccountPaysFivePercentMonthlyInterestAndAllowsWithdrawals() {
        InvestmentAccount inv = new InvestmentAccount("INV-001", 1000.00, "BR-001", "CUST-001");
        double interest = inv.applyInterest();
        assertEquals(50.00, interest, 0.001);
        assertEquals(1050.00, inv.getBalance(), 0.001);

        assertTrue(inv.withdraw(250.00));
        assertEquals(800.00, inv.getBalance(), 0.001);
    }

    @Test
    void chequeAccountRequiresVerifiedEmploymentDetails() {
        EmploymentDetails unemployed = new EmploymentDetails("", "", false);
        assertThrows(IllegalArgumentException.class, () ->
                new ChequeAccount("CHQ-FAIL", 0.0, "BR-001", "CUST-001", unemployed)
        );

        EmploymentDetails employed = new EmploymentDetails("BAC Tech", "Gaborone", true);
        ChequeAccount cheque = new ChequeAccount("CHQ-OK", 100.0, "BR-001", "CUST-001", employed);
        cheque.depositSalary(5000.00);
        assertEquals(5100.00, cheque.getBalance(), 0.001);
        assertTrue(cheque.withdraw(1100.00));
        assertEquals(4000.00, cheque.getBalance(), 0.001);
    }

    @Test
    void customerCanHoldAllThreeAccountTypesAndBatchInterestRunsPolymorphically() {
        Authentication auth = new Authentication("kabo.m", "pass1234");
        assertTrue(auth.login());

        IndividualCustomer customer = new IndividualCustomer(
                "CUST-002", "Kabo", "Molefe", "kabo@bw.org", "+26771111111",
                "Phakalane", auth, "987654321", LocalDate.of(1998, 3, 22)
        );

        SavingsAccount sav = new SavingsAccount("SAV-10", 2000.00, "BR-001", "CUST-002");
        InvestmentAccount inv = new InvestmentAccount("INV-10", 2000.00, "BR-001", "CUST-002");
        ChequeAccount chq = new ChequeAccount("CHQ-10", 1000.00, "BR-001", "CUST-002",
                new EmploymentDetails("Debswana", "Orapa", true));

        customer.addAccount(sav);
        customer.addAccount(inv);
        customer.addAccount(chq);
        assertEquals(3, customer.getAccounts().size());

        InterestCalculator calc = new InterestCalculator();
        int count = calc.batchProcessMonthlyYield(List.of(sav, inv));
        assertEquals(2, count);
        assertEquals(2001.00, sav.getBalance(), 0.001);
        assertEquals(2100.00, inv.getBalance(), 0.001);
    }
}
