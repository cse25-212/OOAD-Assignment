package com.bankingsystem.interfaces;

import com.bankingsystem.model.Account;

/**
 * Contract for accounts supporting fund transfers to another Account.
 */
public interface Transferable {
    boolean transfer(double amount, Account targetAccount);
}
