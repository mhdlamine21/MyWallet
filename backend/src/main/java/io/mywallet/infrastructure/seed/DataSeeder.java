package io.mywallet.infrastructure.seed;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.portfolio.domain.model.Portfolio;
import io.mywallet.portfolio.domain.model.PortfolioMode;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.domain.StrategyMode;
import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import io.mywallet.user.infrastructure.persistence.RoleEntity;
import io.mywallet.user.infrastructure.persistence.RoleJpaRepository;
import io.mywallet.user.infrastructure.persistence.UserEntity;
import io.mywallet.user.infrastructure.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds demo data so the app is immediately explorable - this is what backs the "Try a
 * demo account" buttons on the login screen and what the public cloud demo relies on
 * (see the project plan's decision: pre-seeded demo accounts, no open sign-up on the
 * public instance).
 *
 * <p>Gated behind the {@code seed} or {@code demo} profile - never runs against a
 * production-intent database by accident. Idempotent: checks for existing rows before
 * inserting, so restarting the app in this profile doesn't duplicate demo data (the
 * periodic demo-reset job, added when the public demo is deployed, is expected to
 * truncate the relevant tables and let this seeder repopulate them from a clean slate).</p>
 */
@Component
@Profile({"seed", "demo"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "demo-password-not-for-real-use";

    private final UserJpaRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final AccountJpaRepository accountRepository;
    private final AssetJpaRepository assetRepository;
    private final PortfolioProjectionJpaRepository portfolioProjectionRepository;
    private final EventStoreRepository<Portfolio> portfolioEventStoreRepository;
    private final StrategyJpaRepository strategyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
        UserJpaRepository userRepository,
        RoleJpaRepository roleRepository,
        AccountJpaRepository accountRepository,
        AssetJpaRepository assetRepository,
        PortfolioProjectionJpaRepository portfolioProjectionRepository,
        EventStoreRepository<Portfolio> portfolioEventStoreRepository,
        StrategyJpaRepository strategyRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.portfolioProjectionRepository = portfolioProjectionRepository;
        this.portfolioEventStoreRepository = portfolioEventStoreRepository;
        this.strategyRepository = strategyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running DataSeeder - populating demo accounts, assets, and a sample strategy");

        List<AssetEntity> assets = seedAssets();
        UserEntity investor = seedUser("demo.investor@mywallet.dev", "INVESTOR");
        seedUser("demo.trader@mywallet.dev", "TRADER");
        seedUser("demo.analyst@mywallet.dev", "ANALYST");
        seedUser("demo.admin@mywallet.dev", "ADMIN");
        seedUser("demo.auditor@mywallet.dev", "AUDITOR");

        AccountEntity account = seedAccount(investor);
        UUID portfolioId = seedPortfolio(account);
        seedStrategy(investor, portfolioId, assets);

        log.info("DataSeeder complete - demo login: demo.investor@mywallet.dev (see README.md for the shared demo password)");
    }

    private List<AssetEntity> seedAssets() {
        record Spec(String symbol, AssetEntity.AssetClass assetClass, String currency, String name,
                    String initialPrice, String drift, String volatility, long seed) {}

        List<Spec> specs = List.of(
            new Spec("BTCUSDT", AssetEntity.AssetClass.CRYPTO, "USD", "Bitcoin (simulated)", "60000.00", "0.10", "0.60", 1001L),
            new Spec("ETHUSDT", AssetEntity.AssetClass.CRYPTO, "USD", "Ethereum (simulated)", "3000.00", "0.08", "0.65", 1002L),
            new Spec("AAPL", AssetEntity.AssetClass.STOCK, "USD", "Apple Inc. (simulated)", "190.00", "0.06", "0.25", 1003L),
            new Spec("EURUSD", AssetEntity.AssetClass.CURRENCY, "USD", "Euro / US Dollar (simulated)", "1.08", "0.00", "0.08", 1004L)
        );

        return specs.stream().map(spec -> assetRepository.findBySymbol(spec.symbol())
            .orElseGet(() -> assetRepository.save(new AssetEntity(
                UUID.randomUUID(), spec.symbol(), spec.assetClass(), spec.currency(), spec.name(),
                new BigDecimal(spec.initialPrice()), new BigDecimal(spec.drift()), new BigDecimal(spec.volatility()), spec.seed()
            )))).toList();
    }

    private UserEntity seedUser(String email, String roleName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity user = new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode(DEMO_PASSWORD));
            RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role missing - check V1__init_schema.sql seed data"));
            user.assignRole(role);
            return userRepository.save(user);
        });
    }

    private AccountEntity seedAccount(UserEntity owner) {
        List<AccountEntity> existing = accountRepository.findByOwnerId(owner.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return accountRepository.save(new AccountEntity(
            UUID.randomUUID(), owner.getId(), AccountEntity.AccountType.DEMO, "Demo account"
        ));
    }

    private UUID seedPortfolio(AccountEntity account) {
        List<PortfolioProjectionEntity> existing = portfolioProjectionRepository.findByAccountId(account.getId());
        if (!existing.isEmpty()) {
            return existing.get(0).getId();
        }

        Portfolio portfolio = Portfolio.create(
            account.getId(), "Demo portfolio", PortfolioMode.DEMO,
            new BigDecimal("100000.00"), UUID.randomUUID(), account.getOwnerId()
        );
        portfolioEventStoreRepository.append(portfolio, 0L);

        portfolioProjectionRepository.save(new PortfolioProjectionEntity(
            portfolio.getId(), account.getId(), "Demo portfolio", PortfolioMode.DEMO.name(),
            new BigDecimal("100000.00"), portfolio.getVersion(), Instant.now()
        ));
        return portfolio.getId();
    }

    private void seedStrategy(UserEntity owner, UUID portfolioId, List<AssetEntity> assets) {
        if (!strategyRepository.findByOwnerId(owner.getId()).isEmpty()) {
            return;
        }
        AssetEntity btc = assets.stream().filter(a -> a.getSymbol().equals("BTCUSDT")).findFirst().orElseThrow();

        strategyRepository.save(new StrategyEntity(
            UUID.randomUUID(),
            "Demo SMA/RSI crossover",
            "Sample strategy seeded for demonstration - SMA crossover with an RSI filter and an exposure cap.",
            owner.getId(), portfolioId, StrategyMode.PAPER, RiskLevel.MEDIUM,
            null, null,
            "SMA(%s, 20) > SMA(%s, 50) AND RSI(%s, 14) < 70 AND PortfolioExposure < 50%%"
                .formatted(btc.getSymbol(), btc.getSymbol(), btc.getSymbol())
        ));
    }
}
