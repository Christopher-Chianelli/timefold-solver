package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.math.BigDecimal;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.tri.InnerTriConstraintStream;
import ai.timefold.solver.core.api.function.ToIntTriFunction;
import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollector;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
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

public final class AsmTriConstraintStream<A, B, C> extends AbstractAsmConstraintStream implements
        InnerTriConstraintStream<A, B, C> {
    public AsmTriConstraintStream(AbstractAsmConstraintStream parent,
            JoinNode<?> ast) {
        super(parent, ast);
    }

    public AsmTriConstraintStream(AbstractAsmConstraintStream parent, IfExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmTriConstraintStream(AbstractAsmConstraintStream parent, IfNotExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmTriConstraintStream(AbstractAsmConstraintStream parent, AbstractConstraintStreamNode child) {
        super(parent, child);
    }

    @Override
    public TriConstraintStream<A, B, C> filter(TriPredicate<A, B, C> predicate) {
        return new AsmTriConstraintStream<>(this, new FilterNode<>(predicate));
    }

    @Override
    @SafeVarargs
    public final <D> QuadConstraintStream<A, B, C, D> join(UniConstraintStream<D> otherStream,
            QuadJoiner<A, B, C, D>... joiners) {
        return new AsmQuadConstraintStream<>(this, new JoinNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        ((AbstractAsmConstraintStream) otherStream).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <D> QuadConstraintStream<A, B, C, D> join(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners) {
        return join(createUni(otherClass), joiners);
    }

    @Override
    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners) {
        return new AsmTriConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        return new AsmTriConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifNotExists(Class<D> otherClass, QuadJoiner<A, B, C, D>... joiners) {
        return new AsmTriConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <D> TriConstraintStream<A, B, C> ifNotExistsIncludingNullVars(Class<D> otherClass,
            QuadJoiner<A, B, C, D>... joiners) {
        return new AsmTriConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        return new AsmUniConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collector)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
            TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    TriConstraintCollector<A, B, C, ResultContainerA_, ResultA_> collectorA,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(TriFunction<A, B, C, GroupKey_> groupKeyMapping) {
        return new AsmUniConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            TriFunction<A, B, C, GroupKey_> groupKeyMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    TriFunction<A, B, C, GroupKey_> groupKeyMapping,
                    TriConstraintCollector<A, B, C, ResultContainerB_, ResultB_> collectorB,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriConstraintCollector<A, B, C, ResultContainer_, Result_> collector) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriConstraintCollector<A, B, C, ResultContainerC_, ResultC_> collectorC,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
            TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping,
                    TriConstraintCollector<A, B, C, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .withCollectors(collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(
                    TriFunction<A, B, C, GroupKeyA_> groupKeyAMapping, TriFunction<A, B, C, GroupKeyB_> groupKeyBMapping,
                    TriFunction<A, B, C, GroupKeyC_> groupKeyCMapping, TriFunction<A, B, C, GroupKeyD_> groupKeyDMapping) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping)
                .build());
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(TriFunction<A, B, C, ResultA_> mapping) {
        return new AsmUniConstraintStream<>(this, new MapNode<>(mapping));
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(TriFunction<A, B, C, ResultA_> mappingA,
            TriFunction<A, B, C, ResultB_> mappingB) {
        return new AsmBiConstraintStream<>(this, new MapNode<>(mappingA, mappingB));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC) {
        return new AsmTriConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            TriFunction<A, B, C, ResultA_> mappingA, TriFunction<A, B, C, ResultB_> mappingB,
            TriFunction<A, B, C, ResultC_> mappingC, TriFunction<A, B, C, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC, mappingD));
    }

    @Override
    public <ResultC_> TriConstraintStream<A, B, ResultC_> flattenLast(Function<C, Iterable<ResultC_>> mapping) {
        return new AsmTriConstraintStream<>(this, new FlattenLastNode<>(mapping));
    }

    @Override
    public TriConstraintStream<A, B, C> distinct() {
        return new AsmTriConstraintStream<>(this, new DistinctNode());
    }

    @Override
    public <ResultD_> QuadConstraintStream<A, B, C, ResultD_> expand(TriFunction<A, B, C, ResultD_> mapping) {
        return new AsmQuadConstraintStream<>(this, new ExpandNode<>(mapping));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalize(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> penalizeBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> penalizeConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> penalizeConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> penalizeConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.PENALTY,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> reward(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> rewardBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> rewardConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> rewardConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> rewardConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.REWARD,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impact(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactLong(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> impactBigDecimal(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                constraintWeight,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> impactConfigurable(ToIntTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.INTEGER,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> impactConfigurableLong(ToLongTriFunction<A, B, C> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.LONG,
                matchWeigher)));
    }

    @Override
    public TriConstraintBuilder<A, B, C, ?> impactConfigurableBigDecimal(TriFunction<A, B, C, BigDecimal> matchWeigher) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(), withChild(new ImpactByConfigurableMatchWeightNode<>(
                ScoreImpactType.MIXED,
                ScoreWeightType.BIG_DECIMAL,
                matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToIntTriFunction<A, B, C> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.INTEGER,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            ToLongTriFunction<A, B, C> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.LONG,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> TriConstraintBuilder<A, B, C, Score_> innerImpact(Score_ constraintWeight,
            TriFunction<A, B, C, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmTriConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.BIG_DECIMAL,
                        constraintWeight, matchWeigher)));
    }
}
