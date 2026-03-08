package com.gamegoo.gamegoo_v2.integration.rollbti;

import com.gamegoo.gamegoo_v2.account.member.domain.LoginType;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.content.board.repository.BoardRepository;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiCompatibilityOrder;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
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
    private BoardRepository boardRepository;

    @AfterEach
    void tearDown() {
        boardRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("티어 필터 추천은 첫 페이지에 후보가 부족해도 다음 페이지까지 조회해 size를 채운다")
    void getRecommendationsByType_shouldCollectNextPagesWhenTierCandidatesAreNotInFirstPage() {
        List<Long> goldMemberIds = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Member goldMember = memberRepository.save(createMember(i, Tier.GOLD));
            goldMemberIds.add(goldMember.getId());
            boardRepository.save(createBoard(goldMember));
        }

        for (int i = 0; i < 100; i++) {
            Member silverMember = memberRepository.save(createMember(100 + i, Tier.SILVER));
            boardRepository.save(createBoard(silverMember));
        }

        RollBtiRecommendationResponse response = rollBtiFacadeService.getRecommendationsByType(
                RollBtiType.ADCI,
                20,
                RollBtiCompatibilityOrder.HIGH,
                Tier.GOLD,
                null
        );

        assertThat(response.getRecommendations()).hasSize(20);
        assertThat(response.getRecommendations())
                .extracting(recommendation -> recommendation.getMemberId())
                .containsExactlyInAnyOrderElementsOf(goldMemberIds);
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

    private Board createBoard(Member member) {
        return Board.create(
                member,
                GameMode.SOLO,
                Position.TOP,
                Position.JUNGLE,
                List.of(Position.MID),
                Mike.AVAILABLE,
                "rollbti test board"
        );
    }
}
