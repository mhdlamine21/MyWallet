package io.mywallet.account.interfaces.rest.dto;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotNull AccountEntity.AccountType type,
    @NotBlank String displayName
) {}
