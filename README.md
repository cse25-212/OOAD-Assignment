# CSE212 — Banking System (Part B: Core Model Implementation)

Object-Oriented Analysis & Design with Java (BSc Computer Systems Engineering).
This repository implements the Banking System UML model with a JavaFX interface and SQLite persistence, ready for **GitHub Codespaces**.

The domain model, JavaFX view/controller, and JDBC DAOs are separated into `model`, `view`, `controller`, and `dao` packages.

## Quick Start in GitHub Codespaces

1. Generate a 32-byte key (for example, run `openssl rand -base64 32`) and add it as the `BANKING_DATA_KEY` **Codespaces secret** in the repository or account settings. Keep the key outside the repository and retain the same key for future sessions; losing it makes encrypted customer data unrecoverable.
2. Create or rebuild the **Codespace** so the secret is available in its environment.
3. Run the unit tests:
   ```bash
   mvn test
   ```
4. Launch the JavaFX banking application:
   ```bash
   mvn javafx:run
   ```
   The application initializes `banking_system.db` in the project directory and seeds sample records on first launch. Legacy plaintext databases are backed up beside the database before migration. Both the database and migration backups are ignored by Git.

   Sample sign-in: `tshepang.s` / `pass1234`.

### Open the JavaFX desktop in Codespaces

The Codespace provides a lightweight desktop through noVNC. Forwarded port `6080` opens in a browser; click **Connect** and enter the desktop password `vscode`. Start the JavaFX application from the Codespace terminal with `mvn javafx:run`; its window appears in that desktop. The Codespaces forwarded port remains access-controlled by GitHub.

### Data protection and migration

- Passwords are stored as salted PBKDF2 hashes. Customer contact/identity details and employment information are encrypted with AES-GCM; email uniqueness uses a keyed digest.
- Set `BANKING_DATA_KEY` to a base64-encoded 32-byte secret before launching. In Codespaces, configure it as a Codespaces secret; for local development, set it in the shell or a secret manager. Never commit or rotate the key without first planning a data migration.
- On the first startup against a legacy database, the app creates a `.before-security-<timestamp>.bak` backup, migrates existing credentials and PII, and preserves existing records and demo sign-ins. Keep the backup until the migrated database has been verified.
- The test suite injects a test-only key and does not require the production secret.

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
- The database seeds ten sample customers and related account and transaction records the first time it is created.
