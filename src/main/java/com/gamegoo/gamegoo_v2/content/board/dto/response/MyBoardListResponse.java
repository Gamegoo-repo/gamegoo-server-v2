package com.gamegoo.gamegoo_v2.content.board.dto.response;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyBoardListResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    long boardId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    long memberId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Integer profileImage;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String gameName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String tag;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    Tier tier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    int rank;
    String contents;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
    LocalDateTime bumpTime;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Integer mannerLevel;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean canRefresh;

    public static MyBoardListResponse of(Board board) {
        Member member = board.getMember();
        Tier tier;
        int rank;

        if (board.getGameMode() == GameMode.FREE) {
            tier = member.getFreeTier();
            rank = member.getFreeRank();
        } else {
            tier = member.getSoloTier();
            rank = member.getSoloRank();
        }

        return MyBoardListResponse.builder()
                .boardId(board.getId())
                .memberId(member.getId())
                .profileImage(member.getProfileImage())
                .gameName(member.getGameName())
                .tag(member.getTag())
                .tier(tier)
                .rank(rank)
                .contents(board.getContent())
                .createdAt(board.getCreatedAt())
                .bumpTime(board.getBumpTime())
                .mannerLevel(member.getMannerLevel())
                .canRefresh(member.canRefreshChampionStats())
                .build();
    }

}
