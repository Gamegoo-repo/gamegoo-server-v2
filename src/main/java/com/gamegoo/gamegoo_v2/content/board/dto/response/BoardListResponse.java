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
public class BoardListResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long boardId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String gameName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String tag;
    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    private Position mainP;
    @Schema(ref = "#/components/schemas/Position", requiredMode = Schema.RequiredMode.REQUIRED)
    private Position subP;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/Position"))
    private List<Position> wantP;
    @Schema(ref = "#/components/schemas/Mike", requiredMode = Schema.RequiredMode.REQUIRED)
    private Mike mike;
    private String contents;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer profileImage;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer mannerLevel;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    private Tier tier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int rank;
    @Schema(ref = "#/components/schemas/GameMode", requiredMode = Schema.RequiredMode.REQUIRED)
    private GameMode gameMode;
    private Double winRate;
    private LocalDateTime bumpTime;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ChampionStatsResponse> championStatsResponseList;
    private MemberRecentStatsResponse memberRecentStats;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    private Tier freeTier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int freeRank;
    @Schema(ref = "#/components/schemas/Tier", requiredMode = Schema.RequiredMode.REQUIRED)
    private Tier soloTier;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int soloRank;
    private Boolean isBlocked;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean canRefresh;

    public static BoardListResponse of(Board board, Boolean isBlocked) {
        Member member = board.getMember();

        // 모든 게시글은 임시 멤버든 정식 멤버든 항상 Member를 가짐
        Tier tier;
        int rank;
        Double winRate;
        switch (board.getGameMode()) {
            case FREE:
                tier = member.getFreeTier();
                rank = member.getFreeRank();
                winRate = member.getFreeWinRate();
                break;
            case ARAM:
                tier = member.getSoloTier(); // 칼바람은 티어가 없으므로 솔로 티어 사용
                rank = member.getSoloRank();
                winRate = member.getAramWinRate();
                break;
            default: // SOLO, FAST
                tier = member.getSoloTier();
                rank = member.getSoloRank();
                winRate = member.getSoloWinRate();
                break;
        }

        List<ChampionStatsResponse> championStatsResponseList = getChampionStatsByGameMode(member, board.getGameMode());

        return BoardListResponse.builder()
                .boardId(board.getId())
                .memberId(member.getId())
                .gameName(member.getGameName())
                .tag(member.getTag())
                .mainP(board.getMainP())
                .subP(board.getSubP())
                .wantP(board.getWantP())
                .mike(board.getMike())
                .contents(board.getContent())
                .createdAt(board.getCreatedAt())
                .profileImage(member.getProfileImage())
                .mannerLevel(member.getMannerLevel())
                .tier(tier)
                .rank(rank)
                .gameMode(board.getGameMode())
                .winRate(winRate)
                .bumpTime(board.getBumpTime())
                .championStatsResponseList(championStatsResponseList)
                .memberRecentStats(
                        MemberRecentStatsResponse.fromGameMode(member.getMemberRecentStats(), board.getGameMode()))
                .freeTier(member.getFreeTier())
                .freeRank(member.getFreeRank())
                .soloTier(member.getSoloTier())
                .soloRank(member.getSoloRank())
                .isBlocked(isBlocked)
                .canRefresh(member.canRefreshChampionStats())
                .build();
    }

    private static List<ChampionStatsResponse> getChampionStatsByGameMode(Member member, GameMode gameMode) {
        if (member.getMemberChampionList() == null) {
            return List.of();
        }

        return member.getMemberChampionList().stream()
                .map(mc -> {
                    return switch (gameMode) {
                        case SOLO, FAST -> ChampionStatsResponse.builder()
                                .championId(mc.getChampion().getId())
                                .championName(mc.getChampion().getName())
                                .wins(mc.getSoloWins())
                                .games(mc.getSoloGames())
                                .winRate(
                                        mc.getSoloGames() > 0 ?
                                                ((double) mc.getSoloWins() / mc.getSoloGames()) * 100 : 0)
                                .csPerMinute(mc.getSoloCsPerMinute())
                                .averageCs(mc.getSoloGames() > 0 ? (double) mc.getSoloTotalCs() / mc.getSoloGames() : 0)
                                .kda(mc.getSoloKDA())
                                .kills(mc.getSoloGames() > 0 ? (double) mc.getSoloKills() / mc.getSoloGames() : 0)
                                .deaths(mc.getSoloGames() > 0 ? (double) mc.getSoloDeaths() / mc.getSoloGames() : 0)
                                .assists(mc.getSoloGames() > 0 ? (double) mc.getSoloAssists() / mc.getSoloGames() : 0)
                                .build();
                        case FREE -> ChampionStatsResponse.builder()
                                .championId(mc.getChampion().getId())
                                .championName(mc.getChampion().getName())
                                .wins(mc.getFreeWins())
                                .games(mc.getFreeGames())
                                .winRate(
                                        mc.getFreeGames() > 0 ?
                                                ((double) mc.getFreeWins() / mc.getFreeGames()) * 100 : 0)
                                .csPerMinute(mc.getFreeCsPerMinute())
                                .averageCs(mc.getFreeGames() > 0 ? (double) mc.getFreeTotalCs() / mc.getFreeGames() : 0)
                                .kda(mc.getFreeKDA())
                                .kills(mc.getFreeGames() > 0 ? (double) mc.getFreeKills() / mc.getFreeGames() : 0)
                                .deaths(mc.getFreeGames() > 0 ? (double) mc.getFreeDeaths() / mc.getFreeGames() : 0)
                                .assists(mc.getFreeGames() > 0 ? (double) mc.getFreeAssists() / mc.getFreeGames() : 0)
                                .build();
                        case ARAM -> ChampionStatsResponse.builder()
                                .championId(mc.getChampion().getId())
                                .championName(mc.getChampion().getName())
                                .wins(mc.getAramWins())
                                .games(mc.getAramGames())
                                .winRate(
                                        mc.getAramGames() > 0 ?
                                                ((double) mc.getAramWins() / mc.getAramGames()) * 100 : 0)
                                .csPerMinute(mc.getAramCsPerMinute())
                                .averageCs(mc.getAramGames() > 0 ? (double) mc.getAramTotalCs() / mc.getAramGames() : 0)
                                .kda(mc.getAramKDA())
                                .kills(mc.getAramGames() > 0 ? (double) mc.getAramKills() / mc.getAramGames() : 0)
                                .deaths(mc.getAramGames() > 0 ? (double) mc.getAramDeaths() / mc.getAramGames() : 0)
                                .assists(mc.getAramGames() > 0 ? (double) mc.getAramAssists() / mc.getAramGames() : 0)
                                .build();
                    };
                })
                .filter(response -> response.getGames() > 0) // 게임이 있는 챔피언만 포함
                .collect(Collectors.toList());
    }

}
