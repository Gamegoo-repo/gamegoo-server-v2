package com.gamegoo.gamegoo_v2.content.board.repository;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("SELECT b FROM Board b LEFT JOIN b.member m WHERE " +
            "b.deleted = false AND " +
            "(:gameMode IS NULL OR b.gameMode = :gameMode) AND " +
            "(:tier IS NULL OR (CASE WHEN b.gameMode = com.gamegoo.gamegoo_v2.matching.domain.GameMode.FREE THEN m.freeTier ELSE m.soloTier END) = :tier) AND " +
            "(:positionList IS NULL OR b.mainP IN :positionList OR b.subP IN :positionList) AND " +
            "(:mike IS NULL OR b.mike = :mike) " +
            "ORDER BY COALESCE(b.bumpTime, b.createdAt) DESC")
    Page<Board> findByGameModeAndTierAndMainPInAndSubPInAndMikeAndDeletedFalse(
            @Param("gameMode") GameMode gameMode,
            @Param("tier") Tier tier,
            @Param("positionList") List<Position> positionList,
            @Param("mike") Mike mike,
            Pageable pageable);

    Optional<Board> findByIdAndDeleted(Long boardId, boolean b);

    Optional<Board> findTopByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId);

    Page<Board> findByMemberIdAndDeletedFalse(Long memberId, Pageable pageable);

    @Query("SELECT b FROM Board b " +
           "WHERE b.member.id = :memberId " +
           "AND b.deleted = false " +
           "AND (:activityTime IS NULL OR COALESCE(b.bumpTime, b.createdAt) < :activityTime) " +
           "ORDER BY COALESCE(b.bumpTime, b.createdAt) DESC")
    Slice<Board> findByMemberIdAndActivityTimeLessThan(
            @Param("memberId") Long memberId,
            @Param("activityTime") LocalDateTime activityTime,
            Pageable pageable);

    @Query("SELECT b FROM Board b LEFT JOIN b.member m " +
           "WHERE b.deleted = false " +
           "AND (" +
           "  :activityTime IS NULL " +
           "  OR COALESCE(b.bumpTime, b.createdAt) < :activityTime " +
           "  OR (COALESCE(b.bumpTime, b.createdAt) = :activityTime AND b.id < :cursorId)" +
           ") " +
           "AND (:gameMode IS NULL OR b.gameMode = :gameMode) " +
           "AND (:tier IS NULL OR (CASE WHEN b.gameMode = com.gamegoo.gamegoo_v2.matching.domain.GameMode.FREE THEN m.freeTier ELSE m.soloTier END) = :tier) " +
           "AND (:positionList IS NULL OR b.mainP IN :positionList OR b.subP IN :positionList) " +
           "AND (:mike IS NULL OR b.mike = :mike) " +
           "ORDER BY COALESCE(b.bumpTime, b.createdAt) DESC, b.id DESC")
    Slice<Board> findAllBoardsWithCursor(
            @Param("activityTime") LocalDateTime activityTime,
            @Param("cursorId") Long cursorId,
            @Param("gameMode") GameMode gameMode,
            @Param("tier") Tier tier,
            @Param("positionList") List<Position> positionList,
            @Param("mike") Mike mike,
            Pageable pageable);

    @EntityGraph(attributePaths = "member")
    @Query("SELECT b FROM Board b " +
           "WHERE b.deleted = false " +
           "ORDER BY COALESCE(b.bumpTime, b.createdAt) DESC, b.id DESC")
    List<Board> findRecentBoardsWithMember(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Board b SET b.deleted = true where b.member = :member")
    void deleteAllByMember(@Param("member") Member member);

}
