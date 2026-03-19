package com.gamegoo.gamegoo_v2.integration.rollbti;

import com.gamegoo.gamegoo_v2.account.member.domain.LoginType;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiCompatibilityOrder;
import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.repository.MemberRollBtiProfileRepository;
import com.gamegoo.gamegoo_v2.rollbti.service.RollBtiFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RollBtiFacadeServiceTest {

    @Autowired
    private RollBtiFacadeService rollBtiFacadeService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRollBtiProfileRepository memberRollBtiProfileRepository;

    @AfterEach
    void tearDown() {
        memberRollBtiProfileRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

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

    private Member createMember(int index, Tier tier) {
        return Member.createForGeneral(
                "rollbti" + index + "@test.com",
                "password",
                LoginType.GENERAL,
                "rollbti" + index,
                "KR" + index,
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
