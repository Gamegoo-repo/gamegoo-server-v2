package com.gamegoo.gamegoo_v2.content.board.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.Mike;
import com.gamegoo.gamegoo_v2.account.member.domain.Position;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.account.member.service.MemberService;
import com.gamegoo.gamegoo_v2.external.riot.dto.TierDetails;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotAuthService;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotInfoService;
import com.gamegoo.gamegoo_v2.external.riot.service.RiotRecordService;
import com.gamegoo.gamegoo_v2.account.member.domain.MemberRecentStats;
import com.gamegoo.gamegoo_v2.external.riot.domain.ChampionStats;
import com.gamegoo.gamegoo_v2.account.member.service.MemberChampionService;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.content.board.dto.request.BoardInsertRequest;
import com.gamegoo.gamegoo_v2.content.board.dto.request.BoardUpdateRequest;
import com.gamegoo.gamegoo_v2.content.board.dto.request.GuestBoardInsertRequest;
import com.gamegoo.gamegoo_v2.content.board.dto.request.GuestBoardUpdateRequest;
import com.gamegoo.gamegoo_v2.content.board.dto.request.GuestBoardDeleteRequest;
import com.gamegoo.gamegoo_v2.content.board.dto.response.*;
import com.gamegoo.gamegoo_v2.core.common.validator.BanValidator;
import com.gamegoo.gamegoo_v2.core.exception.BoardException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.matching.domain.GameMode;
import com.gamegoo.gamegoo_v2.social.block.service.BlockService;
import com.gamegoo.gamegoo_v2.social.friend.service.FriendService;
import com.gamegoo.gamegoo_v2.social.manner.service.MannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardFacadeService {

    private final BoardService boardService;
    private final BoardGameStyleService boardGameStyleService;
    private final FriendService friendService;
    private final BlockService blockService;
    private final ProfanityCheckService profanityCheckService;
    private final MannerService mannerService;
    private final BanValidator banValidator;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final RiotAuthService riotAuthService;
    private final RiotInfoService riotInfoService;
    private final RiotRecordService riotRecordService;
    private final MemberChampionService memberChampionService;

    /**
     * 게시글 생성 (파사드)
     * - DTO -> 엔티티 변환 및 저장
     * - 연관된 GameStyle(BoardGameStyle) 매핑 처리
     * - 결과를 BoardInsertResponse로 변환하여 반환
     */
    @Transactional
    public BoardInsertResponse createBoard(BoardInsertRequest request, Member member) {
        // 게시글 작성 제재 검증
        banValidator.throwIfBannedFromPosting(member);

        profanityCheckService.validateProfanity(request.getContents());
        Board board = boardService.createAndSaveBoard(request, member);
        boardGameStyleService.mapGameStylesToBoard(board, request.getGameStyles());

        return BoardInsertResponse.of(board, member);
    }

    /**
     * 게스트 게시글 생성 (파사드)
     * - 소환사명 + 태그로 임시 멤버 생성/재사용
     * - 라이엇 API 호출하여 실제 게임 정보 가져오기
     * - 임시 멤버로 게시글 생성
     */
    @Transactional
    public BoardInsertResponse createGuestBoard(GuestBoardInsertRequest request, String gameName, String tag) {
        // 라이엇 API 호출하여 puuid 및 티어 정보 가져오기
        String puuid = riotAuthService.getPuuid(gameName, tag);
        List<TierDetails> tiers = null;
        RiotRecordService.Recent30GameStatsResponse recentStats = null;

        if (puuid != null) {
            tiers = riotInfoService.getTierWinrateRank(puuid);
            recentStats = riotRecordService.getRecent30GameStats(gameName, puuid);

        }

        // 임시 멤버 생성 또는 재사용
        Member tmpMember = memberService.getOrCreateTmpMember(gameName, tag, tiers);

        // MemberChampion 처리 (임시 멤버에게 최신 챔피언 통계 추가/업데이트)
        if (puuid != null) {
            List<ChampionStats> preferChampionStats = riotRecordService.getPreferChampionfromMatch(gameName, puuid);
            if (preferChampionStats != null && !preferChampionStats.isEmpty()) {
                memberChampionService.saveMemberChampions(tmpMember, preferChampionStats);
            }
        }

        // MemberRecentStats 항상 최신 데이터로 업데이트
        MemberRecentStats existingStats = tmpMember.getMemberRecentStats();
        if (existingStats != null) {
            // 기존 통계 업데이트
            if (recentStats != null) {
                existingStats.update(
                        recentStats.getRecTotalWins(),
                        recentStats.getRecTotalLosses(),
                        recentStats.getRecWinRate(),
                        recentStats.getRecAvgKDA(),
                        recentStats.getRecAvgKills(),
                        recentStats.getRecAvgDeaths(),
                        recentStats.getRecAvgAssists(),
                        recentStats.getRecAvgCsPerMinute(),
                        recentStats.getRecTotalCs()
                );
            } else {
                // recentStats가 null인 경우 기본값으로 업데이트
                existingStats.update(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);
            }
        } else {
            // 새로운 통계 생성
            MemberRecentStats memberRecentStats;
            if (recentStats != null) {
                memberRecentStats = MemberRecentStats.builder()
                        .recTotalWins(recentStats.getRecTotalWins())
                        .recTotalLosses(recentStats.getRecTotalLosses())
                        .recWinRate(recentStats.getRecWinRate())
                        .recAvgKDA(recentStats.getRecAvgKDA())
                        .recAvgKills(recentStats.getRecAvgKills())
                        .recAvgDeaths(recentStats.getRecAvgDeaths())
                        .recAvgAssists(recentStats.getRecAvgAssists())
                        .recAvgCsPerMinute(recentStats.getRecAvgCsPerMinute())
                        .recTotalCs(recentStats.getRecTotalCs())
                        .build();
            } else {
                // recentStats가 null인 경우 기본값으로 생성
                memberRecentStats = MemberRecentStats.builder()
                        .recTotalWins(0)
                        .recTotalLosses(0)
                        .recWinRate(0.0)
                        .recAvgKDA(0.0)
                        .recAvgKills(0.0)
                        .recAvgDeaths(0.0)
                        .recAvgAssists(0.0)
                        .recAvgCsPerMinute(0.0)
                        .recTotalCs(0)
                        .build();
            }
            tmpMember.setMemberRecentStats(memberRecentStats);
        }

        profanityCheckService.validateProfanity(request.getContents());
        Board board = boardService.createAndSaveGuestBoard(request, tmpMember, request.getPassword());
        boardGameStyleService.mapGameStylesToBoard(board, request.getGameStyles());

        return BoardInsertResponse.ofGuest(board);
    }

    /**
     * 게시판 글 목록 조회 (파사드)
     */
    public BoardResponse getBoardList(Long memberId, GameMode gameMode, Tier tier, Position mainP, Position subP, Mike mike, int pageIdx) {
        if (mainP == null) {
            mainP = Position.ANY;
        }
        if (subP == null) {
            subP = Position.ANY;
        }
        Member member = memberId != null ? memberService.findMemberById(memberId) : null;

        Page<Board> boardPage = boardService.getBoardsWithPagination(gameMode, tier, mainP, subP, mike, pageIdx);

        Map<Long, Boolean> blockedMap = Collections.emptyMap();
        if(member != null && !boardPage.getContent().isEmpty()){
            List<Long> memberIds = boardPage.getContent().stream()
                    .map(board -> board.getMember().getId())
                    .distinct()
                    .collect(Collectors.toList());
            blockedMap = blockService.hasBlockedTargetMembersBatch(member, memberIds);
        }
        return BoardResponse.of(boardPage, blockedMap);
    }

    /**
     * 회원 게시판 글 단건 조회 (파사드)
     * - "회원 전용" 조회 로직
     */
    public BoardByIdResponseForMember getBoardByIdForMember(Long boardId, Member viewer) {

        Board board = boardService.findBoard(boardId);

        boolean isBlocked = blockService.isBlocked(viewer, board.getMember());
        boolean isFriend = friendService.isFriend(viewer, board.getMember());
        Long friendRequestMemberId = friendService.getFriendRequestMemberId(viewer, board.getMember());

        return BoardByIdResponseForMember.of(board, isBlocked, isFriend, friendRequestMemberId, mannerService);
    }

    /**
     * 비회원 게시판 글 단건 조회 (파사드)
     * - "비회원 전용" 조회 로직
     */
    public BoardByIdResponse getBoardById(Long boardId) {

        Board board = boardService.findBoard(boardId);

        return BoardByIdResponse.of(board);
    }

    /**
     * 게시글 수정 (파사드)
     *
     * @param request 게시글 수정 요청 DTO
     * @param member  현재 로그인한 Member
     * @param boardId 수정할 게시글 ID
     * @return 수정된 게시글 정보(Response)
     */
    @Transactional
    public BoardUpdateResponse updateBoard(BoardUpdateRequest request, Member member, Long boardId) {
        // 게시글 작성 제재 검증
        banValidator.throwIfBannedFromPosting(member);

        profanityCheckService.validateProfanity(request.getContents());
        Board board = boardService.updateBoard(request, member.getId(), boardId);
        boardGameStyleService.updateBoardGameStyles(board, request.getGameStyles());

        return BoardUpdateResponse.of(board);

    }

    /**
     * 게시글 삭제 (파사드)
     *
     * @param member  현재 로그인한 Member
     * @param boardId 삭제할 게시글 ID
     */
    @Transactional
    public void deleteBoard(Member member, Long boardId) {
        boardService.deleteBoard(boardId, member.getId());
    }

    /**
     * 비회원 게시글 수정 (파사드)
     */
    @Transactional
    public BoardUpdateResponse updateGuestBoard(GuestBoardUpdateRequest request, Long boardId) {
        profanityCheckService.validateProfanity(request.getContents());
        Board board = boardService.updateGuestBoard(request, boardId);
        boardGameStyleService.updateBoardGameStyles(board, request.getGameStyles());

        return BoardUpdateResponse.of(board);
    }

    /**
     * 비회원 게시글 삭제 (파사드)
     */
    @Transactional
    public void deleteGuestBoard(Long boardId, GuestBoardDeleteRequest request) {
        boardService.deleteGuestBoard(boardId, request.getPassword());
    }

    /**
     * 내가 작성한 게시글 목록 조회 (파사드)
     */
    public MyBoardResponse getMyBoardList(Member member, int pageIdx) {
        Page<Board> boardPage = boardService.getMyBoards(member.getId(), pageIdx);
        return MyBoardResponse.of(boardPage);
    }

    /**
     * 내가 작성한 게시글 목록 조회(커서)
     */
    public MyBoardCursorResponse getMyBoardCursorList(Member member, LocalDateTime cursor) {
        Slice<Board> boardSlice = boardService.getMyBoards(member.getId(), cursor);
        return MyBoardCursorResponse.of(boardSlice);
    }

    /**
     * 게시글 끌올(bump) 기능 (파사드)
     * 사용자가 "끌올" 버튼을 누르면 해당 게시글의 bumpTime을 업데이트합니다.
     * 단, 마지막 끌올 후 5분이 지나지 않았다면 예외를 발생시킵니다.
     */

    @Transactional
    public BoardBumpResponse bumpBoard(Long boardId, Member member) {
        // 게시글 작성 제재 검증
        banValidator.throwIfBannedFromPosting(member);

        Board board = boardService.bumpBoard(boardId, member.getId());
        return BoardBumpResponse.of(board.getId(), board.getBumpTime());
    }

    /**
     * 최신글 자동 끌올 기능
     * 사용자가 작성한 가장 최근 게시글을 자동으로 끌올합니다.
     */
    @Transactional
    public BoardBumpResponse bumpLatestBoard(Member member) {
        // 게시글 작성 제재 검증
        banValidator.throwIfBannedFromPosting(member);

        // 최신 게시글 조회
        Board latestBoard = boardService.findLatestBoardByMember(member.getId());

        // 끌올 처리
        Board board = boardService.bumpBoard(latestBoard.getId(), member.getId());
        return BoardBumpResponse.of(board.getId(), board.getBumpTime());
    }

    /**
     * 전체 게시글 커서 기반 조회 (Secondary Cursor)
     */
    public BoardCursorResponse getAllBoardsWithCursor(
            LocalDateTime cursor,
            Long cursorId,
            GameMode gameMode,
            Tier tier,
            Position position1,
            Position position2,
            Mike mike,
            Long memberId) {

        Member member = memberId != null ? memberService.findMemberById(memberId) : null;
        Slice<Board> boardSlice = boardService.getAllBoardsWithCursor(cursor, cursorId, gameMode, tier, position1, position2, mike);
        Map<Long, Boolean> blockedMap = Collections.emptyMap();
        if(member != null && !boardSlice.getContent().isEmpty()){
            List<Long> memberIds = boardSlice.getContent().stream()
                    .map(board -> board.getMember().getId())
                    .distinct()
                    .collect(Collectors.toList());
            blockedMap = blockService.hasBlockedTargetMembersBatch(member, memberIds);
        }
        return BoardCursorResponse.of(boardSlice, blockedMap);
    }

}
