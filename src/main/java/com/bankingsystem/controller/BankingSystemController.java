package com.bankingsystem.controller;

import com.bankingsystem.dao.AccountDAO;
import com.bankingsystem.dao.CustomerDAO;
import com.bankingsystem.dao.TransactionDAO;
import com.bankingsystem.interfaces.InterestPayable;
import com.bankingsystem.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate
import java.util.*;

public class BankingSystemController {
    @FXML private Label sessionLabel, loginFeedbackLabel, regFeedbackLabel, totalBalanceLabel, accountStatusLabel;
    @FXML private TabPane mainTabPane;
    @FXML private Tab dashboardTab;
    @FXML private TextField loginUsernameField, loginPasswordField;
    @FXML private ComboBox<String> regTypeCombo, newAccountTypeCombo, branchCombo;
    @FXML private TextField regFirstNameField, regSurnameField, regEmailField, regPhoneField, regAddressField;
    @FXML private TextField regUsernameField, regPasswordField, regNationalIdField;
    @FXML private DatePicker regDobPicker;
    @FXML private TextField regCompanyNameField, regCompanyAddressField, regNumberField;
    @FXML private TextField txAmountField, txReferenceField, targetAccountField, openingDepositField;
    @FXML private TextField employerNameField, employerAddressField;
    @FXML private CheckBox employedCheckBox;
    @FXML private ListView<Account> accountListView;
    @FXML private ListView<String> transactionListView;
    @FXML private Accordion actionAccordion;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private Customer activeCustomer;
    private int customerSequence = 1;

