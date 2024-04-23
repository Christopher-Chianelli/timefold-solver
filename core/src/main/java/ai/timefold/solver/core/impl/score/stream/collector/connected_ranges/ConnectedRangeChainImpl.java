package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

public final class ConnectedRangeChainImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConnectedRangeChain<Interval_, Point_, Difference_> {

    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, ConnectedRangeImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToCluster;
    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, RangeGapImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToNextBreak;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConnectedRangeChainImpl(NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.clusterStartSplitPointToCluster = new TreeMap<>();
        this.clusterStartSplitPointToNextBreak = new TreeMap<>();
        this.splitPointSet = splitPointSet;
        this.differenceFunction = differenceFunction;
    }

    void addInterval(Interval<Interval_, Point_> interval) {
        var intersectedIntervalClusterMap = clusterStartSplitPointToCluster
                .subMap(Objects.requireNonNullElseGet(clusterStartSplitPointToCluster.floorKey(interval.getStartSplitPoint()),
                        interval::getStartSplitPoint), true, interval.getEndSplitPoint(), true);

        // Case: the interval cluster before this interval does not intersect this interval
        if (!intersectedIntervalClusterMap.isEmpty()
                && intersectedIntervalClusterMap.firstEntry().getValue().getEndSplitPoint()
                        .isBefore(interval.getStartSplitPoint())) {
            // Get the tail map after the first cluster
            intersectedIntervalClusterMap = intersectedIntervalClusterMap.subMap(intersectedIntervalClusterMap.firstKey(),
                    false, intersectedIntervalClusterMap.lastKey(), true);
        }

        if (intersectedIntervalClusterMap.isEmpty()) {
            // Interval does not intersect anything
            // Ex:
            //     -----
            //----       -----
            createNewIntervalCluster(interval);
            return;
        }

        // Interval intersect at least one cluster
        // Ex:
        //      -----------------
        //  ------  ------  ---   ----
        var firstIntersectedIntervalCluster = intersectedIntervalClusterMap.firstEntry().getValue();
        var oldStartSplitPoint = firstIntersectedIntervalCluster.getStartSplitPoint();
        firstIntersectedIntervalCluster.addInterval(interval);

        // Merge all the intersected interval clusters into the first intersected
        // interval cluster
        intersectedIntervalClusterMap.tailMap(oldStartSplitPoint, false).values()
                .forEach(firstIntersectedIntervalCluster::mergeConnectedRange);

        // Remove all the intersected interval clusters after the first intersected
        // one, since they are now merged in the first
        intersectedIntervalClusterMap.tailMap(oldStartSplitPoint, false).clear();
        removeSpannedBreaksAndUpdateIntersectedBreaks(interval, firstIntersectedIntervalCluster);

        // If the first intersected interval cluster start after the interval,
        // we need to make the interval start point the key for this interval
        // cluster in the map
        if (oldStartSplitPoint.isAfter(firstIntersectedIntervalCluster.getStartSplitPoint())) {
            clusterStartSplitPointToCluster.remove(oldStartSplitPoint);
            clusterStartSplitPointToCluster.put(firstIntersectedIntervalCluster.getStartSplitPoint(),
                    firstIntersectedIntervalCluster);
            var nextBreak = clusterStartSplitPointToNextBreak.get(firstIntersectedIntervalCluster.getStartSplitPoint());
            if (nextBreak != null) {
                nextBreak.setPreviousCluster(firstIntersectedIntervalCluster);
                nextBreak.setLength(differenceFunction.apply(nextBreak.getPreviousRangeEnd(),
                        nextBreak.getNextRangeStart()));
            }
        }
    }

