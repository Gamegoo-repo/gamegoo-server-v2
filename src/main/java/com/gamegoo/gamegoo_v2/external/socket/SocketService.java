package com.gamegoo.gamegoo_v2.external.socket;

import com.gamegoo.gamegoo_v2.core.exception.SocketException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocketService {

    private final RestTemplate restTemplate;

    @Value("${socket.server.url}")
    private String SOCKET_SERVER_URL;

    private static final String JOIN_CHATROOM_URL = "/socket/room/join";
    private static final String SYS_MESSAGE_URL = "/socket/sysmessage";
    private static final String FRIEND_ONLINE_URL = "/socket/friend/online/";
    private static final String NEW_NOTIFICATION_URL = "/socket/newnotification/";

    /**
     * SOCKET 서버로 해당 회원의 socket을 room에 join 요청하는 API 전송
     *
     * @param memberId 회원 id
     * @param uuid     채팅방 uuid
     */
    public void joinSocketToChatroom(Long memberId, String uuid) {
        String url = SOCKET_SERVER_URL + JOIN_CHATROOM_URL;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("memberId", memberId);
        requestBody.put("chatroomUuid", uuid);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            log.info("response of joinSocketToChatroom: {}", response.getStatusCode());
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error("joinSocketToChatroom API call FAIL: {}", response.getBody());
                throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
            } else {
                log.info("joinSocketToChatroom API call SUCCESS: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error occurred while joinSocketToChatroom method", e);
            throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
        }
    }

    /**
     * SOCKET 서버로 해당 회원의 socket에 시스템 메시지 전송을 요청하는 API 전송
     *
     * @param memberId     회원 id
     * @param chatroomUuid 채팅방 uuid
     * @param content      메시지 내용
     * @param timestamp    메시지 전송 시각
     */
    public void sendSystemMessage(Long memberId, String chatroomUuid, String content, Long timestamp) {
        String url = SOCKET_SERVER_URL + SYS_MESSAGE_URL;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("memberId", memberId);
        requestBody.put("chatroomUuid", chatroomUuid);
        requestBody.put("content", content);
        requestBody.put("timestamp", timestamp);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            log.info("response of joinSocketToChatroom: {}", response.getStatusCode());
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error("joinSocketToChatroom API call FAIL: {}", response.getBody());
                throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
            } else {
                log.info("joinSocketToChatroom API call SUCCESS: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error occurred while sendSystemMessage method", e);
            throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
        }
    }

    /**
     * SOCKET 서버로 member와 targetMember의 socket에 friend-online event emit을 요청하는 API 전송
     *
     * @param memberId       회원 id
     * @param targetMemberId 상대 회원 id
     */
    public void emitFriendOnlineEvent(Long memberId, Long targetMemberId) {
        String url = SOCKET_SERVER_URL + FRIEND_ONLINE_URL + memberId.toString();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("targetMemberId", targetMemberId);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            log.info("response of emitFriendOnlineEvent: {}", response.getStatusCode());
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error("emitFriendOnlineEvent API call FAIL: {}", response.getBody());
                throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
            } else {
                log.info("emitFriendOnlineEvent API call SUCCESS: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error occurred while emitFriendOnlineEvent method", e);
            throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
        }
    }

    /**
     * SOCKET 서버로 member의 socket에 new-notification event emit을 요청하는 API 전송
     *
     * @param memberId
     */
    public void emitNewNotification(Long memberId, Long notificationId, int notificationType, String content,
                                    String pageUrl, boolean read) {
        String url = SOCKET_SERVER_URL + NEW_NOTIFICATION_URL + memberId.toString();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("notificationId", notificationId);
        requestBody.put("notificationType", notificationType);
        requestBody.put("content", content);
        requestBody.put("pageUrl", pageUrl);
        requestBody.put("read", read);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);


            log.info("response of emitNewNotification: {}", response.getStatusCode());
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.error("emitNewNotification API call FAIL: {}", response.getBody());
                throw new SocketException(ErrorCode.SOCKET_API_RESPONSE_ERROR);
            } else {
                log.info("emitNewNotification API call SUCCESS: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("Error occurred while emitNewNotification method", e);
        }
    }

}
