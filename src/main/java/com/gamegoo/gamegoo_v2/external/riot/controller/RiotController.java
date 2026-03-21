package com.gamegoo.gamegoo_v2.external.riot.controller;

import com.gamegoo.gamegoo_v2.core.common.ApiResponse;
import com.gamegoo.gamegoo_v2.core.config.swagger.ApiErrorCodes;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.external.riot.dto.request.RiotJoinRequest;
import com.gamegoo.gamegoo_v2.external.riot.dto.request.RiotVerifyExistUserRequest;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RSOLoginResponse;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotJoinResponse;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Riot", description = "Riot 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/riot")
@Validated
public class RiotController {

    private final RiotFacadeService riotFacadeService;

    @PostMapping("/verify")
    @Operation(summary = "실제 존재하는 Riot 계정인지 검증하는 API", description = "API for verifying account by riot API")
    @ApiErrorCodes({
            ErrorCode.RIOT_INVALID_API_KEY,
            ErrorCode.RIOT_NOT_FOUND,
            ErrorCode.RIOT_API_ERROR,
            ErrorCode.RIOT_SERVER_ERROR,
            ErrorCode.RIOT_NETWORK_ERROR,
            ErrorCode.RIOT_UNKNOWN_ERROR
    })
    public ApiResponse<String> VerifyRiot(@RequestBody @Valid RiotVerifyExistUserRequest request) {
        return ApiResponse.ok(riotFacadeService.verifyRiotAccount(request));
    }

    @PostMapping("/join")
    @Operation(summary = "RSO 전용 회원가입 API", description = "API for RSO join")
    @ApiErrorCodes({
            ErrorCode.MEMBER_ALREADY_EXISTS,
            ErrorCode.RIOT_INVALID_API_KEY,
            ErrorCode.RIOT_NOT_FOUND,
            ErrorCode.RIOT_API_ERROR,
            ErrorCode.RIOT_SERVER_ERROR,
            ErrorCode.RIOT_NETWORK_ERROR,
            ErrorCode.RIOT_UNKNOWN_ERROR
    })
    public ApiResponse<RiotJoinResponse> joinByRSO(@RequestBody @Valid RiotJoinRequest request) {
        return ApiResponse.ok(riotFacadeService.join(request));
    }

    @GetMapping("/oauth/callback")
    @Operation(summary = "Riot OAuth 인증 코드 콜백 처리")
    @ApiErrorCodes({
            ErrorCode.RIOT_API_ERROR,
            ErrorCode.RSO_NO_STATE,
            ErrorCode.STATE_WRONG_DECODE,
            ErrorCode.INACTIVE_MEMBER
    })
    public ResponseEntity<RSOLoginResponse> handleRSOCallback(@RequestParam("code") String code,
                                                              @RequestParam(required = false) String state,
                                                              HttpServletResponse response) {

        RSOLoginResponse rsoLoginResponse = riotFacadeService.processOAuthCallback(code, state);

        // refreshToken 쿠키로 저장
        if (rsoLoginResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", rsoLoginResponse.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(60 * 60 * 24 * 14);
            response.addCookie(refreshCookie);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", rsoLoginResponse.getRedirectUrl())
                .build();
    }

}
