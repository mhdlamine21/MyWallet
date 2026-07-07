package io.mywallet.infrastructure.seed;

import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import io.mywallet.support.PostgresIntegrationTest;
import io.mywallet.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Explicitly overrides the JWT secret rather than relying on the "seed" profile being
 * treated as safe by {@code JwtSecretSafetyCheck} - that profile is deliberately NOT in
 * the safety check's allow-list, because docker-compose.yml also activates "seed" for
 * real local deployments that genuinely need a real secret. Giving this test its own
 * realistic-length secret keeps the safety check's real-world behavior intact instead of
 * weakening it just to make a test pass.
 */
@ActiveProfiles("seed")
@TestPropertySource(properties = "mywallet.jwt.secret=test-secret-for-data-seeder-integration-test-only-1234567890")
class DataSeederTest extends PostgresIntegrationTest {

    @Autowired private DataSeeder dataSeeder;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private AssetJpaRepository assetRepository;
    @Autowired private StrategyJpaRepository strategyRepository;

    @Test
    void seedingTwiceDoesNotDuplicateAnything() {
        dataSeeder.run();
        long usersAfterFirstRun = userRepository.count();
        long assetsAfterFirstRun = assetRepository.count();
        long strategiesAfterFirstRun = strategyRepository.count();

        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(usersAfterFirstRun);
        assertThat(assetRepository.count()).isEqualTo(assetsAfterFirstRun);
        assertThat(strategyRepository.count()).isEqualTo(strategiesAfterFirstRun);

        assertThat(userRepository.findByEmail("demo.investor@mywallet.dev")).isPresent();
        assertThat(assetRepository.findBySymbol("BTCUSDT")).isPresent();
    }
}
