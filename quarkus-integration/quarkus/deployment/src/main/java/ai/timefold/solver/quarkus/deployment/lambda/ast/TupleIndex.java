package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;

public record TupleIndex(int[] indices) implements Comparable<TupleIndex> {
    public static TupleIndex empty() {
        return new TupleIndex(new int[] {});
    }

    public static TupleIndex singleton(int value) {
        return new TupleIndex(new int[] { value });
    }

    public TupleIndex concat(TupleIndex other) {
        int[] newIndices = Arrays.copyOf(indices, indices.length + other.indices.length);
        System.arraycopy(other.indices, 0, newIndices, indices.length, other.indices.length);
        return new TupleIndex(newIndices);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        TupleIndex that = (TupleIndex) object;
        return Arrays.equals(indices, that.indices);
    }

    @Override
    public int compareTo(TupleIndex other) {
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] != other.indices[i]) {
                return indices[i] - other.indices[i];
            }
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(indices);
    }
}