    @FXML
    public void initialize() {
        regTypeCombo.getItems().setAll("IndividualCustomer", "CorporateCustomer");
        regTypeCombo.getSelectionModel().selectFirst();
        newAccountTypeCombo.getItems().setAll("SavingsAccount", "InvestmentAccount", "ChequeAccount");
        newAccountTypeCombo.getSelectionModel().selectFirst();
        branchCombo.getItems().setAll("BR-001", "BR-002");
        branchCombo.getSelectionModel().selectFirst();
        regDobPicker.setValue(LocalDate.of(2002, 5, 14));
        handleRegTypeChanged();

        accountListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, account) -> refreshTransactionList(account));

        try {
            customerSequence = customerDAO.nextCustomerSequence();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read the next customer ID from the database.", exception);
        }
        refreshPortfolio();
    }

    @FXML
    public void goToLoginTab() {
        mainTabPane.getSelectionModel().select(0);
    }

    @FXML
    public void goToRegisterTab() {
        mainTabPane.getSelectionModel().select(1);
    }

    @FXML
    public void goToDashboardTab() {
        if (activeCustomer == null) {
            loginFeedbackLabel.setText("Please sign in before opening the ledger.");
            goToLoginTab();
            return;
        }
        mainTabPane.getSelectionModel().select(2);
    }

    @FXML
    public void handleLogin() {
        String username = value(loginUsernameField);
        String password = value(loginPasswordField);
        try {
            Optional<Customer> result = customerDAO.authenticate(username, password);
            if (result.isPresent()) {
                Customer customer = result.get();
                for (Account account : accountDAO.findByCustomerId(customer.getCustomerID())) {
                    customer.addAccount(account);
                }
                activeCustomer = customer;
                sessionLabel.setText("Signed in · " + customer.getFirstName() + " " + customer.getSurname());
                loginFeedbackLabel.setText("Sign-in successful.");
                refreshPortfolio();
                goToDashboardTab();
            } else {
                loginFeedbackLabel.setText("Invalid username or password.");
            }
        } catch (SQLException exception) {
            loginFeedbackLabel.setText("Could not sign in: " + exception.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        if (activeCustomer != null) {
            activeCustomer.getAuthentication().logout();
        }
        activeCustomer = null;
        sessionLabel.setText("Guest Session · Not Signed In");
        loginPasswordField.clear();
        loginFeedbackLabel.setText("Session ended.");
        goToLoginTab();
    }

    @FXML
    public void handleRegTypeChanged() {
        boolean individual = "IndividualCustomer".equals(regTypeCombo.getValue());
        regNationalIdField.setDisable(!individual);
        regDobPicker.setDisable(!individual);
        regCompanyNameField.setDisable(individual);
        regCompanyAddressField.setDisable(individual);
        regNumberField.setDisable(individual);
    }

    @FXML
    public void handleRegister() {
        try {
            String password = value(regPasswordField);
            Authentication auth = new Authentication(value(regUsernameField), password);
            String customerId = "CUST-" + String.format("%03d", customerSequence);
            Customer customer;
            if ("CorporateCustomer".equals(regTypeCombo.getValue())) {
                customer = new CorporateCustomer(
                        customerId, value(regFirstNameField), value(regSurnameField), value(regEmailField),
                        value(regPhoneField), value(regAddressField), auth,
                        value(regCompanyNameField), value(regCompanyAddressField), value(regNumberField));
                ((CorporateCustomer) customer).openBusinessAccount();
            } else {
                LocalDate dob = regDobPicker.getValue();
                customer = new IndividualCustomer(
                        customerId, value(regFirstNameField), value(regSurnameField), value(regEmailField),
                        value(regPhoneField), value(regAddressField), auth, value(regNationalIdField), dob);
                ((IndividualCustomer) customer).openPersonalAccount();
            }
            customerDAO.create(customer, password);
            customerSequence++;
            regFeedbackLabel.setText("Profile saved. Customer ID: " + customerId);
            clearRegistrationFields();
        } catch (SQLException | RuntimeException exception) {
            regFeedbackLabel.setText(exception.getMessage());
        }
    }

    @FXML
    public void handleDeposit() {
        Account account = selectedAccount();
        if (account == null) return;
        try {
            double amount = parseAmount(txAmountField);
            String reference = optionalReference(txReferenceField);
            account.deposit(amount, reference);
            persist(account, "DEPOSIT", amount, reference);
            setAccountStatus("Deposit completed.", true);
            refreshPortfolio();
        } catch (SQLException | RuntimeException exception) {
            setAccountStatus(exception.getMessage(), false);
        }
    }

    @FXML
    public void handleWithdraw() {
        Account account = selectedAccount();
        if (account == null) return;
        try {
            double amount = parseAmount(txAmountField);
            boolean withdrawn = account.withdraw(amount);
            if (withdrawn) {
                persist(account, "WITHDRAWAL", amount, optionalReference(txReferenceField));
            }
            setAccountStatus(withdrawn ? "Withdrawal completed." : "Withdrawal was not permitted or exceeds the balance.", withdrawn);
            refreshPortfolio();
        } catch (SQLException | RuntimeException exception) {
            setAccountStatus(exception.getMessage(), false);
        }
    }

    @FXML
    public void handleTransfer() {
        Account source = selectedAccount();
        if (source == null) return;
        Account target = findAccount(value(targetAccountField));
        if (target == null) {
            setAccountStatus("Enter a valid target account number.", false);
            return;
        }
        try {
            double amount = parseAmount(txAmountField);
            boolean transferred = source.transfer(amount, target);
            if (transferred) {
                transactionDAO.persistTransfer(source, target, nextTransactionId(), amount,
                        optionalReference(txReferenceField));
            }
            setAccountStatus(transferred ? "Transfer completed." : "Transfer was not permitted.", transferred);
            refreshPortfolio();
        } catch (SQLException | RuntimeException exception) {
            setAccountStatus(exception.getMessage(), false);
        }
    }

    @FXML
    public void handleDepositSalary() {
        Account account = selectedAccount();
        if (!(account instanceof ChequeAccount cheque)) {
            setAccountStatus("Salary credit is available only for a ChequeAccount.", false);
            return;
        }
        try {
            double amount = parseAmount(txAmountField);
            cheque.depositSalary(amount);
            persist(cheque, "SALARY", amount, "Salary Credit");
            setAccountStatus("Salary credit completed.", true);
            refreshPortfolio();
        } catch (SQLException | RuntimeException exception) {
            setAccountStatus(exception.getMessage(), false);
        }
    }

    @FXML
    public void handleOpenAccount() {
        if (activeCustomer == null) {
            setAccountStatus("Sign in before opening an account.", false);
            return;
        }
        try {
            double openingDeposit = Double.parseDouble(value(openingDepositField));
            String branchCode = value(branchCombo);
            String accountNumber = nextAccountNumber();
            Account account;
            switch (value(newAccountTypeCombo)) {
                case "InvestmentAccount":
                    account = new InvestmentAccount(accountNumber, openingDeposit, branchCode, activeCustomer.getCustomerID());
                    break;
                case "ChequeAccount":
                    EmploymentDetails employment = new EmploymentDetails(
                            value(employerNameField), value(employerAddressField), employedCheckBox.isSelected());
                    account = new ChequeAccount(accountNumber, openingDeposit, branchCode, activeCustomer.getCustomerID(), employment);
                    break;
                default:
                    account = new SavingsAccount(accountNumber, openingDeposit, branchCode, activeCustomer.getCustomerID());
            }
            accountDAO.create(account);
            activeCustomer.addAccount(account);
            setAccountStatus(account.getAccountType() + " created: " + accountNumber, true);
            refreshPortfolio();
        } catch (SQLException | RuntimeException exception) {
            setAccountStatus(exception.getMessage(), false);
        }
    }

    @FXML
    public void handleApplyMonthlyInterest() {
        if (activeCustomer == null) {
            setAccountStatus("Sign in before running interest.", false);
            return;
        }
        List<InterestPayable> interestAccounts = new ArrayList<>();
        for (Account account : activeCustomer.getAccounts()) {
            if (account instanceof InterestPayable payable) interestAccounts.add(payable);
        }
        Map<Account, Double> oldBalances = new LinkedHashMap<>();
        for (Account account : activeCustomer.getAccounts()) {
            if (account instanceof InterestPayable) oldBalances.put(account, account.getBalance());
        }
        int processed = new InterestCalculator().batchProcessMonthlyYield(interestAccounts);
        try {
            for (Map.Entry<Account, Double> entry : oldBalances.entrySet()) {
                Account account = entry.getKey();
                double interest = account.getBalance() - entry.getValue();
                if (interest > 0) {
                    persist(account, "INTEREST", interest, "Monthly interest");
                }
            }
        } catch (SQLException exception) {
            setAccountStatus("Interest was applied but could not be fully saved: " + exception.getMessage(), false);
            refreshPortfolio();
            return;
        }
        setAccountStatus("Monthly interest batch applied to " + processed + " account(s).", true);
        refreshPortfolio();
    }

    private Account selectedAccount() {
        Account account = accountListView.getSelectionModel().getSelectedItem();
        if (account == null) {
            setAccountStatus("Select an account from Held Accounts.", false);
        }
        return account;
    }

    private Account findAccount(String accountNumber) {
        if (activeCustomer == null) return null;
        for (Account account : activeCustomer.getAccounts()) {
            if (account.getAccountNumber().equalsIgnoreCase(accountNumber)) return account;
        }
        return null;
    }

    private String nextAccountNumber() throws SQLException {
        String prefix = value(newAccountTypeCombo).startsWith("Savings") ? "SAV"
                : value(newAccountTypeCombo).startsWith("Investment") ? "INV" : "CHQ";
        return accountDAO.nextAccountNumber(prefix);
    }

    private void refreshPortfolio() {
        accountListView.getItems().clear();
        transactionListView.getItems().clear();
        if (activeCustomer == null) {
            totalBalanceLabel.setText("BWP 0.00");
            return;
        }
        accountListView.getItems().addAll(activeCustomer.getAccounts());
        double total = activeCustomer.getAccounts().stream().mapToDouble(Account::getBalance).sum();
        totalBalanceLabel.setText(String.format("BWP %.2f", total));
        if (!accountListView.getItems().isEmpty()) {
            accountListView.getSelectionModel().selectFirst();
        }
    }

    private void refreshTransactionList(Account account) {
        transactionListView.getItems().clear();
        if (account == null) return;
        transactionListView.getItems().addAll(account.getTransactionHistory().getAuditHistory());
        try {
            for (TransactionDAO.TransactionRecord record
                    : transactionDAO.findByAccountNumber(account.getAccountNumber())) {
                transactionListView.getItems().add(String.format(
                        "%s · BWP %.2f · %s · %s",
                        record.txType(), record.amount(), record.timestamp(),
                        record.reference() == null ? "" : record.reference()));
            }
        } catch (SQLException exception) {
            setAccountStatus("Could not load transaction history: " + exception.getMessage(), false);
        }
    }

    private void persist(Account account, String type, double amount, String reference) throws SQLException {
        transactionDAO.persistBalanceAndTransaction(
                account, nextTransactionId(), type, amount, reference);
    }

    private String nextTransactionId() {
        return "TX-" + UUID.randomUUID();
    }

    private void setAccountStatus(String message, boolean success) {
        accountStatusLabel.setText(message);
        accountStatusLabel.setStyle(success
                ? "-fx-text-fill: #047857; -fx-font-weight: bold;"
                : "-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
    }

    private void clearRegistrationFields() {
        regFirstNameField.clear();
        regSurnameField.clear();
        regEmailField.clear();
        regPhoneField.clear();
        regAddressField.clear();
        regUsernameField.clear();
        regPasswordField.clear();
        regNationalIdField.clear();
        regCompanyNameField.clear();
        regCompanyAddressField.clear();
        regNumberField.clear();
    }

    private double parseAmount(TextField field) {
        double amount = Double.parseDouble(value(field));
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        return amount;
    }

    private String optionalReference(TextField field) {
        String reference = value(field);
        return reference.isBlank() ? "Standard Deposit" : reference;
    }

    private String value(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String value(ComboBox<String> combo) {
        return combo.getValue() == null ? "" : combo.getValue();
    }
}
