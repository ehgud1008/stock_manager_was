package com.example.stockanalysis.holdings;

import com.example.stockanalysis.shared.api.ApiResponse;
import com.example.stockanalysis.shared.exception.BusinessException;
import com.example.stockanalysis.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name="키움 계좌 조회")
@RequestMapping("/api/v1/broker-account")
public class BrokerAccountController {
    private static final String ACCESS_HEADER="X-Broker-Access-Key";
    private final BrokerAccountService service;
    private final byte[] accessKey;

    public BrokerAccountController(BrokerAccountService service, @Value("${broker-account.access-key:}") String accessKey) {
        this.service=service;
        this.accessKey=accessKey.getBytes(StandardCharsets.UTF_8);
    }

    @Operation(summary="키움 예수금·주문 가능 금액과 수동 등록 현금 조회")
    @GetMapping("/cash")
    public ResponseEntity<ApiResponse<BrokerAccountService.Cash>> cash(@RequestHeader(value=ACCESS_HEADER,required=false) String key,
            HttpServletRequest request) {
        check(key,request);
        return response(service.cash());
    }

    @Operation(summary="키움 보유 수량·평가손익과 수동 보유정보 조회")
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<BrokerAccountService.Positions>> positions(@RequestHeader(value=ACCESS_HEADER,required=false) String key,
            @RequestParam(required=false) String cursor,HttpServletRequest request) {
        check(key,request);
        return response(service.positions(cursor));
    }

    @Operation(summary="지정 주문일의 키움 체결 내역 조회")
    @GetMapping("/fills")
    public ResponseEntity<ApiResponse<BrokerAccountService.Fills>> fills(@RequestHeader(value=ACCESS_HEADER,required=false) String key,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestParam(required=false) String cursor,
            HttpServletRequest request) {
        check(key,request);
        return response(service.fills(date,cursor));
    }

    @Operation(summary="기간별 키움 실현손익과 수수료·세금 조회")
    @GetMapping("/pnl")
    public ResponseEntity<ApiResponse<BrokerAccountService.Pnl>> pnl(@RequestHeader(value=ACCESS_HEADER,required=false) String key,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) String cursor,
            HttpServletRequest request) {
        check(key,request);
        return response(service.pnl(from,to,cursor));
    }

    private void check(String key,HttpServletRequest request) {
        String remote=request.getRemoteAddr();
        boolean local="127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
        if (accessKey.length<32 || key==null || key.length()>512
                || !MessageDigest.isEqual(accessKey,key.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"계좌 조회 연결 설정과 접근 키를 확인하세요.");
        }
        if (!request.isSecure() && !local) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,"계좌 조회는 HTTPS 또는 로컬 연결에서 사용하세요.");
        }
    }

    private static <T> ResponseEntity<ApiResponse<T>> response(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(data));
    }
}
