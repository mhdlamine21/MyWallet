package io.mywallet.risk.domain.detection;

import java.util.List;
import java.util.Random;

/**
 * One tree in an {@link IsolationForest}. Built by repeatedly picking a random feature and
 * a random split value within that feature's observed range, recursively, until either a
 * height limit is reached or a node can't be split further (a single point, or every point
 * identical on the chosen feature). Anomalies are points that get isolated
 * (end up alone in a leaf) in <em>fewer</em> splits than normal points - intuitively, an
 * outlier is "easy to separate from everything else" - which is exactly what
 * {@link #pathLength} measures.
 */
final class IsolationTree {

    private record Node(int splitFeature, double splitValue, Node left, Node right, int size, boolean isLeaf) {
        static Node leaf(int size) {
            return new Node(-1, 0, null, null, size, true);
        }
    }

    private final Node root;

    private IsolationTree(Node root) {
        this.root = root;
    }

    static IsolationTree build(List<double[]> sample, int heightLimit, Random random) {
        return new IsolationTree(buildNode(sample, 0, heightLimit, random));
    }

    private static Node buildNode(List<double[]> data, int currentHeight, int heightLimit, Random random) {
        if (currentHeight >= heightLimit || data.size() <= 1) {
            return Node.leaf(data.size());
        }

        int numFeatures = data.get(0).length;
        int feature = random.nextInt(numFeatures);

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double[] point : data) {
            min = Math.min(min, point[feature]);
            max = Math.max(max, point[feature]);
        }
        if (min == max) {
            return Node.leaf(data.size()); // every point identical on this feature - can't split on it
        }

        double splitValue = min + random.nextDouble() * (max - min);
        List<double[]> left = data.stream().filter(p -> p[feature] < splitValue).toList();
        List<double[]> right = data.stream().filter(p -> p[feature] >= splitValue).toList();

        if (left.isEmpty() || right.isEmpty()) {
            return Node.leaf(data.size()); // degenerate split (can happen at floating-point boundaries)
        }

        return new Node(feature, splitValue,
            buildNode(left, currentHeight + 1, heightLimit, random),
            buildNode(right, currentHeight + 1, heightLimit, random),
            data.size(), false);
    }

    /**
     * Number of splits to isolate {@code point}, plus a correction for the leaf's
     * remaining size (a leaf with more than one point stopped early due to the height
     * limit, not because the points were actually isolated from each other - {@code c(n)}
     * estimates how many more splits it would have taken).
     */
    double pathLength(double[] point) {
        return pathLength(point, root, 0);
    }

    private double pathLength(double[] point, Node node, int currentHeight) {
        if (node.isLeaf()) {
            return currentHeight + PathLengthMath.averagePathLength(node.size());
        }
        Node next = point[node.splitFeature()] < node.splitValue() ? node.left() : node.right();
        return pathLength(point, next, currentHeight + 1);
    }
}
