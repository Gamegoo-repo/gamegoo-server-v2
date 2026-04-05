package com.gamegoo.gamegoo_v2.rollbti.repository;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RollBtiEventRepository extends JpaRepository<RollBtiEvent, Long> {

    @Query(value = """
            SELECT COUNT(DISTINCT CASE
                WHEN member_id IS NOT NULL THEN CONCAT('M:', member_id)
                WHEN session_id IS NOT NULL THEN CONCAT('S:', session_id)
                ELSE NULL
            END)
            FROM roll_bti_event
            WHERE event_type = 'COMPLETE_TEST'
            """, nativeQuery = true)
    long countDistinctParticipantsByCompleteTest();
}
