package io.mywallet.account.application;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Account is a plain CRUD entity (not event-sourced) - see the Phase 2 note on
 * {@code AuditableEntity} for why that's a deliberate, pragmatic choice distinct from
 * Order/Portfolio.
 */
@Service
public class AccountApplicationService {

    private final AccountJpaRepository accountRepository;

    public AccountApplicationService(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountEntity createAccount(UUID ownerId, AccountEntity.AccountType type, String displayName) {
        AccountEntity account = new AccountEntity(UUID.randomUUID(), ownerId, type, displayName);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<AccountEntity> listForOwner(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public AccountEntity getOwnedAccount(UUID accountId, UUID requestingUserId) {
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Account not found: " + accountId); // see isolation note in Portfolio service
        }
        return account;
    }
}
