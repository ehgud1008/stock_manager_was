package com.example.stockanalysis.shared.config;

import com.example.stockanalysis.decision.DecisionController;
import com.example.stockanalysis.shared.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockManagerOpenApi() {
        String errors = Arrays.stream(ErrorCode.values())
                .filter(code -> code != ErrorCode.SUCCESS)
                .map(code -> "- HTTP " + code.getHttpStatus().value() + " / `" + code.name()
                        + "`: " + code.getDefaultMessage())
                .collect(Collectors.joining("\n"));
        return new OpenAPI()
                .info(new Info().title("Stock Manager API").version("v1")
                        .description("국내 주식 조회·분석·스크리너·보고서·보유종목 API입니다.\n\n"
                                + "모든 업무 응답은 `success`, `code`, `message`, `data` 형식입니다. "
                                + "실패 시 `success=false`, `data=null`이며 `code`로 원인을 구분합니다.\n\n"
                                + "비동기 API의 202 응답 이후에는 해당 실행 조회 API로 진행 상태를 확인하세요. "
                                + "`Idempotency-Key`의 필수 여부는 각 API의 헤더 정의를 따릅니다. "
                                + "동일한 작업을 재전송할 때는 같은 키를 사용합니다.\n\n"
                                + "날짜는 `yyyy-MM-dd`, LocalDateTime은 한국시간 기준 "
                                + "`yyyy-MM-dd'T'HH:mm:ss`입니다. 페이지 번호는 0부터 시작합니다.\n\n"
                                + "기능 설정과 DB 설치 여부에 따라 일부 기능은 사용할 수 없습니다. "
                                + "Swagger의 실행 기능은 실제 API를 호출합니다.\n\n공통 오류 코드:\n" + errors))
                .components(new Components().addSchemas("ApiError", new ObjectSchema()
                        .addProperty("success", new BooleanSchema().example(false))
                        .addProperty("code", new StringSchema().description("오류 코드 (API 소개의 HTTP 상태별 목록 참고)"))
                        .addProperty("message", new StringSchema().description("오류 설명"))
                        .addProperty("data", new ObjectSchema().nullable(true).description("실패 응답에서는 null"))
                        .required(Arrays.asList("success", "code", "message", "data"))));
    }

    @Bean
    public OperationCustomizer documentCommonApiDetails() {
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("stockCode", "종목코드. 예: 005930"),
                Map.entry("code", "종목코드. 예: 005930"),
                Map.entry("page", "0부터 시작하는 페이지 번호"),
                Map.entry("size", "페이지당 조회 건수. 기본값과 허용 범위는 API별로 다릅니다."),
                Map.entry("Idempotency-Key", "중복 요청 방지 키. 동일 작업 재전송 시 같은 키를 사용합니다."),
                Map.entry("sourceRunId", "원본 종합분석 실행 ID"),
                Map.entry("search", "종목코드 또는 종목명 검색어"),
                Map.entry("minTradingAmountEok", "최소 거래대금 (억원)"),
                Map.entry("minTradingAmountMillionWon", "최소 거래대금 (백만원)"),
                Map.entry("minVolume", "최소 거래량 (주)"),
                Map.entry("from", "조회 시작일 (yyyy-MM-dd)"),
                Map.entry("to", "조회 종료일 (yyyy-MM-dd)"));
        return (operation, handlerMethod) -> {
            operation.getResponses().addApiResponse("default", new ApiResponse()
                    .description("오류 응답. HTTP 상태와 code는 API 소개의 공통 오류 코드 목록을 참고하세요.")
                    .content(new Content().addMediaType("application/json", new MediaType()
                            .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ApiError"))
                            .example(Map.of("success", false, "code", "INVALID_REQUEST",
                                    "message", "요청값이 올바르지 않습니다.", "data", com.fasterxml.jackson.databind.node.NullNode.instance)))));
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(parameter -> {
                    if (parameter.getDescription() == null && descriptions.containsKey(parameter.getName())) {
                        parameter.setDescription(descriptions.get(parameter.getName()));
                    }
                    // MVC accepts an absent key so the service can return its business error envelope.
                    // These decision operations nevertheless require a valid key to execute.
                    if (handlerMethod.getBeanType() == DecisionController.class
                            && "Idempotency-Key".equals(parameter.getName())) {
                        parameter.setRequired(true);
                        parameter.setDescription("필수 중복 요청 방지 키. 영문·숫자·마침표·밑줄·콜론·하이픈 1~100자.");
                        parameter.setSchema(new StringSchema().minLength(1).maxLength(100)
                                .pattern("[A-Za-z0-9._:-]{1,100}"));
                    }
                });
            }
            return operation;
        };
    }
}
