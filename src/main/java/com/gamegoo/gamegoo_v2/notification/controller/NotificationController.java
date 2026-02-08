package com.gamegoo.gamegoo_v2.notification.controller;

import com.gamegoo.gamegoo_v2.account.auth.annotation.AuthMember;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.core.common.ApiResponse;
import com.gamegoo.gamegoo_v2.core.common.annotation.ValidCursor;
import com.gamegoo.gamegoo_v2.core.common.annotation.ValidPage;
import com.gamegoo.gamegoo_v2.core.config.swagger.ApiErrorCodes;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.notification.dto.NotificationCursorListResponse;
import com.gamegoo.gamegoo_v2.notification.dto.NotificationMultiReadRequest;
import com.gamegoo.gamegoo_v2.notification.dto.NotificationPageListResponse;
import com.gamegoo.gamegoo_v2.notification.dto.ReadNotificationResponse;
import com.gamegoo.gamegoo_v2.notification.service.NotificationFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "Notification 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/notification")
@Validated
public class NotificationController {

    private final NotificationFacadeService notificationFacadeService;

    @Operation(summary = "알림 읽음 처리 API", description = "특정 알림을 읽음 처리하는 API 입니다.")
    @Parameter(name = "notificationId", description = "읽음 처리할 알림의 id 입니다.")
    @PatchMapping("/{notificationId}")
    @ApiErrorCodes({ErrorCode.NOTIFICATION_NOT_FOUND})
    public ApiResponse<ReadNotificationResponse> readNotification(
            @PathVariable(name = "notificationId") Long notificationId, @AuthMember Member member) {
        return ApiResponse.ok(notificationFacadeService.readNotification(member, notificationId));
    }

    @Operation(summary = "안읽은 알림 개수 조회 API", description = "해당 회원의 안읽은 알림의 개수를 조회하는 API 입니다.")
    @GetMapping("/unread/count")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.INACTIVE_MEMBER
    })
    public ApiResponse<Integer> getUnreadNotificationCount(@AuthMember Member member) {
        return ApiResponse.ok(notificationFacadeService.countUnreadNotification(member));
    }

    @Operation(summary = "알림 전체 목록 조회 API", description = "알림 전체보기 화면에서 알림 목록을 조회하는 API 입니다.")
    @Parameter(name = "page", description = "페이지 번호, 1 이상의 숫자를 입력해 주세요.")
    @GetMapping("/total")
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.INACTIVE_MEMBER,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<NotificationPageListResponse> getNotificationListByPage(
            @ValidPage @RequestParam(name = "page") Integer page, @AuthMember Member member) {
        return ApiResponse.ok(notificationFacadeService.getNotificationPageList(member, page));
    }

    @Operation(summary = "알림 팝업 목록 조회 API", description = "알림 팝업 화면에서 알림 목록을 조회하는 API 입니다.")
    @Parameter(name = "cursor", description = "페이징을 위한 커서, Long 타입 notificationId를 보내주세요. " +
            "보내지 않으면 가장 최근 알림 10개를 조회합니다.")
    @GetMapping
    @ApiErrorCodes({
            ErrorCode.UNAUTHORIZED_EXCEPTION,
            ErrorCode.MEMBER_NOT_FOUND,
            ErrorCode.INACTIVE_MEMBER,
            ErrorCode._BAD_REQUEST
    })
    public ApiResponse<NotificationCursorListResponse> getNotificationListByCursor(
            @ValidCursor @RequestParam(name = "cursor", required = false) Long cursor,
            @AuthMember Member member) {
        return ApiResponse.ok(notificationFacadeService.getNotificationCursorList(member, cursor));
    }

    @Operation(summary = "여러 알림 읽음 처리 API", description = "여러 알림을 한 번에 읽음 처리하는 API 입니다.")
    @PatchMapping
    @ApiErrorCodes({ErrorCode.NOTIFICATION_NOT_FOUND})
    public ApiResponse<String> readMultipleNotifications(
            @RequestBody NotificationMultiReadRequest notificationMultiReadRequest,
            @AuthMember Member member) {
        return ApiResponse.ok(notificationFacadeService.readMultipleNotifications(member, notificationMultiReadRequest.getNotificationIds()));
    }
}
