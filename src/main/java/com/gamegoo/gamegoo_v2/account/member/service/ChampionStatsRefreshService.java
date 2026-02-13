package com.gamegoo.gamegoo_v2.account.member.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.MemberRecentStats;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberChampionRepository;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRecentStatsRepository;
import com.gamegoo.gamegoo_v2.external.riot.domain.ChampionStats;
import com.gamegoo.gamegoo_v2.external.riot.dto.TierDetails;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotAuthService;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotInfoService;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChampionStatsRefreshService {

    private final RiotAuthService riotAuthService;
    private final MemberChampionService memberChampionService;
    private final MemberChampionRepository memberChampionRepository;
    private final RiotRecordService riotRecordService;
    private final RiotInfoService riotInfoService;
    private final MemberService memberService;
    private final MemberRecentStatsRepository memberRecentStatsRepository;

    @Transactional
    public void refreshChampionStats(Member member) {
        Long memberId = member.getId();
        Member freshMember = memberService.findMemberById(memberId);

        String gameName = freshMember.getGameName();
        String tag = freshMember.getTag();
        String puuid = freshMember.getPuuid() != null ? freshMember.getPuuid() : riotAuthService.getPuuid(gameName, tag);

        // 먼저 새로운 데이터 조회
        var accountInfo = riotAuthService.getAccountByPuuid(puuid);

        // 최적화된 한 번의 API 호출로 모든 모드 통계 조회
        var allModeStats = riotRecordService.getAllModeStatsOptimized(gameName, puuid);

        // 프로필용 통합 데이터 (솔로+자유만)
        var recStats = allModeStats.getCombinedStats();
        List<ChampionStats> preferChampionStats = allModeStats.getCombinedChampionStats().values().stream()
                .filter(stats -> stats.getGames() > 0)
                .sorted(Comparator.comparingInt(ChampionStats::getGames).reversed())
                .limit(4)
                .collect(Collectors.toList());

        // 모드별 분리 데이터 (게시판용)
        var soloRecStats = allModeStats.getSoloStats();
        var freeRecStats = allModeStats.getFreeStats();
        var aramRecStats = allModeStats.getAramStats();

        List<ChampionStats> soloChampionStats = allModeStats.getSoloChampionStats().values().stream()
                .filter(stats -> stats.getGames() > 0)
                .sorted(Comparator.comparingInt(ChampionStats::getGames).reversed())
                .limit(4)
                .collect(Collectors.toList());

        List<ChampionStats> freeChampionStats = allModeStats.getFreeChampionStats().values().stream()
                .filter(stats -> stats.getGames() > 0)
                .sorted(Comparator.comparingInt(ChampionStats::getGames).reversed())
                .limit(4)
                .collect(Collectors.toList());

        List<ChampionStats> aramChampionStats = allModeStats.getAramChampionStats().values().stream()
                .filter(stats -> stats.getGames() > 0)
                .sorted(Comparator.comparingInt(ChampionStats::getGames).reversed())
                .limit(4)
                .collect(Collectors.toList());

        List<TierDetails> tierWinrateRank = riotInfoService.getTierWinrateRank(puuid);

        // API 호출이 성공한 경우에만 기존 데이터 삭제 후 새로 저장
        memberChampionRepository.deleteByMember(freshMember);

        // 1. 먼저 프로필용 통합 챔피언 통계 저장 (솔랭+자유 플레이한 챔피언만)
        memberChampionService.saveMemberChampions(freshMember, preferChampionStats);

        // 2. 그 다음 모든 모드별 챔피언들을 저장하고 모드별 통계 추가
        memberChampionService.saveMemberChampionsByMode(freshMember, soloChampionStats, freeChampionStats, aramChampionStats);

        freshMember.updateRiotBasicInfo(accountInfo.getGameName(), accountInfo.getTagLine());
        freshMember.updateRiotStats(tierWinrateRank);

        // 칼바람 승률 업데이트
        freshMember.updateAramWinRate(aramRecStats.getRecWinRate());

        // 갱신 시간도 함께 업데이트
        freshMember.updateChampionStatsRefreshedAt();

        // 최근 30게임 통계 계산 및 저장
        MemberRecentStats memberRecentStats = memberRecentStatsRepository.findById(memberId)
                .orElseGet(() -> {
                    MemberRecentStats stats = MemberRecentStats.builder().build();
                    freshMember.setMemberRecentStats(stats);
                    return stats;
                });

        // 기존 통합 통계 업데이트 (프로필용)
        memberRecentStats.update(
                recStats.getRecTotalWins(),
                recStats.getRecTotalLosses(),
                recStats.getRecWinRate(),
                recStats.getRecAvgKDA(),
                recStats.getRecAvgKills(),
                recStats.getRecAvgDeaths(),
                recStats.getRecAvgAssists(),
                recStats.getRecAvgCsPerMinute(),
                recStats.getRecTotalCs()
        );

        // 모드별 통계 업데이트 (게시판용)
        memberRecentStats.updateSoloStats(
                soloRecStats.getRecTotalWins(),
                soloRecStats.getRecTotalLosses(),
                soloRecStats.getRecWinRate(),
                soloRecStats.getRecAvgKDA(),
                soloRecStats.getRecAvgKills(),
                soloRecStats.getRecAvgDeaths(),
                soloRecStats.getRecAvgAssists(),
                soloRecStats.getRecAvgCsPerMinute(),
                soloRecStats.getRecTotalCs()
        );

        memberRecentStats.updateFreeStats(
                freeRecStats.getRecTotalWins(),
                freeRecStats.getRecTotalLosses(),
                freeRecStats.getRecWinRate(),
                freeRecStats.getRecAvgKDA(),
                freeRecStats.getRecAvgKills(),
                freeRecStats.getRecAvgDeaths(),
                freeRecStats.getRecAvgAssists(),
                freeRecStats.getRecAvgCsPerMinute(),
                freeRecStats.getRecTotalCs()
        );

        memberRecentStats.updateAramStats(
                aramRecStats.getRecTotalWins(),
                aramRecStats.getRecTotalLosses(),
                aramRecStats.getRecWinRate(),
                aramRecStats.getRecAvgKDA(),
                aramRecStats.getRecAvgKills(),
                aramRecStats.getRecAvgDeaths(),
                aramRecStats.getRecAvgAssists(),
                aramRecStats.getRecAvgCsPerMinute(),
                aramRecStats.getRecTotalCs()
        );

        memberRecentStatsRepository.save(memberRecentStats);
    }
}
