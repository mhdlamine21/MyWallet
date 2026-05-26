package io.mywallet.account.interfaces.rest;

import io.mywallet.account.application.AccountApplicationService;
import io.mywallet.account.interfaces.rest.dto.AccountResponse;
import io.mywallet.account.interfaces.rest.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public class AccountController {

    private final AccountApplicationService accountApplicationService;

    public AccountController(AccountApplicationService accountApplicationService) {
        this.accountApplicationService = accountApplicationService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request, Authentication authentication) {
        var account = accountApplicationService.createAccount(userId(authentication), request.type(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping
    public List<AccountResponse> list(Authentication authentication) {
        return accountApplicationService.listForOwner(userId(authentication)).stream()
            .map(AccountResponse::from)
            .toList();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
