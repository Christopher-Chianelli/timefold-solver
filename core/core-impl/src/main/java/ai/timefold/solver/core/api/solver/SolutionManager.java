package ai.timefold.solver.core.api.solver;

import static ai.timefold.solver.core.api.solver.SolutionUpdatePolicy.UPDATE_ALL;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.impl.solver.DefaultSolutionManager;

/**
 * A stateless service to help calculate {@link Score}, {@link ConstraintMatchTotal},
 * {@link Indictment}, etc.
 * <p>
 * To create a {@link SolutionManager} instance, use {@link #create(SolverFactory)}.
 * <p>
 * These methods are thread-safe unless explicitly stated otherwise.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the actual score type
 */
public interface SolutionManager<Solution_, Score_ extends Score<Score_>> {

    // ************************************************************************
    // Static creation methods: SolverFactory
    // ************************************************************************

    /**
     * Uses a {@link SolverFactory} to build a {@link SolutionManager}.
     *
     * @param solverFactory never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     */
    static <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> create(
            SolverFactory<Solution_> solverFactory) {
        return new DefaultSolutionManager<>(solverFactory);
    }

    /**
     * Uses a {@link SolverManager} to build a {@link SolutionManager}.
     *
     * @param solverManager never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, Score_ extends Score<Score_>, ProblemId_> SolutionManager<Solution_, Score_> create(
            SolverManager<Solution_, ProblemId_> solverManager) {
        return new DefaultSolutionManager<>(solverManager);
    }

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * As defined by {@link #update(Object, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     *
     */
    default Score_ update(Solution_ solution) {
        return update(solution, UPDATE_ALL);
    }

    /**
     * Updates the given solution according to the {@link SolutionUpdatePolicy}.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return possibly null if already null and {@link SolutionUpdatePolicy} didn't cause its update
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #explain(Object)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default ScoreExplanation<Solution_, Score_> explain(Solution_ solution) {
        return explain(solution, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves {@link ConstraintMatchTotal}s and {@link Indictment}s necessary for describing the
     * quality of a particular solution.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return never null
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * Quickly runs through all possible options of fitting a given fittedElement in a given solution,
     * and returns a list of recommendations sorted by the difference in the score after fitting the element.
     * Does not call local search, runs a greedy algorithm instead.
     *
     * <p>
     * For problems with only basic planning variables or with chained planning variables,
     * the element is the planning entity of the problem,
     * and each available planning value will be tested for fit
     * by setting it to the planning variable in question.
     * For problems with a list variable, the element is a shadow entity,
     * and it will be tested for fit in each position of the planning list variable.
     *
     * <p>
     * When an element is tested for fit, a score is calculated over the entire solution with the element in place,
     * also called a placement.
     * The proposition function is also called at that time,
     * allowing the user to extract any information from the current placement;
     * the extracted information is called the proposition.
     * After the proposition is extracted, the solution is returned to its original state,
     * resetting all changes made by the fitting.
     * This has consequences for the proposition:
     *
     * <ul>
     * <li>If the element is an immutable object,
     * the proposition will remain unchanged after the placement is discarded.</li>
     * <li>If the element is mutable, such as a planning entity,
     * the proposition extracted from the element contains live data
     * and that data will be erased when the next placement is tested for fit.
     * This means that the proposition function needs to make defensive copies of everything it wants to return,
     * such as values of shadow variables etc.
     * The argument of the proposition function will be a {@link SolutionCloner planning clone} of the original fittedElement,
     * and every reference from it will also be planning-cloned.</li>
     * </ul>
     *
     * <p>
     * Example: Consider a planning entity Shift, with a variable "employee" and a shadow variable "hourlyCost".
     * Let's assume we have two employees to test for fit, Alice and Bob.
     * Alice has an hourly cost of 10, Bob has an hourly cost of 20.
     * The proposition function will be called twice, once with Shift(Alice) and once with Shift(Bob).
     * Let's assume the proposition function returns the Shift instance in its entirety.
     * This is what will happen:
     *
     * <ol>
     * <li>Calling propositionFunction on Shift(Ann) results in proposition P1: Shift(Ann.)</li>
     * <li>Placement is cleared, Shift(Bob) is now tested for fit.</li>
     * <li>Calling propositionFunction on Shift(Bob) results in proposition P2: Shift(Bob.)</li>
     * <li>
     * Proposition P1 (originally Shift(Ann)) is now also Shift(Bob), P1 equals P2.
     * This is because both propositions operate on the same object, and therefore share the same state.
     * </li>
     * <li>
     * Then placement is cleared, both elements have been tested for fit,
     * solution is returned to its original order.
     * The propositions are then returned to the user, who notices that both P1 and P2 are Shift(null).
     * This is because they shared state,
     * and the original state of the solution was for Shift to be unassigned.
     * </li>
     * </ol>
     *
     * If instead the proposition function returned Ann and Bob, immutable planning variables,
     * this problem would have been avoided.
     * Alternatively, the proposition function could have returned a defensive copy of the Shift.
     * <p>
     * Once all placements have been tested for fit,
     * a final collection of propositions is returned.
     * It is sorted from best fit to worst,
     * the proposition with the smallest score difference will come first.
     * Every proposition will be in a state as if the solution was never changed,
     * that means that no variables will be set and shadow variables will be initialized.
     * The input solution will be unchanged.
     *
     * @param solution never null
     * @param fittedElement never null
     * @param propositionFunction never null
     * @return never null, sorted from best to worst
     * @param <Element_> generic type of the unassigned element
     * @param <Proposition_> generic type of the user-provided proposition
     */
    <Element_, Proposition_> List<RecommendedFit<Proposition_, Score_>> recommendFit(Solution_ solution, Element_ fittedElement,
            Function<Element_, Proposition_> propositionFunction);

}
