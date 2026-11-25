package com.example.stockanalysis.engine.screener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import static com.example.stockanalysis.engine.screener.ScreenerTypes.Transition;
import static com.example.stockanalysis.engine.screener.ScreenerResult.StagePoint;

public final class StageHistoryAnalyzer {
    public record History(Integer previousBarStage, Integer previousDistinctStage, Integer barsInStage,
            boolean durationComplete, LocalDateTime startedAt, boolean changed,
            Transition transition, Transition lastTransition, Integer barsSinceTransition) {}
    public History analyze(List<StagePoint> points) {
        StageAnalyzer analyzer = new StageAnalyzer();
        int last = points.size() - 1;
        Integer current = last < 0 ? null : points.get(last).stage();
        Integer previous = last > 0 ? points.get(last - 1).stage() : null;
        if (current == null) return new History(previous, null, null, false, null, false, Transition.UNKNOWN, Transition.UNKNOWN, null);
        int first = last;
        while (first > 0 && Objects.equals(points.get(first - 1).stage(), current)) first--;
        Integer distinct = first > 0 ? points.get(first - 1).stage() : null;
        // A warmup/boundary point breaks continuity; do not invent a crossing through it.
        boolean complete = distinct != null;
        Transition transition = analyzer.transition(previous, current);
        return new History(previous, distinct, last - first + 1, complete,
                complete ? points.get(first).at() : null,
                previous != null && !previous.equals(current), transition,
                analyzer.transition(distinct, current), complete ? last - first : null);
    }
}
