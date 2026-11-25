package com.example.stockanalysis.analysis.api;

import com.example.stockanalysis.analysis.application.AnalysisExecutionCommand;
import com.example.stockanalysis.engine.model.AnalysisMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AnalysisExecutionRequest(
        @Schema(description = "분석 모드", example = "SWING") @NotNull AnalysisMode analysisMode,
        @Schema(description = "분석 기준일. 생략 시 서버의 현재 날짜 기준", example = "2026-11-13") LocalDate baseDate,
        @Schema(description = "저장 결과를 재사용하지 않고 재계산할지 여부. 생략 시 false", example = "false") Boolean forceRecalculate,
        @Schema(description = "선택 보유 포지션 문맥") com.example.stockanalysis.engine.model.PositionContext position
) {
    public AnalysisExecutionRequest(AnalysisMode analysisMode, LocalDate baseDate, Boolean forceRecalculate) {
        this(analysisMode, baseDate, forceRecalculate, null);
    }

    AnalysisExecutionCommand toCommand() {
        return new AnalysisExecutionCommand(
                analysisMode,
                baseDate,
                Boolean.TRUE.equals(forceRecalculate),
                null,
                position
        );
    }
}
