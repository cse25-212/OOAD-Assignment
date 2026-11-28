# CSE212 — Banking System (Part B: Core Model Implementation)

Object-Oriented Analysis & Design with Java (BSc Computer Systems Engineering).
This repository implements the Banking System UML model with a JavaFX interface and SQLite persistence, ready for **GitHub Codespaces**.

The domain model, JavaFX view/controller, and JDBC DAOs are separated into `model`, `view`, `controller`, and `dao` packages.

## Quick Start in GitHub Codespaces

1. Open this folder as the root of your GitHub repository and launch a **Codespace**.
   The dev container installs JDK 25 for the VS Code Java language server, configures the Java debugger and test extensions, and installs JDK 21 as an additional runtime; Maven compiles the project for Java 21.
2. Run the unit tests:
   ```bash
   mvn test
   ```
3. Open the **Ports** tab and open port **6080** in your browser. This private port provides the Codespace desktop; the desktop password is `vscode`.
4. Launch the JavaFX banking application:
   ```bash
   mvn javafx:run
   ```
   The application initializes `banking_system.db` in the project directory and seeds sample records on first launch. The database is ignored by Git.

   Sample sign-in: `tshepang.s` / `pass1234`.

### Run from VS Code

Open `src/main/java/com/bankingsystem/view/BankingApplication.java` and click **Run** above `main`. This launcher keeps JavaFX's `Application` subclass out of the JVM entry-point class, allowing the Java extension's Maven classpath launch to start the app.

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

### Database (`com.bankingsystem.dao`)
- SQLite is initialized at application startup. `CustomerDAO`, `AccountDAO`, and `TransactionDAO` persist customer profiles, account balances, and transaction history.
- The database seeds ten sample customers and related account and transaction records the first time it is created
