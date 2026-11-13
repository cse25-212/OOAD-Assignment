# CSE202 — Banking System (Part B: Core Model Implementation)

Object-Oriented Analysis & Design with Java (BSc Computer Systems Engineering).
This repository implements **Part B, Section 4 (Implementation of Core Model)** directly from the Banking System UML Class Diagram, ready for **GitHub Codespaces**.

> **Scope Note (Week 4 Core Model):** As specified in Part B of the assignment, this stage implements the domain entities, interfaces, attributes, and business logic only—with strict separation from JavaFX GUI views (Week 5), controllers (Week 6), and JDBC persistence (Week 7).

## Quick Start in GitHub Codespaces

1. Open this folder as the root of your GitHub repository and launch a **Codespace**.
2. Run the unit tests to verify all OOP rules and banking constraints:
   ```bash
   mvn test
   ```
3. Run the core model demonstration program (`BankingSystem.main`):
   ```bash
   mvn compile exec:java
   ```

## Core Model Architecture

### Interfaces (`com.bankingsystem.interfaces`)
- `Authenticatable` — `login()`, `logout()`, `validateCredentials()`
- `Auditable` — `logAudit(String)`, `getAuditHistory()`
- `Depositable` — `deposit(double)`, `deposit(double, String)` (overloading)
- `InterestPayable` — `calculateMonthlyInterest()`, `applyInterest()`
- `Payable` — `makePayment(double)`, `verifyPIN(int)`
- `Reportable` — `generateReport()`, `exportStatements()`
- `Transferable` — `transfer(double, Account)`
- `Verifiable` — `verifyDetails()`, `getVerificationStatus()`
- `Withdrawable` — `withdraw(double)`

### Domain Classes (`com.bankingsystem.model`)
- **Organization**: `Bank`, `Branch`, `ATM`, `Employee`
- **Customers & Security**: `Customer` (abstract), `IndividualCustomer`, `CorporateCustomer`, `Authentication`, `EmploymentDetails`
- **Accounts & Interest**: `Account` (abstract), `SavingsAccount` (0.05% monthly interest, no withdrawals), `InvestmentAccount` (5% monthly interest, min BWP 500.00 opening deposit), `ChequeAccount` (salary credits, requires verified `EmploymentDetails`), `InterestCalculator`
- **Transactions & Audit**: `TransactionHistory`, `Transaction` (abstract), `Deposit`, `Withdrawal`, `Transfer`
- **Cards**: `Card` (abstract), `DebitCard`, `CreditCard`
- **Entry Point**: `BankingSystem`
