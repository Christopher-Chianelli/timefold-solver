package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.math.BigDecimal;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.quad.InnerQuadConstraintStream;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.quarkus.deployment.lambda.ScoreWeightType;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.DistinctNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.FilterNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.FlattenLastNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.GroupByNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.IfExistsNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.IfNotExistsNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByConfigurableMatchWeightNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ImpactByMatchWeightNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.JoinNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.JoinerDefinition;
import ai.timefold.solver.quarkus.deployment.lambda.ast.MapNode;

public final class AsmQuadConstraintStream<A, B, C, D> extends AbstractAsmConstraintStream
        implements InnerQuadConstraintStream<A, B, C, D> {
    public AsmQuadConstraintStream(AbstractAsmConstraintStream parent,
            JoinNode<?> ast) {
        super(parent, ast);
    }

    public AsmQuadConstraintStream(AbstractAsmConstraintStream parent, IfExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmQuadConstraintStream(AbstractAsmConstraintStream parent, IfNotExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmQuadConstraintStream(AbstractAsmConstraintStream parent, AbstractConstraintStreamNode child) {
        super(parent, child);
    }

    @Override
    public QuadConstraintStream<A, B, C, D> filter(QuadPredicate<A, B, C, D> predicate) {
        return new AsmQuadConstraintStream<>(this, new FilterNode<>(predicate));
    }

    @Override
    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifExists(Class<E> otherClass, PentaJoiner<A, B, C, D, E>... joiners) {
        return new AsmQuadConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifExistsIncludingNullVars(Class<E> otherClass,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return new AsmQuadConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExists(Class<E> otherClass, PentaJoiner<A, B, C, D, E>... joiners) {
        return new AsmQuadConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExistsIncludingNullVars(Class<E> otherClass,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return new AsmQuadConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        return new AsmUniConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collector)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
            QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping) {
        return new AsmUniConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping, QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping, QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping, QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .withCollectors(collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(
                    QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadFunction<A, B, C, D, GroupKeyD_> groupKeyDMapping) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping)
                .build());
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(QuadFunction<A, B, C, D, ResultA_> mapping) {
        return new AsmUniConstraintStream<>(this, new MapNode<>(mapping));
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(QuadFunction<A, B, C, D, ResultA_> mappingA,
            QuadFunction<A, B, C, D, ResultB_> mappingB) {
        return new AsmBiConstraintStream<>(this, new MapNode<>(mappingA, mappingB));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC) {
        return new AsmTriConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC, QuadFunction<A, B, C, D, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC, mappingD));
    }

    @Override
    public <ResultD_> QuadConstraintStream<A, B, C, ResultD_> flattenLast(Function<D, Iterable<ResultD_>> mapping) {
        return new AsmQuadConstraintStream<>(this, new FlattenLastNode<>(mapping));
    }

    @Override
    public QuadConstraintStream<A, B, C, D> distinct() {
        return new AsmQuadConstraintStream<>(this, new DistinctNode());
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalize(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurableBigDecimal(
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> reward(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurableBigDecimal(
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactLong(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactBigDecimal(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> impactConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> impactConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public QuadConstraintBuilder<A, B, C, D, ?> impactConfigurableBigDecimal(
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.INTEGER,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.LONG,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmQuadConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.BIG_DECIMAL,
                        constraintWeight, matchWeigher)));
    }
}
