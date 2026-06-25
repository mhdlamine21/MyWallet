package io.mywallet.risk.domain.detection;

/**
 * Shared math between {@link IsolationTree} and {@link IsolationForest}: the "average path
 * length of an unsuccessful search in a Binary Search Tree" formula from the original
 * Isolation Forest paper (Liu, Ting & Zhou, 2008) - this is what both a single tree's leaf
 * size correction and the forest's overall normalization constant are built from.
 */
final class PathLengthMath {

    private static final double EULER_MASCHERONI = 0.5772156649;

    private PathLengthMath() {
    }

    /** Harmonic number H(i) ≈ ln(i) + γ - the standard approximation, accurate for i ≥ ~2. */
    static double harmonic(int i) {
        if (i <= 0) {
            return 0;
        }
        return Math.log(i) + EULER_MASCHERONI;
    }

    /** c(n): expected path length to isolate a point in a random tree over n points. */
    static double averagePathLength(int n) {
        if (n <= 1) {
            return 0;
        }
        return 2 * harmonic(n - 1) - (2.0 * (n - 1) / n);
    }
}
