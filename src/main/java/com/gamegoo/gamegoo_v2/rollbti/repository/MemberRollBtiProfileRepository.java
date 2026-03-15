package com.gamegoo.gamegoo_v2.rollbti.repository;

import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRollBtiProfileRepository extends JpaRepository<MemberRollBtiProfile, Long> {

    Optional<MemberRollBtiProfile> findByMember_Id(Long memberId);

    List<MemberRollBtiProfile> findAllByMember_IdIn(Collection<Long> memberIds);

    @Query("""
            SELECT p
            FROM MemberRollBtiProfile p
            JOIN p.member m
            WHERE m.blind = false
              AND (:excludeMemberId IS NULL OR m.id <> :excludeMemberId)
              AND (:tier IS NULL OR m.soloTier = :tier OR m.freeTier = :tier)
            ORDER BY COALESCE(p.updatedAt, p.createdAt) DESC, m.id DESC
            """)
    List<MemberRollBtiProfile> findRecommendationCandidates(
            @Param("tier") Tier tier,
            @Param("excludeMemberId") Long excludeMemberId,
            Pageable pageable);
}
