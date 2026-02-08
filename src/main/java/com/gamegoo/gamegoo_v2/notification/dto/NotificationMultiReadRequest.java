package com.gamegoo.gamegoo_v2.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationMultiReadRequest {

    @Min(value = 1)
    @NotNull(message = "mike 는 비워둘 수 없습니다.")
    List<Long> notificationIds;
}
