package ai.timefold.solver.quarkus.deployment.lambda.api;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.bi.BiJoinerComber;
import ai.timefold.solver.constraint.streams.common.uni.InnerUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.bi.BiJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.quarkus.deployment.lambda.AsmConstraintFactory;
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

public final class AsmUniConstraintStream<A> extends AbstractAsmConstraintStream implements InnerUniConstraintStream<A> {
    public AsmUniConstraintStream(AsmConstraintFactory<?> constraintFactory,
            AbstractConstraintStreamNode leafNode,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, leafNode, retrievalSemantics);
    }

    public AsmUniConstraintStream(AbstractAsmConstraintStream parent, IfExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmUniConstraintStream(AbstractAsmConstraintStream parent, IfNotExistsNode<?> ast) {
        super(parent, ast);
    }

    public AsmUniConstraintStream(AbstractAsmConstraintStream parent, AbstractConstraintStreamNode child) {
        super(parent, child);
    }

    @Override
    public UniConstraintStream<A> filter(Predicate<A> predicate) {
        return new AsmUniConstraintStream<>(this, new FilterNode<>(predicate));
    }

    @Override
    @SafeVarargs
    public final <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream, BiJoiner<A, B>... joiners) {
        return new AsmBiConstraintStream<>(this, new JoinNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        ((AbstractAsmConstraintStream) otherStream).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <B> BiConstraintStream<A, B> join(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return join(createUni(otherClass), joiners);
    }

    @Override
    public <B> BiConstraintStream<A, B> join(UniConstraintStream<B> otherStream, BiJoinerComber<A, B> joinerComber) {
        return join(otherStream, joinerComber.getMergedJoiner(), Joiners.filtering(joinerComber.getMergedFiltering()));
    }

    @Override
    @SafeVarargs
    public final <B> UniConstraintStream<A> ifExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return new AsmUniConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <B> UniConstraintStream<A> ifExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return new AsmUniConstraintStream<>(this, new IfExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <B> UniConstraintStream<A> ifNotExists(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return new AsmUniConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUni(otherClass).getLeafNode()));
    }

    @Override
    @SafeVarargs
    public final <B> UniConstraintStream<A> ifNotExistsIncludingNullVars(Class<B> otherClass, BiJoiner<A, B>... joiners) {
        return new AsmUniConstraintStream<>(this, new IfNotExistsNode<>(JoinerDefinition.from(joiners))
                .withParents(getLeafNode(),
                        createIfExistUniWithNulls(otherClass).getLeafNode()));
    }

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        return new AsmUniConstraintStream<>(this, GroupByNode
                .builder()
                .withCollectors(collector)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
            UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_> groupBy(
                    UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC)
                .build());
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> groupBy(
                    UniConstraintCollector<A, ResultContainerA_, ResultA_> collectorA,
                    UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withCollectors(collectorA, collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(Function<A, GroupKey_> groupKeyMapping) {
        return new AsmUniConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            Function<A, GroupKey_> groupKeyMapping, UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(
                    Function<A, GroupKey_> groupKeyMapping, UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC)
                .build());
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_> groupBy(
                    Function<A, GroupKey_> groupKeyMapping, UniConstraintCollector<A, ResultContainerB_, ResultB_> collectorB,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyMapping)
                .withCollectors(collectorB, collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(Function<A, GroupKeyA_> groupKeyAMapping,
            Function<A, GroupKeyB_> groupKeyBMapping) {
        return new AsmBiConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
            UniConstraintCollector<A, ResultContainer_, Result_> collector) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collector)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    UniConstraintCollector<A, ResultContainerC_, ResultC_> collectorC,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping)
                .withCollectors(collectorC, collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
            Function<A, GroupKeyC_> groupKeyCMapping) {
        return new AsmTriConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_> groupBy(
                    Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    Function<A, GroupKeyC_> groupKeyCMapping,
                    UniConstraintCollector<A, ResultContainerD_, ResultD_> collectorD) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping)
                .withCollectors(collectorD)
                .build());
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(
                    Function<A, GroupKeyA_> groupKeyAMapping, Function<A, GroupKeyB_> groupKeyBMapping,
                    Function<A, GroupKeyC_> groupKeyCMapping, Function<A, GroupKeyD_> groupKeyDMapping) {
        return new AsmQuadConstraintStream<>(this, GroupByNode.builder()
                .withGroupers(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping)
                .build());
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(Function<A, ResultA_> mapping) {
        return new AsmUniConstraintStream<>(this, new MapNode<>(mapping));
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(Function<A, ResultA_> mappingA,
            Function<A, ResultB_> mappingB) {
        return new AsmBiConstraintStream<>(this, new MapNode<>(mappingA, mappingB));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(Function<A, ResultA_> mappingA,
            Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC) {
        return new AsmTriConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC));
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            Function<A, ResultA_> mappingA, Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC,
            Function<A, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new MapNode<>(mappingA, mappingB, mappingC, mappingD));
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> flattenLast(Function<A, Iterable<ResultA_>> mapping) {
        return new AsmUniConstraintStream<>(this, new FlattenLastNode<>(mapping));
    }

    @Override
    public UniConstraintStream<A> distinct() {
        return new AsmUniConstraintStream<>(this, new DistinctNode());
    }

    @Override
    public <ResultB_> BiConstraintStream<A, ResultB_> expand(Function<A, ResultB_> mapping) {
        return new AsmBiConstraintStream<>(this, new ExpandNode<>(mapping));
    }

    @Override
    public <ResultB_, ResultC_> TriConstraintStream<A, ResultB_, ResultC_> expand(Function<A, ResultB_> mappingB,
            Function<A, ResultC_> mappingC) {
        return new AsmTriConstraintStream<>(this, new ExpandNode<>(mappingB, mappingC));
    }

    @Override
    public <ResultB_, ResultC_, ResultD_> QuadConstraintStream<A, ResultB_, ResultC_, ResultD_> expand(
            Function<A, ResultB_> mappingB, Function<A, ResultC_> mappingC, Function<A, ResultD_> mappingD) {
        return new AsmQuadConstraintStream<>(this, new ExpandNode<>(mappingB, mappingC, mappingD));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalize(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.INTEGER, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalizeLong(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.LONG, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> penalizeBigDecimal(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.BIG_DECIMAL, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> penalizeConfigurable(ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.INTEGER,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> penalizeConfigurableLong(ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.LONG,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> penalizeConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.PENALTY, ScoreWeightType.BIG_DECIMAL,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> reward(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.INTEGER, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardLong(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.LONG, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> rewardBigDecimal(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.BIG_DECIMAL, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> rewardConfigurable(ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.INTEGER,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> rewardConfigurableLong(ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(
                        new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.LONG, matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> rewardConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.REWARD, ScoreWeightType.BIG_DECIMAL,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impact(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.INTEGER, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impactLong(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.LONG, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> impactBigDecimal(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.BIG_DECIMAL, constraintWeight,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> impactConfigurable(ToIntFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.INTEGER,
                        matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> impactConfigurableLong(ToLongFunction<A> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(
                        new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.LONG, matchWeigher)));
    }

    @Override
    public UniConstraintBuilder<A, ?> impactConfigurableBigDecimal(Function<A, BigDecimal> matchWeigher) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByConfigurableMatchWeightNode<>(ScoreImpactType.MIXED, ScoreWeightType.BIG_DECIMAL,
                        matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToIntFunction<A> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.INTEGER,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            ToLongFunction<A> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.LONG,
                        constraintWeight, matchWeigher)));
    }

    @Override
    public <Score_ extends Score<Score_>> UniConstraintBuilder<A, Score_> innerImpact(Score_ constraintWeight,
            Function<A, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        return new AsmUniConstraintBuilder<>(getConstraintFactory(),
                withChild(new ImpactByMatchWeightNode<>(scoreImpactType, ScoreWeightType.BIG_DECIMAL,
                        constraintWeight, matchWeigher)));
    }
}
