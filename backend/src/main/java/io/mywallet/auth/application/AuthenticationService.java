package io.mywallet.auth.application;

import io.mywallet.auth.application.exception.EmailAlreadyInUseException;
import io.mywallet.auth.application.exception.InvalidRefreshTokenException;
import io.mywallet.auth.infrastructure.persistence.RefreshTokenEntity;
import io.mywallet.auth.infrastructure.persistence.RefreshTokenJpaRepository;
import io.mywallet.auth.infrastructure.security.JwtTokenProvider;
import io.mywallet.auth.infrastructure.security.RefreshTokenCrypto;
import io.mywallet.auth.interfaces.rest.dto.AuthResponse;
import io.mywallet.user.infrastructure.persistence.RoleEntity;
import io.mywallet.user.infrastructure.persistence.RoleJpaRepository;
import io.mywallet.user.infrastructure.persistence.UserEntity;
import io.mywallet.user.infrastructure.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    /**
     * A password nobody could ever have chosen (it's not a valid BCrypt-encodable
     * plaintext match target in practice) - used only to run a BCrypt comparison against
     * when the email doesn't exist, so login takes roughly the same time either way and an
     * attacker can't distinguish "wrong password" from "no such account" by timing. This
     * is the "protection contre l'énumération des utilisateurs" requirement from the brief.
     */
    private static final String DUMMY_PASSWORD_HASH =
        "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5V2p1Vh3vhZ3z1Y9Z3z1Y9Z3z1Y9O";

    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenCrypto refreshTokenCrypto;
    private final long refreshTokenTtlSeconds;

    public AuthenticationService(
        UserJpaRepository userRepository,
        RoleJpaRepository roleRepository,
        RefreshTokenJpaRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        RefreshTokenCrypto refreshTokenCrypto,
        @Value("${mywallet.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenCrypto = refreshTokenCrypto;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException();
        }

        UserEntity user = new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode(rawPassword));
        RoleEntity investorRole = roleRepository.findByName("INVESTOR")
            .orElseThrow(() -> new IllegalStateException("INVESTOR role missing - check V1__init_schema.sql seed data"));
        user.assignRole(investorRole);
        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Run a BCrypt comparison anyway against the dummy hash, so this branch takes
            // about as long as the "user exists but password is wrong" branch below.
            passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
            throw new BadCredentialsException("Invalid email or password");
        }

        Instant now = Instant.now();
        if (user.isLocked(now)) {
            // Deliberately the exact same generic message as every other failure case -
            // revealing "this account is locked" would confirm the email exists and that
            // an attacker's brute-force attempts landed, which is exactly the information
            // an account-enumeration/lockout-probing attack wants. This does mean a locked
            // account's login attempt skips the BCrypt comparison entirely, a small timing
            // difference versus the "wrong password" branch below - an accepted trade-off,
            // not a perfect one (see known-limitations.md).
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            if (user.isEnabled()) {
                user.recordFailedLogin(now);
                userRepository.save(user);
            }
            throw new BadCredentialsException("Invalid email or password");
        }

        user.recordSuccessfulLogin();
        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String tokenHash = refreshTokenCrypto.hash(rawRefreshToken);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (token.getRevokedAt() != null) {
            // This exact token was already rotated away once before - presenting it again
            // means either a client bug (retry after already refreshing) or theft. Either
            // way, the safe move is to revoke every active token for this user, forcing
            // re-authentication, rather than trust it.
            revokeAllActiveTokensForUser(token.getUserId());
            throw new InvalidRefreshTokenException();
        }

        if (!token.isActive(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        UserEntity user = userRepository.findById(token.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);

        AuthResponse response = issueTokenPair(user);

        // Rotate: mark the presented token as spent, linked to whichever new token replaces it.
        RefreshTokenEntity newest = refreshTokenRepository.findByTokenHash(refreshTokenCrypto.hash(response.refreshToken()))
            .orElseThrow();
        token.revoke(newest.getId());
        refreshTokenRepository.save(token);

        return response;
    }

    private AuthResponse issueTokenPair(UserEntity user) {
        List<String> roleNames = user.getRoles().stream().map(RoleEntity::getName).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), roleNames);

        String rawRefreshToken = refreshTokenCrypto.generateRawToken();
        Instant now = Instant.now();
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity(
            UUID.randomUUID(),
            user.getId(),
            refreshTokenCrypto.hash(rawRefreshToken),
            now,
            now.plus(refreshTokenTtlSeconds, ChronoUnit.SECONDS)
        );
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(accessToken, jwtTokenProvider.accessTokenTtlSeconds(), rawRefreshToken);
    }

    private void revokeAllActiveTokensForUser(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
            .forEach(t -> {
                t.revoke(null);
                refreshTokenRepository.save(t);
            });
    }
}
