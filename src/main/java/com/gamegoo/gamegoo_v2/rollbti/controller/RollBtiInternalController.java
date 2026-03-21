package com.gamegoo.gamegoo_v2.rollbti.controller;

import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.core.common.ApiResponse;
import com.gamegoo.gamegoo_v2.core.config.swagger.ApiErrorCodes;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiCompatibilityOrder;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiRecommendationBucket;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiEventRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiGuestResultSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiCompatibilityResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiGuestResultResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiGuestResultSaveResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiParticipationCountResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiProfileResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationCursorResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationCursorResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiTypeSummaryResponse;
import com.gamegoo.gamegoo_v2.rollbti.service.RollBtiFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RollBTI Internal", description = "RollBTI integration API")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v2/internal/roll-bti")
public class RollBtiInternalController {

    private final RollBtiFacadeService rollBtiFacadeService;

    @Operation(summary = "특정 회원 롤BTI 타입 저장 API",
            description = "외부 롤BTI 서비스가 회원가입 완료 이후 memberId를 기반으로 타입을 저장할 때 사용합니다.")
    @PostMapping("/members/{memberId}")
    @ApiErrorCodes({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiProfileResponse> saveTypeByMemberId(
            @PathVariable Long memberId,
            @Valid @RequestBody RollBtiSaveRequest request) {
        return ApiResponse.ok(rollBtiFacadeService.saveTypeByMemberId(memberId, request));
    }

    @Operation(summary = "특정 회원 롤BTI 타입 조회 API", description = "memberId 기준 저장된 타입을 조회합니다.")
    @GetMapping("/members/{memberId}")
    @ApiErrorCodes({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.ROLL_BTI_PROFILE_NOT_FOUND
    })
    public ApiResponse<RollBtiProfileResponse> getTypeByMemberId(@PathVariable Long memberId) {
        return ApiResponse.ok(rollBtiFacadeService.getTypeByMemberId(memberId));
    }

    @Operation(summary = "롤BTI 타입 상세 조회 API", description = "타입 설명, 궁합 good/bad, 라인 추천 챔피언을 반환합니다.")
    @GetMapping("/types/{type}")
    @ApiErrorCodes({ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED})
    public ApiResponse<RollBtiTypeSummaryResponse> getTypeSummary(@PathVariable RollBtiType type) {
        return ApiResponse.ok(rollBtiFacadeService.getTypeSummary(type));
    }

    @Operation(summary = "롤BTI 타입 궁합 조회 API", description = "타입별 good/bad 궁합 목록만 반환합니다.")
    @GetMapping("/types/{type}/compatibility")
    @ApiErrorCodes({ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED})
    public ApiResponse<RollBtiCompatibilityResponse> getCompatibility(@PathVariable RollBtiType type) {
        return ApiResponse.ok(rollBtiFacadeService.getCompatibility(type));
    }

    @Operation(summary = "타입 기반 추천 유저 조회 API",
            description = "type 기준으로 롤BTI 회원 카드를 궁합 점수 순으로 반환합니다.")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @Parameter(name = "page", description = "조회 페이지(기본 1)")
    @Parameter(name = "compatibilityOrder", description = "궁합 정렬 순서(HIGH, LOW)")
    @Parameter(name = "tier", description = "티어 필터")
    @Parameter(name = "excludeMemberId", description = "추천에서 제외할 memberId(선택)")
    @GetMapping("/types/{type}/recommendations")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED,
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST
    })
    public ApiResponse<RollBtiRecommendationResponse> getRecommendationsByType(
            @PathVariable RollBtiType type,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) RollBtiCompatibilityOrder compatibilityOrder,
            @RequestParam(required = false) Tier tier,
            @RequestParam(required = false)
            @Min(value = 1, message = "excludeMemberId는 1 이상이어야 합니다.")
            Long excludeMemberId) {
        return ApiResponse.ok(
                rollBtiFacadeService.getRecommendationsByType(
                        type, size, page, compatibilityOrder, tier, excludeMemberId));
    }

    @Operation(summary = "타입 기반 추천 유저 무한스크롤 조회 API",
            description = "type 기준으로 롤BTI 회원 카드를 커서 방식으로 반환합니다.")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @Parameter(name = "cursorMemberId", description = "이전 응답의 마지막 memberId")
    @Parameter(name = "compatibilityOrder", description = "궁합 정렬 순서(HIGH, LOW)")
    @Parameter(name = "tier", description = "티어 필터")
    @Parameter(name = "excludeMemberId", description = "추천에서 제외할 memberId(선택)")
    @GetMapping("/types/{type}/recommendations/cursor")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED,
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiRecommendationCursorResponse> getRecommendationsByTypeWithCursor(
            @PathVariable RollBtiType type,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @Min(1) Long cursorMemberId,
            @RequestParam(required = false) RollBtiCompatibilityOrder compatibilityOrder,
            @RequestParam(required = false) Tier tier,
            @RequestParam(required = false)
            @Min(value = 1, message = "excludeMemberId는 1 이상이어야 합니다.")
            Long excludeMemberId) {
        return ApiResponse.ok(
                rollBtiFacadeService.getRecommendationsByTypeWithCursor(
                        type, size, cursorMemberId, compatibilityOrder, tier, excludeMemberId));
    }

    @Operation(summary = "타입 기반 추천 버킷 무한스크롤 조회 API",
            description = "type 기준으로 GOOD/NORMAL/BAD 버킷별 롤BTI 회원 카드를 커서 방식으로 반환합니다.")
    @Parameter(name = "bucket", description = "추천 버킷(GOOD, NORMAL, BAD)")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @Parameter(name = "cursorMemberId", description = "이전 응답의 마지막 memberId")
    @Parameter(name = "tier", description = "티어 필터")
    @Parameter(name = "excludeMemberId", description = "추천에서 제외할 memberId(선택)")
    @GetMapping("/types/{type}/recommendations/{bucket}/cursor")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_TYPE_NOT_SUPPORTED,
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiRecommendationCursorResponse> getRecommendationsByTypeAndBucketWithCursor(
            @PathVariable RollBtiType type,
            @PathVariable RollBtiRecommendationBucket bucket,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @Min(1) Long cursorMemberId,
            @RequestParam(required = false) Tier tier,
            @RequestParam(required = false)
            @Min(value = 1, message = "excludeMemberId는 1 이상이어야 합니다.")
            Long excludeMemberId) {
        return ApiResponse.ok(
                rollBtiFacadeService.getRecommendationsByTypeAndBucketWithCursor(
                        type, bucket, size, cursorMemberId, tier, excludeMemberId));
    }

    @Operation(summary = "비회원 롤BTI 회원 카드 목록 조회 API",
            description = "비회원 롤BTI 피드에서 타입 없이 전체 회원 카드를 이름순으로 반환합니다.")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @Parameter(name = "page", description = "조회 페이지(기본 1)")
    @Parameter(name = "tier", description = "티어 필터")
    @GetMapping("/recommendations")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiPublicRecommendationResponse> getPublicRecommendations(
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Tier tier) {
        return ApiResponse.ok(rollBtiFacadeService.getPublicRecommendations(size, page, tier));
    }

    @Operation(summary = "비회원 롤BTI 회원 카드 무한스크롤 조회 API",
            description = "비회원 롤BTI 피드에서 타입 없이 전체 회원 카드를 이름순으로 커서 방식 반환합니다.")
    @Parameter(name = "size", description = "조회 개수(기본 20, 최대 50)")
    @Parameter(name = "cursorMemberId", description = "이전 응답의 마지막 memberId")
    @Parameter(name = "tier", description = "티어 필터")
    @GetMapping("/recommendations/cursor")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiPublicRecommendationCursorResponse> getPublicRecommendationsWithCursor(
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) @Min(1) Long cursorMemberId,
            @RequestParam(required = false) Tier tier) {
        return ApiResponse.ok(rollBtiFacadeService.getPublicRecommendationsWithCursor(size, cursorMemberId, tier));
    }

    @Operation(summary = "롤BTI 누적 참여 인원 조회 API",
            description = "complete_test 이벤트를 기준으로 누적 참여 인원 수를 반환합니다.")
    @GetMapping("/participants")
    public ApiResponse<RollBtiParticipationCountResponse> getParticipationCount() {
        return ApiResponse.ok(rollBtiFacadeService.getParticipationCount());
    }

    @Operation(summary = "비회원 롤BTI 결과 저장 API",
            description = "비회원도 결과 payload를 저장하고 공유용 resultId를 발급받을 수 있습니다.")
    @PostMapping("/results")
    @ApiErrorCodes({
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<RollBtiGuestResultSaveResponse> saveGuestResult(
            @Valid @RequestBody RollBtiGuestResultSaveRequest request) {
        return ApiResponse.ok(rollBtiFacadeService.saveGuestResult(request));
    }

    @Operation(summary = "비회원 롤BTI 결과 조회 API",
            description = "공유용 resultId로 저장된 결과 payload를 조회합니다.")
    @GetMapping("/results/{resultId}")
    @ApiErrorCodes({
            ErrorCode.ROLL_BTI_RESULT_NOT_FOUND
    })
    public ApiResponse<RollBtiGuestResultResponse> getGuestResult(@PathVariable String resultId) {
        return ApiResponse.ok(rollBtiFacadeService.getGuestResultByResultId(resultId));
    }

    @Operation(summary = "롤BTI 이벤트 적재 API",
            description = "signup_complete, go_to_gamegoo 등 핵심 이벤트를 적재합니다.")
    @PostMapping("/events")
    @ApiErrorCodes({
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<String> trackEvent(@Valid @RequestBody RollBtiEventRequest request) {
        return ApiResponse.ok(rollBtiFacadeService.trackEvent(request));
    }
}
