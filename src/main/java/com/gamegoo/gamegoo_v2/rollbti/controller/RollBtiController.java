package com.gamegoo.gamegoo_v2.rollbti.controller;

import com.gamegoo.gamegoo_v2.account.auth.annotation.AuthMember;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.core.common.ApiResponse;
import com.gamegoo.gamegoo_v2.core.config.swagger.ApiErrorCodes;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiProfileResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.service.RollBtiFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RollBTI", description = "RollBTI member API")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v2/roll-bti")
public class RollBtiController {

    private final RollBtiFacadeService rollBtiFacadeService;

    @Operation(summary = "내 롤BTI 타입 저장 API", description = "회원의 롤BTI 결과 타입을 저장하거나 수정합니다.")
    @PostMapping("/me")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiProfileResponse> saveMyType(@AuthMember Member member,
                                                          @Valid @RequestBody RollBtiSaveRequest request) {
        return ApiResponse.ok(rollBtiFacadeService.saveMyType(member, request));
    }

    @Operation(summary = "내 롤BTI 타입 조회 API", description = "저장된 회원의 롤BTI 타입 정보를 조회합니다.")
    @GetMapping("/me")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.ROLL_BTI_PROFILE_NOT_FOUND
    })
    public ApiResponse<RollBtiProfileResponse> getMyType(@AuthMember Member member) {
        return ApiResponse.ok(rollBtiFacadeService.getMyType(member));
    }

    @Operation(summary = "내 롤BTI 기반 추천 API",
            description = "회원의 롤BTI 타입 기반으로 최근 게시글 유저를 궁합 점수 순으로 추천합니다.")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @GetMapping("/me/recommendations")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.ROLL_BTI_PROFILE_NOT_FOUND,
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST
    })
    public ApiResponse<RollBtiRecommendationResponse> getMyRecommendations(@AuthMember Member member,
                                                                           @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(rollBtiFacadeService.getMyRecommendations(member, size));
    }
}