    private void createNewIntervalCluster(Interval<Interval_, Point_> interval) {
        // Interval does not intersect anything
        // Ex:
        //     -----
        //----       -----
        var startSplitPoint = splitPointSet.floor(interval.getStartSplitPoint());
        var newCluster = new ConnectedRangeImpl<>(splitPointSet, differenceFunction, startSplitPoint);
        clusterStartSplitPointToCluster.put(startSplitPoint, newCluster);

        // If there a cluster after this interval, add a new break
        // between this interval and the next cluster
        var nextClusterEntry = clusterStartSplitPointToCluster.higherEntry(startSplitPoint);
        if (nextClusterEntry != null) {
            var nextCluster = nextClusterEntry.getValue();
            var difference = differenceFunction.apply(newCluster.getEnd(), nextCluster.getStart());
            var newBreak = new RangeGapImpl<>(newCluster, nextCluster, difference);
            clusterStartSplitPointToNextBreak.put(startSplitPoint, newBreak);
        }

        // If there a cluster before this interval, add a new break
        // between this interval and the previous cluster
        // (this will replace the old break, if there was one)
        var previousClusterEntry = clusterStartSplitPointToCluster.lowerEntry(startSplitPoint);
        if (previousClusterEntry != null) {
            var previousCluster = previousClusterEntry.getValue();
            var difference = differenceFunction.apply(previousCluster.getEnd(), newCluster.getStart());
            var newBreak = new RangeGapImpl<>(previousCluster, newCluster, difference);
            clusterStartSplitPointToNextBreak.put(previousClusterEntry.getKey(), newBreak);
        }
    }

