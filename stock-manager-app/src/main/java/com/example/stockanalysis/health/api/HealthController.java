package com.example.stockanalysis.health.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.stockanalysis.shared.api.ApiResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "서비스 상태")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");
    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @Operation(summary = "서비스 상태 조회", description = "서비스 상태, 애플리케이션 이름, 활성 프로필과 한국시간을 반환합니다.")
    @GetMapping
    public ApiResponse<HealthResponse> health() {
        String profile = Arrays.stream(environment.getActiveProfiles())
                .findFirst()
                .orElse("default");
        HealthResponse response = new HealthResponse(
                "UP",
                "stock-analysis",
                profile,
                LocalDateTime.now(SERVICE_ZONE_ID)
        );
        return ApiResponse.success(response);
    }
}
