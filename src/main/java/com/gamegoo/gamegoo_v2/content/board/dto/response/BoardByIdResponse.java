package com.gamegoo.gamegoo_v2.content.board.dto.response;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.dto.response.MemberRecentStatsResponse;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BoardByIdResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    long boardId;
    Long memberId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Integer profileImage;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String gameName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String tag;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Integer mannerLevel;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    Tier soloTier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    int soloRank;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    Tier freeTier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    int freeRank;
    @Schema(ref = "#/components/schemas/Mike", requiredMode = Schema.RequiredMode.REQUIRED)
    Mike mike;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<ChampionStatsResponse> championStatsResponseList;
    MemberRecentStatsResponse memberRecentStats;
    @Schema(ref = "#/components/schemas/GameMode", requiredMode = Schema.RequiredMode.REQUIRED)
    GameMode gameMode;
    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    Position mainP;
    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    Position subP;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/Position"))
    List<Position> wantP;
    Integer recentGameCount;
    Double winRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<Long> gameStyles;
    String contents;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean canRefresh;

    public static BoardByIdResponse of(Board board) {
        Member poster = board.getMember();
        List<Long> gameStyleIds = board.getBoardGameStyles().stream()
                .map(bgs -> bgs.getGameStyle().getId())
                .collect(Collectors.toList());
        List<ChampionStatsResponse> championStatsResponseList = poster.getMemberChampionList() == null
                ? List.of()
                : poster.getMemberChampionList().stream()
                .map(mc -> ChampionStatsResponse.builder()
                        .championId(mc.getChampion().getId())
                        .championName(mc.getChampion().getName())
                        .wins(mc.getWins())
                        .games(mc.getGames())
                        .winRate(mc.getGames() > 0 ? ((double) mc.getWins() / mc.getGames()) * 100 : 0)
                        .csPerMinute(mc.getCsPerMinute())
                        .averageCs(mc.getGames() > 0 ? (double) mc.getTotalCs() / mc.getGames() : 0)
                        .kda(mc.getKDA())
                        .kills(mc.getGames() > 0 ? (double) mc.getKills() / mc.getGames() : 0)
                        .deaths(mc.getGames() > 0 ? (double) mc.getDeaths() / mc.getGames() : 0)
                        .assists(mc.getGames() > 0 ? (double) mc.getAssists() / mc.getGames() : 0)
                        .build())
                .filter(response -> response.getGames() > 0)
                .sorted((c1, c2) -> Integer.compare(c2.getGames(), c1.getGames()))
                .limit(4)
                .collect(Collectors.toList());

        Integer recentGameCount;
        Double winRate;

        if (board.getGameMode() == GameMode.FREE) {
            recentGameCount = poster.getFreeGameCount();
            winRate = poster.getFreeWinRate();
        } else {
            recentGameCount = poster.getSoloGameCount();
            winRate = poster.getSoloWinRate();
        }

        return BoardByIdResponse.builder()
                .boardId(board.getId())
                .memberId(poster.getId())
                .createdAt(board.getCreatedAt())
                .profileImage(poster.getProfileImage())
                .gameName(poster.getGameName())
                .tag(poster.getTag())
                .mannerLevel(poster.getMannerLevel())
                .soloTier(poster.getSoloTier())
                .soloRank(poster.getSoloRank())
                .freeTier(poster.getFreeTier())
                .freeRank(poster.getFreeRank())
                .mike(board.getMike())
                .championStatsResponseList(championStatsResponseList)
                .memberRecentStats(MemberRecentStatsResponse.from(poster.getMemberRecentStats()))
                .gameMode(board.getGameMode())
                .mainP(board.getMainP())
                .subP(board.getSubP())
                .wantP(board.getWantP())
                .recentGameCount(recentGameCount)
                .winRate(winRate)
                .gameStyles(gameStyleIds)
                .contents(board.getContent())
                .canRefresh(poster.canRefreshChampionStats())
                .build();
    }

}
