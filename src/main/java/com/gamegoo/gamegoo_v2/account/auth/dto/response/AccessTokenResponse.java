package com.gamegoo.gamegoo_v2.account.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccessTokenResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String accessToken;

    public static AccessTokenResponse of(Long id, String accessToken) {
        return AccessTokenResponse.builder()
                .id(id)
                .accessToken(accessToken)
                .build();
    }
}
