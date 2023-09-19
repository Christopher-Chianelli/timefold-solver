package ai.timefold.solver.quarkus.deployment.lambda.ast;

import java.util.Arrays;

public record TupleRecord(Object[] items) {
    public static TupleRecord EMPTY = new TupleRecord(new Object[] {});

    public static TupleRecord empty() {
        return new TupleRecord(null);
    }

    public static TupleRecord of(Object... items) {
        return new TupleRecord(items);
    }

    public TupleRecord concat(TupleRecord other) {
        Object[] newItems = Arrays.copyOf(items, items.length + other.items.length);
        System.arraycopy(other.items, 0, newItems, items.length, other.items.length);
        return new TupleRecord(newItems);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        TupleRecord that = (TupleRecord) object;
        return Arrays.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }
}
