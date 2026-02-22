package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RollBtiRecommendedMemberResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long boardId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String tag;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer profileImage;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer mannerLevel;

    @Schema(ref = "#/components/schemas/GameMode", requiredMode = Schema.RequiredMode.REQUIRED)
    private GameMode gameMode;

    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    private Position mainP;

    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    private Position subP;

    @Schema(ref = "#/components/schemas/Mike", requiredMode = Schema.RequiredMode.REQUIRED)
    private Mike mike;

    private String content;

    @Schema(ref = "#/components/schemas/RollBtiType")
    private RollBtiType memberType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer compatibilityScore;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime activityTime;

    public static RollBtiRecommendedMemberResponse of(
            Long boardId,
            Long memberId,
            String gameName,
            String tag,
            Integer profileImage,
            Integer mannerLevel,
            GameMode gameMode,
            Position mainP,
            Position subP,
            Mike mike,
            String content,
            RollBtiType memberType,
            Integer compatibilityScore,
            LocalDateTime activityTime) {
        return RollBtiRecommendedMemberResponse.builder()
                .boardId(boardId)
                .memberId(memberId)
                .gameName(gameName)
                .tag(tag)
                .profileImage(profileImage)
                .mannerLevel(mannerLevel)
                .gameMode(gameMode)
                .mainP(mainP)
                .subP(subP)
                .mike(mike)
                .content(content)
                .memberType(memberType)
                .compatibilityScore(compatibilityScore)
                .activityTime(activityTime)
                .build();
    }
}

