package com.gamegoo.gamegoo_v2.external.riot.service;

import com.gamegoo.gamegoo_v2.account.auth.jwt.JwtProvider;
import com.gamegoo.gamegoo_v2.account.auth.service.AuthService;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.service.BanService;
import com.gamegoo.gamegoo_v2.account.member.service.MemberService;
import com.gamegoo.gamegoo_v2.account.member.service.AsyncChampionStatsService;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RSOLoginResponse;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotJoinResponse;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.gamegoo.gamegoo_v2.external.riot.domain.RSOState;
import com.gamegoo.gamegoo_v2.external.riot.dto.TierDetails;
import com.gamegoo.gamegoo_v2.external.riot.dto.request.RiotJoinRequest;
import com.gamegoo.gamegoo_v2.external.riot.dto.request.RiotVerifyExistUserRequest;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotAccountIdResponse;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotAuthTokenResponse;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotPuuidGameNameResponse;
import com.gamegoo.gamegoo_v2.utils.StateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiotFacadeService {

    private final RiotAuthService riotAccountService;
    private final RiotOAuthService riotOAuthService;
    private final RiotInfoService riotInfoService;
    private final MemberService memberService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final BanService banService;
    private final AsyncChampionStatsService asyncChampionStatsService;

    /**
     * 사용가능한 riot 계정인지 검증
     *
     * @param request 소환사명, 태그
     */
    public String verifyRiotAccount(RiotVerifyExistUserRequest request) {
        // puuid 발급 가능한지 검증
        riotAccountService.getPuuid(request.getGameName(), request.getTag());

        return "해당 Riot 계정은 존재합니다";
    }

    @Transactional
    public RiotJoinResponse join(RiotJoinRequest request) {
        // [Member] puuid 중복 확인
        memberService.checkDuplicateMemberByPuuid(request.getPuuid());

        // [Riot] gameName, Tag 얻기
        RiotPuuidGameNameResponse response = riotAccountService.getAccountByPuuid(request.getPuuid());

        // [Riot] tier, rank, winrate 얻기
        List<TierDetails> tierWinrateRank = riotInfoService.getTierWinrateRank(request.getPuuid());

        // [Member] member DB에 저장
        Member member = memberService.createMemberRiot(request, response.getGameName(), response.getTagLine(),
                tierWinrateRank);

        // [Async] 트랜잭션 커밋 후 비동기로 champion stats refresh 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    asyncChampionStatsService.refreshChampionStatsAsync(member.getId());
                } catch (Exception e) {
                    // 비동기 작업 실패가 메인 플로우에 영향을 주지 않도록 로그만 기록
                    // 로그 출력 생략 (주요 플로우 방해 안 하기 위해)
                }
            }
        });

        // 해당 사용자의 정보를 가진 jwt 토큰 발급
        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole());

        // DB에 저장
        authService.updateRefreshToken(member, refreshToken);

        return RiotJoinResponse.of(member, accessToken, refreshToken);
    }

    /**
     * RSO 콜백 처리 로직
     *
     * @param code  토큰 발급용 코드
     * @param state 프론트 리다이렉트용 state
     * @return RSOLoginResponse (redirectUrl + refreshToken)
     */
    @Transactional
    public RSOLoginResponse processOAuthCallback(String code, String state) {
        RSOState decodedRSOState = StateUtil.decodeRSOState(state);
        String targetUrl = decodedRSOState.getRedirect();

        RiotAuthTokenResponse riotAuthTokenResponse;
        RiotAccountIdResponse summonerInfo;

        try {
            // 토큰 교환
            riotAuthTokenResponse = riotOAuthService.exchangeCodeForTokens(code);

            // id_token 파싱 → Riot 사용자 정보 추출
            summonerInfo = riotOAuthService.getSummonerInfo(riotAuthTokenResponse.getAccessToken());
        } catch (Exception e) {
            return RSOLoginResponse.builder()
                    .redirectUrl(String.format("%s?error=riot_api_error", targetUrl))
                    .build();
        }

        // Riot 계정 정보 없음
        if (summonerInfo == null) {
            return RSOLoginResponse.builder()
                    .redirectUrl(String.format("%s?error=signup_disabled", targetUrl))
                    .build();
        }

        // DB에서 사용자 존재 여부 확인
        String puuid = summonerInfo.getPuuid();
        List<Member> memberList = memberService.findMemberByPuuid(puuid);

        // 회원가입 필요
        if (memberList.isEmpty()) {
            return RSOLoginResponse.join(targetUrl, state, puuid);
        }

        Member member = memberList.get(0);

        // 탈퇴 사용자
        if (member.getBlind()) {
            return RSOLoginResponse.builder()
                    .redirectUrl(String.format("%s?error=member_isBlind&puuid=%s", targetUrl, puuid))
                    .build();
        }

        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole());

        // refresh token 저장
        authService.updateRefreshToken(member, refreshToken);

        // 만료된 제재 자동 해제
        banService.checkBanExpiry(member);

        // 제재 메시지 생성
        String banMessage = null;
        if (member.isBanned()) {
            banMessage = banService.getBanReasonMessage(member.getBanType());
        }

        return RSOLoginResponse.of(member, state, targetUrl, accessToken, refreshToken, banMessage);
    }

}
