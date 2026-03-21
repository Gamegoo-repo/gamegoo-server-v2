package com.gamegoo.gamegoo_v2.account.auth.controller;

import com.gamegoo.gamegoo_v2.account.auth.annotation.AuthMember;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.AdminLoginRequest;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.RefreshTokenRequest;
import com.gamegoo.gamegoo_v2.account.auth.dto.response.AccessTokenResponse;
import com.gamegoo.gamegoo_v2.account.auth.dto.response.TokensResponse;
import com.gamegoo.gamegoo_v2.account.auth.dto.response.RejoinResponse;
import com.gamegoo.gamegoo_v2.account.auth.service.AuthFacadeService;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.RejoinRequest;
import com.gamegoo.gamegoo_v2.core.common.ApiResponse;
import com.gamegoo.gamegoo_v2.core.config.swagger.ApiErrorCodes;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotJoinResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
public class AuthController {

    private final AuthFacadeService authFacadeService;

    @PostMapping("/logout")
    @Operation(summary = "logout API 입니다.", description = "API for logout")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.INACTIVE_MEMBER
    })
    public ApiResponse<String> logout(@AuthMember Member member) {
        return ApiResponse.ok(authFacadeService.logout(member));
    }

    @PostMapping("/refresh")
    @Operation(summary = "refresh   토큰을 통한 access, refresh 토큰 재발급 API 입니다.", description = "API for Refresh Token")
    @ApiErrorCodes({
            ErrorCode.INVALID_REFRESH_TOKEN,
            ErrorCode.INVALID_SIGNATURE,
            ErrorCode.MALFORMED_TOKEN,
            ErrorCode.EXPIRED_JWT_EXCEPTION,
            ErrorCode.UNSUPPORTED_TOKEN,
            ErrorCode.INVALID_CLAIMS,
            ErrorCode.MEMBER_NOT_FOUND
    })
    public ApiResponse<AccessTokenResponse> updateToken(@Valid @RequestBody RefreshTokenRequest request,
                                                        HttpServletResponse response) {
        TokensResponse tokensResponse = authFacadeService.updateToken(request);

        // refreshToken 쿠키로 저장
        if (tokensResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", tokensResponse.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(60 * 60 * 24 * 14);
            response.addCookie(refreshCookie);
        }

        return ApiResponse.ok(AccessTokenResponse.of(tokensResponse.getId(),tokensResponse.getAccessToken()));
    }

    @DeleteMapping
    @Operation(summary = "탈퇴 API입니다.", description = "API for Blinding Member")
    @ApiErrorCodes({ErrorCode.MEMBER_NOT_FOUND})
    public ApiResponse<String> blindMember(@AuthMember Member member) {
        return ApiResponse.ok(authFacadeService.blindMember(member));
    }

    @Operation(summary = "탈퇴했던 사용자 재가입 API입니다.", description = "Rejoin API for blind member")
    @PostMapping("/rejoin")
    @ApiErrorCodes({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.ACTIVE_MEMBER,
            ErrorCode.DULPLICATED_MEMBER
    })
    public ApiResponse<RejoinResponse> rejoinMember(@RequestBody RejoinRequest rejoinRequest) {
        return ApiResponse.ok(authFacadeService.rejoinMember(rejoinRequest));
    }

    @Operation(summary = "관리자 로그인 API", description = "gameName#tag 형식의 계정과 비밀번호를 통한 로그인 방식입니다")
    @PostMapping("/admin/login")
    @ApiErrorCodes({
            ErrorCode.INVALID_ADMIN_ACCOUNT_FORMAT,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.NOT_ADMIN,
            ErrorCode.INVALID_PASSWORD
    })
    public ApiResponse<RiotJoinResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authFacadeService.adminLogin(request));
    }

}
