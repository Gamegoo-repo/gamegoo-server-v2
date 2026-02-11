package com.gamegoo.gamegoo_v2.notification.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.notification.domain.Notification;
import com.gamegoo.gamegoo_v2.notification.dto.NotificationCursorListResponse;
import com.gamegoo.gamegoo_v2.notification.dto.NotificationPageListResponse;
import com.gamegoo.gamegoo_v2.notification.dto.ReadNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationFacadeService {

    private final NotificationService notificationService;

    /**
     * 알림 읽음 처리 Facade 메소드
     *
     * @param member         회원
     * @param notificationId 알림 id
     * @return ReadNotificationResponse
     */
    @Transactional
    public ReadNotificationResponse readNotification(Member member, Long notificationId) {
        Notification notification = notificationService.readNotification(member, notificationId);

        return ReadNotificationResponse.of(notification.getId());
    }

    /**
     * 안읽은 알림 개수 조회 Facade 메소드
     *
     * @param member 회원
     * @return 안읽은 알림 개수
     */
    public int countUnreadNotification(Member member) {
        return notificationService.countUnreadNotification(member);
    }

    /**
     * 알림 전체 목록 조회 Facade 메소드
     *
     * @param member  회원
     * @param pageIdx 페이지 번호
     * @return NotificationPageListResponse
     */
    public NotificationPageListResponse getNotificationPageList(Member member, Integer pageIdx) {
        Page<Notification> notificationPage = notificationService.getNotificationPage(member, pageIdx);

        return NotificationPageListResponse.of(notificationPage);
    }

    /**
     * 알림 팝업 목록 조회 Facade 메소드
     *
     * @param member   회원
     * @param cursorId 알림 id
     * @return NotificationCursorListResponse
     */
    public NotificationCursorListResponse getNotificationCursorList(Member member, Long cursorId) {
        Slice<Notification> notificationSlice = notificationService.getNotificationSlice(member, cursorId);

        return NotificationCursorListResponse.of(notificationSlice);
    }

    /**
     * 여러 알림 읽음 처리 Facade 메소드
     * @param member       회원
     * @param notificationIds 알림 id 리스트
     * @return 처리 결과 메시지
     */
    @Transactional
    public String readMultipleNotifications(Member member, List<Long> notificationIds) {
        notificationService.readMultipleNotifications(member, notificationIds);
        return "선택한 알림들이 읽음 처리되었습니다.";
    }

    /**
     * 여러 알림 삭제 처리 Facade 메소드
     * @param member      회원
     * @param notificationIds 알림 id 리스트
     * @return 처리 결과 메시지
     */
    @Transactional
    public String deleteMultipleNotifications(Member member, List<Long> notificationIds) {
        notificationService.deleteMultipleNotifications(member, notificationIds);
        return "선택한 알림들이 삭제되었습니다.";
    }

}
