package com.gamegoo.gamegoo_v2.external.riot.dto.response;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Builder
public class RSOLoginResponse {

    public String redirectUrl;
    public String refreshToken;

    // 로그인 성공 Redirect URL 생성
    public static RSOLoginResponse of(Member member, String state, String frontUrl, String accessToken, String refreshToken, String banMessage) {
        String redirectUrl = buildLoginRedirectUrl(member, state, frontUrl, accessToken, banMessage);

        return RSOLoginResponse.builder()
                .redirectUrl(redirectUrl)
                .refreshToken(refreshToken)
                .build();
    }

    // 회원가입 필요 Redirect URL 생성
    public static RSOLoginResponse join(String frontUrl, String state, String puuid) {
        return RSOLoginResponse.builder()
                .redirectUrl(buildJoinRedirectUrl(frontUrl, state, puuid))
                .build();
    }

    // 로그인 성공 Redirect URL 생성
    private static String buildLoginRedirectUrl(
            Member member,
            String state,
            String frontUrl,
            String accessToken,
            String banMessage
    ) {
        String encodedName = URLEncoder.encode(member.getGameName(), StandardCharsets.UTF_8);
        String encodedTag = URLEncoder.encode(member.getTag(), StandardCharsets.UTF_8);
        String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedBanType = URLEncoder.encode(String.valueOf(member.getBanType()), StandardCharsets.UTF_8);
        String encodedIsBanned =
                URLEncoder.encode(String.valueOf(member.isBanned()), StandardCharsets.UTF_8);

        String encodedBanExpireAt = member.getBanExpireAt() == null
                ? ""
                : URLEncoder.encode(String.valueOf(member.getBanExpireAt()), StandardCharsets.UTF_8);

        String encodedBanMessage = banMessage == null
                ? ""
                : URLEncoder.encode(banMessage, StandardCharsets.UTF_8);

        return String.format(
                "%s?status=LOGIN_SUCCESS&accessToken=%s&name=%s&tag=%s&profileImage=%s&id=%s" +
                        "&state=%s&banType=%s&banExpireAt=%s&banMessage=%s&isBanned=%s",
                frontUrl,
                encodedAccessToken,
                encodedName,
                encodedTag,
                member.getProfileImage(),
                member.getId(),
                state,
                encodedBanType,
                encodedBanExpireAt,
                encodedBanMessage,
                encodedIsBanned
        );
    }

    // 회원가입 필요 Redirect URL 생성
    private static String buildJoinRedirectUrl(String frontUrl, String state, String puuid) {
        String encodedPuuid = URLEncoder.encode(puuid, StandardCharsets.UTF_8);
        return String.format(
                "%s?status=NEED_SIGNUP&puuid=%s&state=%s",
                frontUrl,
                encodedPuuid,
                state
        );
    }
}


