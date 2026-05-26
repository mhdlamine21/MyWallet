package io.mywallet.account.interfaces.rest.dto;

import io.mywallet.account.infrastructure.persistence.AccountEntity;

import java.util.UUID;

public record AccountResponse(
    UUID id,
    String accountType,
    String displayName
) {
    public static AccountResponse from(AccountEntity entity) {
        return new AccountResponse(entity.getId(), entity.getAccountType().name(), entity.getDisplayName());
    }
}
