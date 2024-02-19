package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomListChangeIteratorTest {

    @Test
    void iterator() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListEntity a = TestdataListEntity.createWithValues("A", v1, v2);
        TestdataListEntity b = TestdataListEntity.createWithValues("B");
        TestdataListEntity c = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        // Iterates over values in this given order.
        EntityIndependentValueSelector<TestdataListSolution> sourceValueSelector =
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3);
        EntityIndependentValueSelector<TestdataListSolution> destinationValueSelector =
                mockEntityIndependentValueSelector(listVariableDescriptor, v2, v3);
        EntitySelector<TestdataListSolution> entitySelector = mockEntitySelector(b, a, c);
        ElementDestinationSelector<TestdataListSolution> destinationSelector =
                new ElementDestinationSelector<>(entitySelector, destinationValueSelector, true);
        RandomListChangeIterator<TestdataListSolution> randomListChangeIterator = new RandomListChangeIterator<>(
                scoreDirector.getSupplyManager().demand(listVariableDescriptor.getProvidedDemand()),
                sourceValueSelector,
                destinationSelector);

        // <3 => entity selector; >=3 => value selector
        TestRandom random = new TestRandom(3, 0, 1);
        final long destinationRange = entitySelector.getSize() + destinationValueSelector.getSize();

        solvingStarted(destinationSelector, scoreDirector, random);

        // The moved values (1, 2, 3) and their source positions are supplied by the mocked value selector.
        // The test is focused on the destinations (A[2], B[0], A[0]), which reflect the numbers supplied by the test random.
        assertCodesOfIterator(randomListChangeIterator,
                "1 {A[0]->A[2]}",
                "2 {A[1]->B[0]}",
                "3 {C[0]->A[0]}");

        random.assertIntBoundJustRequested((int) destinationRange);
    }
}
