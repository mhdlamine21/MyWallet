package io.mywallet.risk.domain.detection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class IsolationForestTest {

    @Test
    void anObviousOutlierScoresHigherThanNormalClusterPoints() {
        List<double[]> data = new ArrayList<>();
        Random random = new Random(42);
        // A tight normal cluster around (0, 0).
        for (int i = 0; i < 200; i++) {
            data.add(new double[]{random.nextGaussian() * 0.5, random.nextGaussian() * 0.5});
        }
        // One obvious outlier, far from the cluster.
        double[] outlier = {50.0, 50.0};
        data.add(outlier);

        IsolationForest forest = IsolationForest.fit(data, 100, 64, 7L);

        double outlierScore = forest.anomalyScore(outlier);
        double normalPointScore = forest.anomalyScore(new double[]{0.1, -0.1});

        assertThat(outlierScore).isGreaterThan(normalPointScore);
        assertThat(outlierScore).isGreaterThan(0.7); // per the paper's convention, >0.6-0.7 is a strong anomaly signal
    }

    @Test
    void pointsInsideADenseClusterScoreConsistentlyLowerThanFarOutliers() {
        List<double[]> data = new ArrayList<>();
        Random random = new Random(123);
        for (int i = 0; i < 300; i++) {
            data.add(new double[]{10 + random.nextGaussian(), 10 + random.nextGaussian(), 10 + random.nextGaussian()});
        }
        IsolationForest forest = IsolationForest.fit(data, 100, 128, 9L);

        double clusterCenterScore = forest.anomalyScore(new double[]{10, 10, 10});
        double farOutlierScore = forest.anomalyScore(new double[]{1000, 1000, 1000});

        assertThat(farOutlierScore).isGreaterThan(clusterCenterScore);
    }

    @Test
    void isDeterministicGivenTheSameSeed() {
        List<double[]> data = List.of(new double[]{1, 1}, new double[]{2, 2}, new double[]{100, 100}, new double[]{1.5, 1.5});

        IsolationForest forestA = IsolationForest.fit(data, 50, 4, 99L);
        IsolationForest forestB = IsolationForest.fit(data, 50, 4, 99L);

        assertThat(forestA.anomalyScore(new double[]{100, 100})).isEqualTo(forestB.anomalyScore(new double[]{100, 100}));
    }

    @Test
    void rejectsFittingOnAnEmptyDataset() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> IsolationForest.fit(List.of(), 10, 5, 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anomalyScoresAreAlwaysInTheValidZeroToOneRange() {
        List<double[]> data = new ArrayList<>();
        Random random = new Random(5);
        for (int i = 0; i < 100; i++) {
            data.add(new double[]{random.nextGaussian(), random.nextGaussian()});
        }
        IsolationForest forest = IsolationForest.fit(data, 50, 32, 5L);

        for (double[] point : data) {
            double score = forest.anomalyScore(point);
            assertThat(score).isBetween(0.0, 1.0);
        }
    }
}
