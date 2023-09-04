package ai.timefold.solver.core.impl.testdata.domain.list.shadow_history;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public class TestdataListWithShadowHistoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListSolutionWithShadowHistory, SimpleScore> {

    public SimpleScore calculateScore(TestdataListSolutionWithShadowHistory testdataListSolutionWithShadowHistory) {
        int score = 0;
        for (var e : testdataListSolutionWithShadowHistory.getEntityList()) {
            score -= (int) Math.pow(e.getValueList().size(), 2);
        }
        return SimpleScore.of(score);
    }
}
