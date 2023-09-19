package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.bi.InnerBiConstraintStream;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriJoiner;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.quarkus.deployment.lambda.ScoreWeightType;
import ai.timefold.solver.quarkus.deployment.lambda.ast.AbstractConstraintStreamNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.DistinctNode;
import ai.timefold.solver.quarkus.deployment.lambda.ast.ExpandNode;
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

public final class AsmBiConstraintStream<A, B> extends AbstractAsmConstraintStream implements InnerBiConstraintStream<A, B> {
    public AsmBiConstraintStream(AbstractAsmConstraintStream parent,
            JoinNode<?> ast) {
        super(parent, ast);
    }

    public AsmBiConstraintStream(AbstractAsmConstraintStream parent, IfExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmBiConstraintStream(AbstractAsmConstraintStream parent, IfNotExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmBiConstraintStream(AbstractAsmConstraintStream parent, AbstractConstraintStreamNode child) {
        super(parent, child);
    }

    @Override
    public BiConstraintStream<A, B> filter(BiPredicate<A, B> predicate) {
        return new AsmBiConstraintStream<>(this, new FilterNode<>(predicate));
    }

    @Override
    @SafeVarargs
    public final <C> TriConstraintStream<A, B, C> join(UniConstraintStream<C> otherStream, TriJoiner<A, B, C>... joiners) {
        return new AsmTriConstraintStream<>(this,
                new JoinNode<>(JoinerDefinition.from(joiners)).withParents(getLeafNode(),
                        ((AbstractAsmConstraintStream) otherStream).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <C> TriConstraintStream<A, B, C> join(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return join(createUni(otherClass), joiners);
    }

    @Override
    @SafeVarargs
    public final <C> BiConstraintStream<A, B> ifExists(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return new AsmBiConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <C> BiConstraintStream<A, B> ifExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return new AsmBiConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <C> BiConstraintStream<A, B> ifNotExists(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return new AsmBiConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <C> BiConstraintStream<A, B> ifNotExistsIncludingNullVars(Class<C> otherClass, TriJoiner<A, B, C>... joiners) {
        return new AsmBiConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        return new AsmUniConstraintStream<>(this,
                GroupByNode.builder()
                        .withCollectors(collector)
                        .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
            BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB) {
        return new AsmBiConstraintStream<>(this,
                GroupByNode.builder()
                        .withCollectors(collectorA, collectorB)
                        .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this,
                GroupByNode.builder()
                        .withCollectors(collectorA, collectorB, collectorC)
                        .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    BiConstraintCollector<A, B, ResultContainerA_, ResultA_> collectorA,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this,
                GroupByNode.builder()
                        .withCollectors(collectorA, collectorB, collectorC, collectorD)
                        .build());
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(BiFunction<A, B, GroupKey_> groupKeyMapping) {
        return new AsmUniConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyMapping)
                        .build());
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            BiFunction<A, B, GroupKey_> groupKeyMapping, BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        return new AsmBiConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyMapping)
                        .withCollectors(collector)
                        .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    BiFunction<A, B, GroupKey_> groupKeyMapping,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyMapping)
                        .withCollectors(collectorB, collectorC)
                        .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKey_> groupKeyMapping,
                    BiConstraintCollector<A, B, ResultContainerB_, ResultB_> collectorB,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyMapping)
                        .withCollectors(collectorB, collectorC, collectorD)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping) {
        return new AsmBiConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
            BiConstraintCollector<A, B, ResultContainer_, Result_> collector) {
        return new AsmTriConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping)
                        .withCollectors(collector)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiConstraintCollector<A, B, ResultContainerC_, ResultC_> collectorC,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping)
                        .withCollectors(collectorC, collectorD)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
            BiFunction<A, B, GroupKeyC_> groupKeyCMapping) {
        return new AsmTriConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiFunction<A, B, GroupKeyC_> groupKeyCMapping,
                    BiConstraintCollector<A, B, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                        .withCollectors(collectorD)
                        .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(
                    BiFunction<A, B, GroupKeyA_> groupKeyAMapping, BiFunction<A, B, GroupKeyB_> groupKeyBMapping,
                    BiFunction<A, B, GroupKeyC_> groupKeyCMapping, BiFunction<A, B, GroupKeyD_> groupKeyDMapping) {
        return new AsmQuadConstraintStream<>(this,
                GroupByNode.builder()
                        .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping)
                        .build());
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(BiFunction<A, B, ResultA_> mapping) {
        return new AsmUniConstraintStream<>(this, new MapNode<>(mapping));
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(BiFunction<A, B, ResultA_> mappingA,
            BiFunction<A, B, ResultB_> mappingB) {
        return new AsmBiConstraintStream<>(this, new MapNode<>(mappingA, mappingB));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            BiFunction<A, B, ResultA_> mappingA, BiFunction<A, B, ResultB_> mappingB, BiFunction<A, B, ResultC_> mappingC) {
        return new AsmTriConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            BiFunction<A, B, ResultA_> mappingA, BiFunction<A, B, ResultB_> mappingB, BiFunction<A, B, ResultC_> mappingC,
            BiFunction<A, B, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC, mappingD));
    }

    @Override
    public <ResultB_> BiConstraintStream<A, ResultB_> flattenLast(Function<B, Iterable<ResultB_>> mapping) {
        return new AsmBiConstraintStream<>(this, new FlattenLastNode<>(mapping));
    }

    @Override
    public BiConstraintStream<A, B> distinct() {
        return new AsmBiConstraintStream<>(this, new DistinctNode());
    }

    @Override
    public <ResultC_> TriConstraintStream<A, B, ResultC_> expand(BiFunction<A, B, ResultC_> mapping) {
        return new AsmTriConstraintStream<>(this, new ExpandNode<>(mapping));
    }

    @Override
    public <ResultC_, ResultD_> QuadConstraintStream<A, B, ResultC_, ResultD_> expand(BiFunction<A, B, ResultC_> mappingC,
            BiFunction<A, B, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new ExpandNode<>(mappingC, mappingD));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalize(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> penalizeBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> penalizeConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> penalizeConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> penalizeConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> reward(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> rewardLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> rewardBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> rewardConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> rewardConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> rewardConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impact(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impactLong(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> impactBigDecimal(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> impactConfigurable(ToIntBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> impactConfigurableLong(ToLongBiFunction<A, B> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public BiConstraintBuilder<A, B, ?> impactConfigurableBigDecimal(BiFunction<A, B, BigDecimal> matchWeigher) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToIntBiFunction<A, B> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.INTEGER,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            ToLongBiFunction<A, B> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.LONG,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> BiConstraintBuilder<A, B, Score_> innerImpact(Score_ constraintWeight,
            BiFunction<A, B, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmBiConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.BIG_DECIMAL,
                        constraintWeight, matchWeigher)));
    }
}
