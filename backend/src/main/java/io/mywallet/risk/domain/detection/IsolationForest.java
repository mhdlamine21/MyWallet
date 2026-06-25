package io.mywallet.risk.domain.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An ensemble of {@link IsolationTree}s trained (unsupervised - no labeled "normal" vs
 * "anomalous" data needed) over a population of feature vectors, used to score how
 * anomalous a given point is relative to that population. This is the "explainable-ish,
 * not-a-black-box" detection improvement from the original feature brainstorm - it's not
 * a neural network; the anomaly score is directly traceable to "this point was isolated
 * faster than average across N random trees", which a caller can sanity-check.
 *
 * <p>Score interpretation (per the original paper): close to 1 -> likely anomaly, close to
 * 0.5 -> no clear signal either way, well below 0.5 -> likely normal.</p>
 */
public final class IsolationForest {

    private final List<IsolationTree> trees;
    private final double normalizationConstant;

    private IsolationForest(List<IsolationTree> trees, double normalizationConstant) {
        this.trees = trees;
        this.normalizationConstant = normalizationConstant;
    }

    /**
     * @param data       the population of feature vectors to build the forest from (every
     *                   vector must have the same number of features)
     * @param numTrees   how many trees to build - more trees stabilize the score at the
     *                   cost of more computation; 100 is the paper's usual default
     * @param sampleSize how many points each individual tree is built from, subsampled
     *                   (with the whole population reshuffled per tree) - subsampling is
     *                   what keeps isolation forests fast even on large populations, and
     *                   also what gives different trees different "views", which is where
     *                   the ensemble's robustness comes from
     */
    public static IsolationForest fit(List<double[]> data, int numTrees, int sampleSize, long seed) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Cannot fit an IsolationForest on an empty dataset");
        }
        Random random = new Random(seed);
        int effectiveSampleSize = Math.min(sampleSize, data.size());
        int heightLimit = (int) Math.ceil(log2(Math.max(2, effectiveSampleSize)));

        List<IsolationTree> trees = new ArrayList<>(numTrees);
        for (int i = 0; i < numTrees; i++) {
            trees.add(IsolationTree.build(subsample(data, effectiveSampleSize, random), heightLimit, random));
        }

        return new IsolationForest(trees, PathLengthMath.averagePathLength(effectiveSampleSize));
    }

    /** @return a score in (0, 1]; see the class javadoc for how to interpret it. */
    public double anomalyScore(double[] point) {
        double averagePathLength = trees.stream().mapToDouble(t -> t.pathLength(point)).average().orElse(0);
        if (normalizationConstant == 0) {
            return 0.5; // degenerate (population of size ≤ 1) - no meaningful signal
        }
        return Math.pow(2, -averagePathLength / normalizationConstant);
    }

    private static List<double[]> subsample(List<double[]> data, int sampleSize, Random random) {
        if (data.size() <= sampleSize) {
            return new ArrayList<>(data);
        }
        List<double[]> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, sampleSize);
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
