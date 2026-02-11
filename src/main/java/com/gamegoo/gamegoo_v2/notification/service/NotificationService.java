package com.gamegoo.gamegoo_v2.notification.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.core.event.SocketNewNotificationEvent;
import com.gamegoo.gamegoo_v2.core.exception.NotificationException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.notification.domain.Notification;
import com.gamegoo.gamegoo_v2.notification.domain.NotificationType;
import com.gamegoo.gamegoo_v2.notification.domain.NotificationTypeTitle;
import com.gamegoo.gamegoo_v2.notification.repository.NotificationRepository;
import com.gamegoo.gamegoo_v2.notification.repository.NotificationTypeRepository;
import com.gamegoo.gamegoo_v2.social.manner.domain.MannerKeyword;
import com.gamegoo.gamegoo_v2.social.manner.repository.MannerKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationRepository notificationRepository;
    private final MannerKeywordRepository mannerKeywordRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final String PLACEHOLDER = "n";
    private final static int PAGE_SIZE = 10;

    /**
     * 친구 요청 전송됨 알림 생성 메소드
     *
     * @param member       알림 전송 대상 회원
     * @param sourceMember 친구 요청 전송 대상 회원
     * @return Notification
     */
    @Transactional
    public Notification createSendFriendRequestNotification(Member member, Member sourceMember) {
        validateMember(member);
        validateMember(sourceMember);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.FRIEND_REQUEST_SEND);
        Notification notification = saveNotification(notificationType, notificationType.getContent(), member,
                sourceMember);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 친구 요청 받음 알림 생성 메소드
     *
     * @param member       알림 전송 대상 회원
     * @param sourceMember 친구 요청 보낸 회원
     * @return Notification
     */
    @Transactional
    public Notification createReceivedFriendRequestNotification(Member member, Member sourceMember) {
        validateMember(member);
        validateMember(sourceMember);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.FRIEND_REQUEST_RECEIVED);
        Notification notification = saveNotification(notificationType, notificationType.getContent(), member,
                sourceMember);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 친구 요청 수락됨 알림 생성 메소드
     *
     * @param member       알림 전송 대상 회원
     * @param sourceMember 친구 요청 수락한 회원
     * @return Notification
     */
    @Transactional
    public Notification createAcceptFriendRequestNotification(Member member, Member sourceMember) {
        validateMember(member);
        validateMember(sourceMember);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.FRIEND_REQUEST_ACCEPTED);
        Notification notification = saveNotification(notificationType, notificationType.getContent(), member,
                sourceMember);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 친구 요청 거절됨 알림 생성 메소드
     *
     * @param member       알림 전송 대상 회원
     * @param sourceMember 친구 요청 거절한 회원
     * @return Notification
     */
    @Transactional
    public Notification createRejectFriendRequestNotification(Member member, Member sourceMember) {
        validateMember(member);
        validateMember(sourceMember);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.FRIEND_REQUEST_REJECTED);
        Notification notification = saveNotification(notificationType, notificationType.getContent(), member,
                sourceMember);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 매너레벨 상승/하락 알림 생성 메소드
     *
     * @param notificationTypeTitle 알림 유형
     * @param member                알림 전송 대상 회원
     * @param mannerLevel           매너레벨
     * @return Notification
     */
    @Transactional
    public Notification createMannerLevelNotification(NotificationTypeTitle notificationTypeTitle, Member member,
                                                      Integer mannerLevel) {
        validateMember(member);

        NotificationType notificationType = findNotificationType(notificationTypeTitle);
        String notificationContent = notificationType.getContent().replace(PLACEHOLDER, Integer.toString(mannerLevel));
        Notification notification = saveNotification(notificationType, notificationContent, member, null);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 매너 평가 등록됨 알림 생성 메소드
     *
     * @param mannerKeywordIdList 매너 키워드 id list
     * @param member              알림 전송 대상 회원
     * @return Notification
     */
    @Transactional
    public Notification createMannerRatingNotification(List<Long> mannerKeywordIdList, Member member) {
        validateMember(member);
        validateMannerKeywordList(mannerKeywordIdList);

        // 매너 키워드 엔티티 조회
        List<MannerKeyword> mannerKeywordList = mannerKeywordRepository.findAllById(mannerKeywordIdList);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.MANNER_KEYWORD_RATED);

        String mannerKeywordString = mannerKeywordList.get(0).getContents();
        if (mannerKeywordList.size() > 1) {
            mannerKeywordString += " 외 " + (mannerKeywordList.size() - 1) + "개의";
        }

        String notificationContent = notificationType.getContent().replace(PLACEHOLDER, mannerKeywordString);

        Notification notification = saveNotification(notificationType, notificationContent, member, null);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(member.getId()));

        return notification;
    }

    /**
     * 신고 처리 결과 알림 생성 메소드 (신고자)
     *
     * @param reporter       신고자
     * @param reportReason   신고 사유
     * @param banDescription 제재 설명
     * @return Notification
     */
    @Transactional
    public Notification createReportProcessedNotificationForReporter(Member reporter, String reportReason,
                                                                     String banDescription) {
        validateMember(reporter);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.REPORT_PROCESSED_REPORTER);

        String notificationContent = String.format(
                "신고 사유: %s\n처리 결과: %s",
                reportReason,
                banDescription
        );

        Notification notification = saveNotification(notificationType, notificationContent, reporter, null);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(reporter.getId()));

        return notification;
    }

    /**
     * 신고 처리 결과 알림 생성 메소드 (신고 당한 자)
     *
     * @param reported       신고 당한 회원
     * @param reportReason   신고 사유
     * @param banDescription 제재 설명
     * @return Notification
     */
    @Transactional
    public Notification createReportProcessedNotificationForReported(Member reported, String reportReason,
                                                                     String banDescription) {
        validateMember(reported);

        NotificationType notificationType = findNotificationType(NotificationTypeTitle.REPORT_PROCESSED_REPORTED);

        String notificationContent = String.format(
                "제한 사유: %s\n제한 기간: %s",
                reportReason,
                banDescription
        );

        Notification notification = saveNotification(notificationType, notificationContent, reported, null);

        // 새 알림 소켓 event 생성
        eventPublisher.publishEvent(new SocketNewNotificationEvent(reported.getId()));

        return notification;
    }

    /**
     * 알림 생성 및 저장 메소드
     *
     * @param type         알림 type
     * @param content      알림 텍스트
     * @param member       알림 전송 대상 회원
     * @param sourceMember 알림 연관 회원
     * @return Notification
     */
    @Transactional
    protected Notification saveNotification(NotificationType type, String content, Member member, Member sourceMember) {
        return notificationRepository.save(Notification.create(member, sourceMember, type, content));
    }

    /**
     * 알림 읽음 처리 메소드
     *
     * @param member         회원
     * @param notificationId 알림 id
     * @return Notification
     */
    @Transactional
    public Notification readNotification(Member member, Long notificationId) {
        validateNotificationExists(member, notificationId);

        Notification notification = notificationRepository.findById(notificationId).get();
        notification.updateIsRead(true);

        return notification;
    }

    /**
     * 안읽은 알림 개수 계산 메소드
     *
     * @param member 회원
     * @return 안읽은 알림 개수
     */
    public int countUnreadNotification(Member member) {
        return notificationRepository.countByMemberIdAndIsReadFalse(member.getId());
    }

    /**
     * 해당 회원의 알림 목록 Page 객체 반환하는 메소드
     *
     * @param member  회원
     * @param pageIdx 페이지 번호
     * @return 알림 Page 객체
     */
    public Page<Notification> getNotificationPage(Member member, Integer pageIdx) {
        PageRequest pageRequest = PageRequest.of(pageIdx - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));

        return notificationRepository.findNotificationsByMember(member, pageRequest);
    }

    /**
     * 해당 회원의 알림 목록 Slice 객체 반환하는 메소드
     *
     * @param member 회원
     * @param cursor 알림 id
     * @return 알림 Slice 객체
     */
    public Slice<Notification> getNotificationSlice(Member member, Long cursor) {
        return notificationRepository.findNotificationsByCursor(member.getId(), cursor, PAGE_SIZE);
    }

    /**
     * 여러 알림 읽음 처리 메소드
     * @param member       회원
     * @param notificationIds 읽음 처리할 알림 id 리스트
     */
    @Transactional
    public void readMultipleNotifications(Member member, List<Long> notificationIds) {
        // 각 notificationId에 해당하는 알림 존재 여부 검증
        for (Long notificationId : notificationIds) {
            validateNotificationExists(member, notificationId);
        }

        // 알림 일괄 읽음 처리
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        for (Notification notification : notifications) {
            notification.updateIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * title로 NotificationType을 찾는 메소드
     *
     * @param title NotificationTypeTitle enum
     * @return NotificationType
     */
    private NotificationType findNotificationType(NotificationTypeTitle title) {
        return notificationTypeRepository.findNotificationTypeByTitle(title)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));
    }

    /**
     * member가 null이 아닌지 검증하는 메소드
     *
     * @param member 회원
     */
    private void validateMember(Member member) {
        if (member == null) {
            throw new NotificationException(ErrorCode.NOTIFICATION_METHOD_BAD_REQUEST);
        }
    }

    /**
     * mannerKeywordList에 키워드가 1개 이상 있는지 검증하는 메소드
     *
     * @param mannerKeywordIdList 매너 레벨 키워드 id list
     */
    private void validateMannerKeywordList(List<Long> mannerKeywordIdList) {
        if (mannerKeywordIdList.isEmpty()) {
            throw new NotificationException(ErrorCode.NOTIFICATION_METHOD_BAD_REQUEST);
        }
    }

    /**
     * notificationId에 해당하는 알림 내역 존재 여부 검증하는 메소드
     *
     * @param member         회원
     * @param notificationId 알림 id
     */
    private void validateNotificationExists(Member member, Long notificationId) {
        boolean exists = notificationRepository.existsByMemberAndId(member, notificationId);
        if (!exists) {
            throw new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    /**
     * 여러 알림 삭제 처리 메소드
     * @param member       회원
     * @param notificationIds 삭제할 알림 id 리스트
     */
    @Transactional
    public void deleteMultipleNotifications(Member member, List<Long> notificationIds) {
        // 각 notificationId에 해당하는 알림 존재 여부 검증
        for (Long notificationId : notificationIds) {
            validateNotificationExists(member, notificationId);
        }

        // 알림 일괄 삭제 처리
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notificationRepository.deleteAll(notifications);
    }

}
