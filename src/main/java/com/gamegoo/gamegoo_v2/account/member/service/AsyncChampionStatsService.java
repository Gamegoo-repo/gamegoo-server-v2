package com.gamegoo.gamegoo_v2.account.member.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncChampionStatsService {

    private final ChampionStatsRefreshService championStatsRefreshService;
    private final MemberService memberService;

    /**
     * 비동기로 회원의 챔피언 통계를 갱신합니다.
     * 회원가입 시 호출되어 백그라운드에서 실행됩니다.
     *
     * @param memberId 갱신할 회원 ID
     */
    @Async
    public void refreshChampionStatsAsync(Long memberId) {
        try {
            Member member = memberService.findMemberById(memberId);
            championStatsRefreshService.refreshChampionStats(member);
            log.info("챔피언 통계 갱신 성공 - memberId: {}", memberId);
        } catch (Exception e) {
            log.error("챔피언 통계 갱신 실패 - memberId: {}, error: {}",
                memberId, e.getMessage(), e);
        }
    }
}
