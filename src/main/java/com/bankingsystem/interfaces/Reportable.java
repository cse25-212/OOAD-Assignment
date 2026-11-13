package com.bankingsystem.interfaces;

/**
 * Contract for institutional entities capable of generating operational reports and statements.
 */
public interface Reportable {
    String generateReport();
    void exportStatements();
}
