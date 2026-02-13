package com.gamegoo.gamegoo_v2.account.member.service;

import com.gamegoo.gamegoo_v2.account.auth.domain.Role;
import com.gamegoo.gamegoo_v2.account.member.domain.LoginType;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.MemberRecentStats;
import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRecentStatsRepository;
import com.gamegoo.gamegoo_v2.core.exception.MemberException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.external.riot.dto.TierDetails;
import com.gamegoo.gamegoo_v2.external.riot.dto.request.RiotJoinRequest;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberRecentStatsRepository memberRecentStatsRepository;

    @Transactional
    public Member createMemberRiot(RiotJoinRequest request, String gameName, String tag, List<TierDetails> tiers) {

        // 기본 값 설정
        Tier soloTier = Tier.UNRANKED;
        int soloRank = 0;
        double soloWinRate = 0.0;
        int soloGameCount = 0;

        Tier freeTier = Tier.UNRANKED;
        int freeRank = 0;
        double freeWinRate = 0.0;
        int freeGameCount = 0;

        // 티어 정보 설정
        for (TierDetails tierDetail : tiers) {
            if (tierDetail.getGameMode() == GameMode.SOLO) {
                soloTier = tierDetail.getTier();
                soloRank = tierDetail.getRank();
                soloWinRate = tierDetail.getWinrate();
                soloGameCount = tierDetail.getGameCount();
            } else if (tierDetail.getGameMode() == GameMode.FREE) {
                freeTier = tierDetail.getTier();
                freeRank = tierDetail.getRank();
                freeWinRate = tierDetail.getWinrate();
                freeGameCount = tierDetail.getGameCount();
            }
        }

        // Member 생성
        Member member = Member.createForRiot(
                request.getPuuid(), LoginType.RSO, gameName, tag,
                soloTier, soloRank, soloWinRate,
                freeTier, freeRank, freeWinRate,
                soloGameCount, freeGameCount, true
        );

        memberRepository.save(member);

        // MemberRecentStats 빈 껍데기 생성 (비동기 통계 갱신 전 EntityNotFoundException 방지)
        MemberRecentStats recentStats = MemberRecentStats.builder().build();
        member.setMemberRecentStats(recentStats);
        memberRecentStatsRepository.save(recentStats);

        return member;
    }

    /**
     * 회원 정보 조회
     *
     * @param memberId 사용자 ID
     * @return Member
     */
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * Email로 회원 정보 조회
     *
     * @param email 사용자 ID
     * @return Member
     */
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 소환사명, 태그로 회원 정보 조회
     *
     * @param gameName 소환사명
     * @param tag      태그
     * @return member
     */
    public Member findMemberByGameNameAndTag(String gameName, String tag) {
        return memberRepository.findByGameNameAndTag(gameName, tag).orElseThrow(
                () -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * Email 중복 확인하기
     *
     * @param email email
     */
    public void checkDuplicateMemberByEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }

    /**
     * Puuid 중복 확인하기
     *
     * @param puuid puuid
     */
    public void checkDuplicateMemberByPuuid(String puuid) {
        if (memberRepository.existsByPuuid(puuid)) {
            throw new MemberException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }


    /**
     * DB에 없는 사용자일 경우 예외 발생
     *
     * @param email email
     */
    public void checkExistMemberByEmail(String email) {
        if (!memberRepository.existsByEmail(email)) {
            throw new MemberException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    /**
     * 프로필 이미지 수정
     *
     * @param member       회원
     * @param profileImage 프로필이미지
     */
    @Transactional
    public void setProfileImage(Member member, int profileImage) {
        member.updateProfileImage(profileImage);
    }

    /**
     * 마이크 여부 수정
     *
     * @param member 회원
     * @param mike   마이크 상태
     */
    @Transactional
    public void setIsMike(Member member, Mike mike) {
        member.updateMike(mike);
    }

    /**
     * 포지션 수정
     *
     * @param member        회원
     * @param mainPosition  주 포지션
     * @param subPosition   부 포지션
     * @param wantPositions 원하는 포지션 리스트
     */
    @Transactional
    public void setPosition(Member member, Position mainPosition, Position subPosition, List<Position> wantPositions) {
        member.updatePosition(mainPosition, subPosition, wantPositions);
    }


    /**
     * 마이크, 포지션 수정
     *
     * @param member        회원
     * @param mike          마이크 유무
     * @param mainP         주 포지션
     * @param subP          부 포지션
     * @param wantPositions 원하는 포지션 리스트
     */
    @Transactional
    public void updateMikePosition(Member member, Mike mike, Position mainP, Position subP,
                                   List<Position> wantPositions) {
        member.updateMemberByMatchingRecord(mike, mainP, subP, wantPositions);
    }


    @Transactional
    public List<Member> findMemberByPuuid(String puuid) {
        return memberRepository.findByPuuid(puuid);
    }

    /**
     * Member 탈퇴 처리
     *
     * @param member 회원
     */
    @Transactional
    public void deactivateMember(Member member) {
        member.updateBlind(true);
        memberRepository.save(member);
    }

    /**
     * Member 재가입 처리
     *
     * @param member 회원
     */
    @Transactional
    public void activateMember(Member member) {
        member.updateBlind(false);
        memberRepository.save(member);
    }

    /**
     * 회원 역할(권한) 수정
     *
     * @param member 회원
     * @param role   변경할 역할
     */
    @Transactional
    public void updateMemberRole(Member member, Role role) {
        member.updateRole(role);
    }

    /**
     * 임시 멤버 생성 및 저장
     *
     * @param gameName 게임 이름
     * @param tag      태그
     * @param tiers    티어 정보 리스트
     * @return 생성된 임시 멤버
     */
    @Transactional
    public Member createTmpMember(String gameName, String tag, List<TierDetails> tiers) {
        // 기본 값 설정
        Tier soloTier = Tier.UNRANKED;
        int soloRank = 0;
        double soloWinRate = 0.0;
        int soloGameCount = 0;

        Tier freeTier = Tier.UNRANKED;
        int freeRank = 0;
        double freeWinRate = 0.0;
        int freeGameCount = 0;

        // 티어 정보 설정
        if (tiers != null) {
            for (TierDetails tierDetail : tiers) {
                if (tierDetail.getGameMode() == GameMode.SOLO) {
                    soloTier = tierDetail.getTier();
                    soloRank = tierDetail.getRank();
                    soloWinRate = tierDetail.getWinrate();
                    soloGameCount = tierDetail.getGameCount();
                } else if (tierDetail.getGameMode() == GameMode.FREE) {
                    freeTier = tierDetail.getTier();
                    freeRank = tierDetail.getRank();
                    freeWinRate = tierDetail.getWinrate();
                    freeGameCount = tierDetail.getGameCount();
                }
            }
        }

        // 임시 멤버 생성
        Member tmpMember = Member.createForTmp(
                gameName, tag,
                soloTier, soloRank, soloWinRate,
                freeTier, freeRank, freeWinRate,
                soloGameCount, freeGameCount
        );

        // Role을 TMP로 설정
        tmpMember.updateRole(Role.TMP);

        memberRepository.save(tmpMember);
        return tmpMember;
    }

    /**
     * 임시 멤버 조회 또는 생성
     *
     * @param gameName 게임 이름
     * @param tag      태그
     * @param tiers    티어 정보 리스트
     * @return 기존 또는 새로 생성된 임시 멤버
     */
    @Transactional
    public Member getOrCreateTmpMember(String gameName, String tag, List<TierDetails> tiers) {
        // 기존 멤버 확인
        return memberRepository.findByGameNameAndTag(gameName, tag)
                .map(existingMember -> {
                    // 기존 회원이 있으면 TMP 역할인지 확인
                    if (existingMember.getRole() == Role.TMP) {
                        return existingMember; // 재사용
                    } else {
                        // 정식 회원이면 예외 발생
                        throw new MemberException(ErrorCode.MEMBER_ALREADY_EXISTS);
                    }
                })
                .orElseGet(() -> createTmpMember(gameName, tag, tiers)); // 없으면 새로 생성
    }

    /**
     * 사용자의 updated_at 얻기
     *
     * @param member 사용자
     * @return 최종 수정된 updated_at
     */
    public LocalDateTime getMemberUpdatedAt(Member member) {
        return member.getUpdatedAt();
    }

}