    private void removeSpannedBreaksAndUpdateIntersectedBreaks(Interval<Interval_, Point_> interval,
            ConnectedRangeImpl<Interval_, Point_, Difference_> intervalCluster) {
        var firstBreakSplitPointBeforeInterval = Objects.requireNonNullElseGet(
                clusterStartSplitPointToNextBreak.floorKey(interval.getStartSplitPoint()), interval::getStartSplitPoint);
        var intersectedIntervalBreakMap = clusterStartSplitPointToNextBreak.subMap(firstBreakSplitPointBeforeInterval, true,
                interval.getEndSplitPoint(), true);

        if (intersectedIntervalBreakMap.isEmpty()) {
            return;
        }

        var clusterBeforeFirstIntersectedBreak =
                (ConnectedRangeImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.firstEntry().getValue()
                        .getPreviousConnectedRange());
        var clusterAfterFinalIntersectedBreak =
                (ConnectedRangeImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.lastEntry().getValue()
                        .getNextConnectedRange());

        // All breaks that are not the first or last intersected breaks will
        // be removed (as interval span them)
        if (!interval.getStartSplitPoint()
                .isAfter(clusterBeforeFirstIntersectedBreak.getEndSplitPoint())) {
            if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
                // Case: interval spans all breaks
                // Ex:
                //   -----------
                //----  ------ -----
                intersectedIntervalBreakMap.clear();
            } else {
                // Case: interval span first break, but does not span the final break
                // Ex:
                //   -----------
                //----  ------   -----
                var finalBreak = intersectedIntervalBreakMap.lastEntry().getValue();
                finalBreak.setPreviousCluster(intervalCluster);
                finalBreak.setLength(
                        differenceFunction.apply(finalBreak.getPreviousRangeEnd(),
                                finalBreak.getNextRangeStart()));
                intersectedIntervalBreakMap.clear();
                clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
            }
        } else if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
            // Case: interval span final break, but does not span the first break
            // Ex:
            //     -----------
            //----   -----   -----
            var previousBreakEntry = intersectedIntervalBreakMap.firstEntry();
            var previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousRangeEnd(), intervalCluster.getStart()));
            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak
                    .put(((ConnectedRangeImpl<Interval_, Point_, Difference_>) (previousBreak
                            .getPreviousConnectedRange())).getStartSplitPoint(), previousBreak);
        } else {
            // Case: interval does not span either the first or final break
            // Ex:
            //     ---------
            //----  ------   -----
            var finalBreak = intersectedIntervalBreakMap.lastEntry().getValue();
            finalBreak.setLength(differenceFunction.apply(finalBreak.getPreviousRangeEnd(),
                    finalBreak.getNextRangeStart()));

            var previousBreakEntry = intersectedIntervalBreakMap.firstEntry();
            var previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousRangeEnd(), intervalCluster.getStart()));

            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak.put(previousBreakEntry.getKey(), previousBreak);
            clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
        }
    }

    void removeInterval(Interval<Interval_, Point_> interval) {
        var intervalClusterEntry = clusterStartSplitPointToCluster.floorEntry(interval.getStartSplitPoint());
        var intervalCluster = intervalClusterEntry.getValue();
        clusterStartSplitPointToCluster.remove(intervalClusterEntry.getKey());
        var previousBreakEntry = clusterStartSplitPointToNextBreak.lowerEntry(intervalClusterEntry.getKey());
        var nextIntervalClusterEntry = clusterStartSplitPointToCluster.higherEntry(intervalClusterEntry.getKey());
        clusterStartSplitPointToNextBreak.remove(intervalClusterEntry.getKey());

        var previousBreak = (previousBreakEntry != null) ? previousBreakEntry.getValue() : null;
        var previousIntervalCluster = (previousBreak != null)
                ? (ConnectedRangeImpl<Interval_, Point_, Difference_>) previousBreak.getPreviousConnectedRange()
                : null;

        var iterator = new ConnectedSubrangeIterator<>(splitPointSet,
                intervalCluster.getStartSplitPoint(),
                intervalCluster.getEndSplitPoint(),
                differenceFunction);
        while (iterator.hasNext()) {
            var newIntervalCluster = iterator.next();
            if (previousBreak != null) {
                previousBreak.setNextCluster(newIntervalCluster);
                previousBreak.setLength(differenceFunction.apply(previousBreak.getPreviousConnectedRange().getEnd(),
                        newIntervalCluster.getStart()));
                clusterStartSplitPointToNextBreak
                        .put(((ConnectedRangeImpl<Interval_, Point_, Difference_>) previousBreak
                                .getPreviousConnectedRange()).getStartSplitPoint(), previousBreak);
            }
            previousBreak = new RangeGapImpl<>(newIntervalCluster, null, null);
            previousIntervalCluster = newIntervalCluster;
            clusterStartSplitPointToCluster.put(newIntervalCluster.getStartSplitPoint(), newIntervalCluster);
        }

        if (nextIntervalClusterEntry != null && previousBreak != null) {
            previousBreak.setNextCluster(nextIntervalClusterEntry.getValue());
            previousBreak.setLength(differenceFunction.apply(previousIntervalCluster.getEnd(),
                    nextIntervalClusterEntry.getValue().getStart()));
            clusterStartSplitPointToNextBreak.put(previousIntervalCluster.getStartSplitPoint(),
                    previousBreak);
        } else if (previousBreakEntry != null && previousBreak == previousBreakEntry.getValue()) {
            // i.e. interval was the last interval in the cluster,
            // (previousBreak == previousBreakEntry.getValue()),
            // and there is no interval cluster after it
            // (previousBreak != null as previousBreakEntry != null,
            // so it must be the case nextIntervalClusterEntry == null)
            clusterStartSplitPointToNextBreak.remove(previousBreakEntry.getKey());
        }
    }

    @Override
    public Iterable<ConnectedRange<Interval_, Point_, Difference_>> getConnectedRanges() {
        return (Iterable) clusterStartSplitPointToCluster.values();
    }

    @Override
    public Iterable<RangeGap<Point_, Difference_>> getGaps() {
        return (Iterable) clusterStartSplitPointToNextBreak.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectedRangeChainImpl<?, ?, ?> that))
            return false;
        return Objects.equals(clusterStartSplitPointToCluster,
                that.clusterStartSplitPointToCluster)
                && Objects.equals(splitPointSet,
                        that.splitPointSet)
                && Objects.equals(clusterStartSplitPointToNextBreak,
                        that.clusterStartSplitPointToNextBreak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterStartSplitPointToCluster, splitPointSet, clusterStartSplitPointToNextBreak);
    }

    @Override
    public String toString() {
        return "Clusters {" +
                "intervalClusters=" + getConnectedRanges() +
                ", breaks=" + getGaps() +
                '}';
    }
}
