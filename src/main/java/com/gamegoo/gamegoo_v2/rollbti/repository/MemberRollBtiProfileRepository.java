package com.gamegoo.gamegoo_v2.rollbti.repository;

import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRollBtiProfileRepository extends JpaRepository<MemberRollBtiProfile, Long> {

    Optional<MemberRollBtiProfile> findByMember_Id(Long memberId);

    List<MemberRollBtiProfile> findAllByMember_IdIn(Collection<Long> memberIds);
}

