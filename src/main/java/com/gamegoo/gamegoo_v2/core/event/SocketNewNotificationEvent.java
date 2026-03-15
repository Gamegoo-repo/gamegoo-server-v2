package com.gamegoo.gamegoo_v2.core.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocketNewNotificationEvent {

    private final Long memberId;
    private final Long notificationId;
    private final int notificationType;
    private final String content;
    private final String pageUrl;
    private final boolean read;

}
