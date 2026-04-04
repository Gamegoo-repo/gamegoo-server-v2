package com.gamegoo.gamegoo_v2.integration.rollbti;

import com.gamegoo.gamegoo_v2.account.member.domain.LoginType;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiCompatibilityOrder;
import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiRecommendationBucket;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationCursorResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.repository.MemberRollBtiProfileRepository;
import com.gamegoo.gamegoo_v2.rollbti.service.RollBtiFacadeService;
import com.gamegoo.gamegoo_v2.social.block.service.BlockService;
import com.gamegoo.gamegoo_v2.social.friend.service.FriendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RollBtiFacadeServiceTest {

    @Autowired
    private RollBtiFacadeService rollBtiFacadeService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRollBtiProfileRepository memberRollBtiProfileRepository;

    @Autowired
    private FriendService friendService;

    @Autowired
    private BlockService blockService;

    @Test
    @DisplayName("게시글이 없어도 롤BTI 프로필이 있으면 회원 기반 추천 대상에 포함된다")
    void getRecommendationsByType_shouldReturnMembersWithoutBoards() {
        List<Long> goldMemberIds = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Member goldMember = memberRepository.save(createMember(i, Tier.GOLD));
            goldMemberIds.add(goldMember.getId());
            memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(goldMember, RollBtiType.ADCI));
        }

        for (int i = 0; i < 100; i++) {
            Member silverMember = memberRepository.save(createMember(100 + i, Tier.SILVER));
            memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(silverMember, RollBtiType.FSCB));
        }

        RollBtiRecommendationResponse response = rollBtiFacadeService.getRecommendationsByType(
                RollBtiType.ADCI,
                20,
                1,
                RollBtiCompatibilityOrder.HIGH,
                Tier.GOLD,
                null
        );

        assertThat(response.getRecommendations()).hasSize(20);
        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getMemberId())
                .containsExactlyInAnyOrderElementsOf(goldMemberIds);
        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getUpdatedAt())
                .doesNotContainNull();
    }

    @Test
    @DisplayName("추천 API는 page 기반으로 무한스크롤에 필요한 다음 페이지 데이터를 내려준다")
    void getRecommendationsByType_shouldSupportInfiniteScroll() {
        for (int i = 0; i < 25; i++) {
            Member member = memberRepository.save(createMember(i, Tier.GOLD));
            memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(member, RollBtiType.ADCI));
        }

        RollBtiRecommendationResponse firstPage = rollBtiFacadeService.getRecommendationsByType(
                RollBtiType.ADCI,
                20,
                1,
                RollBtiCompatibilityOrder.HIGH,
                Tier.GOLD,
                null
        );
        RollBtiRecommendationResponse secondPage = rollBtiFacadeService.getRecommendationsByType(
                RollBtiType.ADCI,
                20,
                2,
                RollBtiCompatibilityOrder.HIGH,
                Tier.GOLD,
                null
        );

        assertThat(firstPage.getPage()).isEqualTo(1);
        assertThat(firstPage.getRecommendations()).hasSize(20);
        assertThat(firstPage.isHasNext()).isTrue();

        assertThat(secondPage.getPage()).isEqualTo(2);
        assertThat(secondPage.getRecommendations()).hasSize(5);
        assertThat(secondPage.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("비회원 롤BTI 피드는 타입 없이 이름순으로 회원 카드를 반환한다")
    void getPublicRecommendations_shouldReturnAlphabeticalCards() {
        Member charlie = memberRepository.save(createMember("charlie", "KR3", Tier.GOLD));
        Member alpha = memberRepository.save(createMember("alpha", "KR1", Tier.GOLD));
        Member bravo = memberRepository.save(createMember("bravo", "KR2", Tier.GOLD));

        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(charlie, RollBtiType.ADTB));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(alpha, RollBtiType.ADCI));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(bravo, RollBtiType.FSCB));

        RollBtiPublicRecommendationResponse response = rollBtiFacadeService.getPublicRecommendations(null, 20, 1, Tier.GOLD);

        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getGameName())
                .containsExactly("alpha", "bravo", "charlie");
        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getRollBtiType())
                .containsExactly(RollBtiType.ADCI, RollBtiType.FSCB, RollBtiType.ADTB);
    }

    @Test
    @DisplayName("비회원 롤BTI 피드 커서 조회는 compatibilityScore 없이 다음 커서를 반환한다")
    void getPublicRecommendationsWithCursor_shouldSupportCursorPaging() {
        for (int i = 0; i < 25; i++) {
            Member member = memberRepository.save(createMember(String.format("member%02d", i), "KR" + i, Tier.GOLD));
            memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(member, RollBtiType.ADCI));
        }

        RollBtiPublicRecommendationCursorResponse firstPage =
                rollBtiFacadeService.getPublicRecommendationsWithCursor(null, 20, null, Tier.GOLD);
        RollBtiPublicRecommendationCursorResponse secondPage =
                rollBtiFacadeService.getPublicRecommendationsWithCursor(null, 20, firstPage.getNextCursorMemberId(), Tier.GOLD);

        assertThat(firstPage.getRecommendations()).hasSize(20);
        assertThat(firstPage.isHasNext()).isTrue();
        assertThat(firstPage.getNextCursorMemberId()).isNotNull();

        assertThat(secondPage.getRecommendations()).hasSize(5);
        assertThat(secondPage.isHasNext()).isFalse();
        assertThat(secondPage.getNextCursorMemberId()).isNull();
    }

    @Test
    @DisplayName("회원용 추천 버킷 커서 조회는 GOOD/NORMAL/BAD를 독립적으로 반환한다")
    void getRecommendationsByTypeAndBucketWithCursor_shouldFilterByBucket() {
        Member sameType = memberRepository.save(createMember("same", "KR1", Tier.GOLD));
        Member goodType = memberRepository.save(createMember("good", "KR2", Tier.GOLD));
        Member normalType = memberRepository.save(createMember("normal", "KR3", Tier.GOLD));
        Member badType = memberRepository.save(createMember("bad", "KR4", Tier.GOLD));

        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(sameType, RollBtiType.ADCI));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(goodType, RollBtiType.ADTB));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(normalType, RollBtiType.ASCB));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(badType, RollBtiType.FDTB));

        var goodResponse = rollBtiFacadeService.getRecommendationsByTypeAndBucketWithCursor(
                RollBtiType.ADCI, RollBtiRecommendationBucket.GOOD, 20, null, Tier.GOLD, null);
        var normalResponse = rollBtiFacadeService.getRecommendationsByTypeAndBucketWithCursor(
                RollBtiType.ADCI, RollBtiRecommendationBucket.NORMAL, 20, null, Tier.GOLD, null);
        var badResponse = rollBtiFacadeService.getRecommendationsByTypeAndBucketWithCursor(
                RollBtiType.ADCI, RollBtiRecommendationBucket.BAD, 20, null, Tier.GOLD, null);

        assertThat(goodResponse.getRecommendations())
                .extracting(recommendation -> recommendation.getCompatibilityScore())
                .containsOnly(95);
        assertThat(normalResponse.getRecommendations())
                .extracting(recommendation -> recommendation.getCompatibilityScore())
                .allMatch(score -> score >= 50 && score < 90);
        assertThat(badResponse.getRecommendations())
                .extracting(recommendation -> recommendation.getCompatibilityScore())
                .containsOnly(20);
    }

    @Test
    @DisplayName("로그인 사용자가 차단한 유저는 공개 피드에서 제외된다")
    void getPublicRecommendations_shouldExcludeBlockedMembers() {
        Member requester = memberRepository.save(createMember("requester", "KR0", Tier.GOLD));
        Member blockedMember = memberRepository.save(createMember("blocked", "KR1", Tier.GOLD));
        Member visibleMember = memberRepository.save(createMember("visible", "KR2", Tier.GOLD));

        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(blockedMember, RollBtiType.ADCI));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(visibleMember, RollBtiType.FSCB));

        blockService.blockMember(requester, blockedMember);

        RollBtiPublicRecommendationResponse response =
                rollBtiFacadeService.getPublicRecommendations(requester, 20, 1, Tier.GOLD);

        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getMemberId())
                .containsExactly(visibleMember.getId());
    }

    @Test
    @DisplayName("나에게 친구 요청을 보낸 유저는 회원 추천에서 가장 먼저 노출된다")
    void getMyRecommendations_shouldPrioritizeReceivedFriendRequest() {
        Member requester = memberRepository.save(createMember("requester", "KR0", Tier.GOLD));
        Member requesterTarget = memberRepository.save(createMember("requesterTarget", "KR9", Tier.GOLD));
        Member requestSender = memberRepository.save(createMember("sender", "KR1", Tier.GOLD));
        Member other = memberRepository.save(createMember("other", "KR2", Tier.GOLD));

        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(requester, RollBtiType.ADCI));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(requesterTarget, RollBtiType.ADTB));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(requestSender, RollBtiType.ADTB));
        memberRollBtiProfileRepository.save(MemberRollBtiProfile.create(other, RollBtiType.ADTB));

        friendService.sendFriendRequest(requestSender, requester);

        RollBtiRecommendationResponse response = rollBtiFacadeService.getMyRecommendations(
                requester,
                20,
                1,
                RollBtiCompatibilityOrder.HIGH,
                Tier.GOLD
        );

        assertThat(response.getRecommendations().get(0).getMemberId()).isEqualTo(requestSender.getId());
        assertThat(response.getRecommendations().get(0).getFriendRequestReceived()).isTrue();
    }

    private Member createMember(int index, Tier tier) {
        return createMember("rollbti" + index, "KR" + index, tier);
    }

    private Member createMember(String gameName, String tag, Tier tier) {
        return Member.createForGeneral(
                gameName + "@test.com",
                "password",
                LoginType.GENERAL,
                gameName,
                tag,
                tier,
                1,
                50.0,
                tier,
                1,
                50.0,
                100,
                100,
                true
        );
    }
}
