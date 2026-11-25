package com.example.stockanalysis.engine.screener;

public final class ScreenerTypes {
    private ScreenerTypes() {}
    public enum Mode { SWING, SHORT_TERM }
    public enum Quality { VALID, BOUNDARY, INSUFFICIENT_DATA }
    public enum Transition { SAME, FORWARD, REVERSE, SKIPPED, UNKNOWN }
    public enum Direction { STRONG_UP, UP, FLAT, DOWN, STRONG_DOWN }
    public enum GapState { EXPANDING, CONTRACTING, FLAT, CROSSING, UNKNOWN }
    public enum EvaluationStatus { NOT_EVALUATED }
    public enum Coverage { FULL_WINDOW, HISTORY_SHORT, NO_DATA }
}
