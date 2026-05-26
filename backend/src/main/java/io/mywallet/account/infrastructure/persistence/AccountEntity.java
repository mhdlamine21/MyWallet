package io.mywallet.account.infrastructure.persistence;

import io.mywallet.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity extends AuditableEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected AccountEntity() {
        // JPA
    }

    public AccountEntity(UUID id, UUID ownerId, AccountType accountType, String displayName) {
        super(id);
        this.ownerId = ownerId;
        this.accountType = accountType;
        this.displayName = displayName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public enum AccountType {
        REAL, SIMULATED, DEMO
    }
}
