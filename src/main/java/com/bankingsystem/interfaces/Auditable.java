package com.bankingsystem.interfaces;

import java.util.List;

/**
 * Contract for recording and retrieving immutable audit trail entries.
 */
public interface Auditable {
    void logAudit(String action);
    List<String> getAuditHistory();
}
