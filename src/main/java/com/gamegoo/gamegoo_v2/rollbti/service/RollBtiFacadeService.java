package com.gamegoo.gamegoo_v2.rollbti.service;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.content.board.domain.Board;
import com.gamegoo.gamegoo_v2.content.board.repository.BoardRepository;
import com.gamegoo.gamegoo_v2.core.exception.MemberException;
import com.gamegoo.gamegoo_v2.core.exception.RollBtiException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiEvent;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiEventRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiCompatibilityResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiProfileResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendedMemberResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiTypeSummaryResponse;
import com.gamegoo.gamegoo_v2.rollbti.repository.MemberRollBtiProfileRepository;
import com.gamegoo.gamegoo_v2.rollbti.repository.RollBtiEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RollBtiFacadeService {

    private static final int DEFAULT_RECOMMENDATION_SIZE = 20;
    private static final int MAX_RECOMMENDATION_SIZE = 50;
    private static final int FETCH_MULTIPLIER = 5;
    private static final int MIN_FETCH_SIZE = 100;
    private static final int MAX_FETCH_SIZE = 500;
    private static final int GOOD_MATCH_SCORE = 95;
    private static final int SAME_TYPE_SCORE = 75;
    private static final int NORMAL_SCORE = 60;
    private static final int NO_PROFILE_SCORE = 50;
    private static final int BAD_MATCH_SCORE = 20;
    private static final String EVENT_SAVED_MESSAGE = "롤BTI 이벤트가 저장되었습니다.";

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final MemberRollBtiProfileRepository memberRollBtiProfileRepository;
    private final RollBtiEventRepository rollBtiEventRepository;
    private final RollBtiCatalogService rollBtiCatalogService;

    @Transactional
    public RollBtiProfileResponse saveMyType(Member member, RollBtiSaveRequest request) {
        return upsertMemberType(member, request.getType());
    }

    @Transactional
    public RollBtiProfileResponse saveTypeByMemberId(Long memberId, RollBtiSaveRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        return upsertMemberType(member, request.getType());
    }

    public RollBtiProfileResponse getMyType(Member member) {
        return RollBtiProfileResponse.of(getProfileOrThrow(member.getId()));
    }

    public RollBtiProfileResponse getTypeByMemberId(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        return RollBtiProfileResponse.of(getProfileOrThrow(memberId));
    }

    public RollBtiTypeSummaryResponse getTypeSummary(RollBtiType type) {
        return rollBtiCatalogService.getTypeSummary(type);
    }

    public RollBtiCompatibilityResponse getCompatibility(RollBtiType type) {
        return rollBtiCatalogService.getCompatibility(type);
    }

    public RollBtiRecommendationResponse getMyRecommendations(Member member, Integer size) {
        RollBtiType type = getProfileOrThrow(member.getId()).getRollBtiType();
        return getRecommendations(type, size, member.getId());
    }

    public RollBtiRecommendationResponse getRecommendationsByType(RollBtiType type, Integer size, Long excludeMemberId) {
        return getRecommendations(type, size, excludeMemberId);
    }

    @Transactional
    public String trackEvent(RollBtiEventRequest request) {
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
        }

        RollBtiEvent event = RollBtiEvent.create(
                member,
                request.getEventType(),
                request.getRollBtiType(),
                request.getSessionId(),
                request.getEventSource());
        rollBtiEventRepository.save(event);
        return EVENT_SAVED_MESSAGE;
    }

    private RollBtiRecommendationResponse getRecommendations(RollBtiType requesterType, Integer size, Long excludeMemberId) {
        int normalizedSize = normalizeSize(size);
        int fetchSize = normalizeFetchSize(normalizedSize);

        List<Board> recentBoards = boardRepository.findRecentBoardsWithMember(PageRequest.of(0, fetchSize));

        // 최신순으로 조회된 게시글에서 회원당 최신 게시글 1개만 유지
        Map<Long, Board> latestBoardByMemberId = recentBoards.stream()
                .filter(board -> board.getMember() != null)
                .filter(board -> excludeMemberId == null || !Objects.equals(board.getMember().getId(), excludeMemberId))
                .collect(Collectors.toMap(
                        board -> board.getMember().getId(),
                        board -> board,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new));

        List<Board> boards = new ArrayList<>(latestBoardByMemberId.values());

        Map<Long, RollBtiType> targetTypeByMemberId = getTargetTypeByMemberId(boards);
        Set<RollBtiType> goodMatches = rollBtiCatalogService.getGoodMatches(requesterType);
        Set<RollBtiType> badMatches = rollBtiCatalogService.getBadMatches(requesterType);

        List<RollBtiRecommendedMemberResponse> recommendations = boards.stream()
                .map(board -> {
                    Long memberId = board.getMember().getId();
                    RollBtiType targetType = targetTypeByMemberId.get(memberId);
                    int compatibilityScore = calculateCompatibilityScore(requesterType, targetType, goodMatches,
                            badMatches);
                    return RollBtiRecommendedMemberResponse.of(
                            board.getId(),
                            memberId,
                            board.getMember().getGameName(),
                            board.getMember().getTag(),
                            board.getMember().getProfileImage(),
                            board.getMember().getMannerLevel(),
                            board.getGameMode(),
                            board.getMainP(),
                            board.getSubP(),
                            board.getMike(),
                            board.getContent(),
                            targetType,
                            compatibilityScore,
                            board.getActivityTime());
                })
                .sorted(Comparator.comparingInt(RollBtiRecommendedMemberResponse::getCompatibilityScore).reversed()
                        .thenComparing(RollBtiRecommendedMemberResponse::getActivityTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RollBtiRecommendedMemberResponse::getBoardId, Comparator.reverseOrder()))
                .limit(normalizedSize)
                .collect(Collectors.toList());

        return RollBtiRecommendationResponse.of(requesterType, normalizedSize, recommendations);
    }

    private Map<Long, RollBtiType> getTargetTypeByMemberId(List<Board> boards) {
        Set<Long> memberIds = boards.stream()
                .map(board -> board.getMember().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return memberRollBtiProfileRepository.findAllByMember_IdIn(memberIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getMember().getId(),
                        MemberRollBtiProfile::getRollBtiType,
                        (existing, ignored) -> existing));
    }

    private int calculateCompatibilityScore(RollBtiType requesterType, RollBtiType targetType,
                                            Set<RollBtiType> goodMatches, Set<RollBtiType> badMatches) {
        if (targetType == null) {
            return NO_PROFILE_SCORE;
        }
        if (targetType == requesterType) {
            return SAME_TYPE_SCORE;
        }
        if (goodMatches.contains(targetType)) {
            return GOOD_MATCH_SCORE;
        }
        if (badMatches.contains(targetType)) {
            return BAD_MATCH_SCORE;
        }
        return NORMAL_SCORE;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_RECOMMENDATION_SIZE;
        }
        if (size < 1) {
            throw new RollBtiException(ErrorCode.ROLL_BTI_SIZE_BAD_REQUEST);
        }
        return Math.min(size, MAX_RECOMMENDATION_SIZE);
    }

    private int normalizeFetchSize(int size) {
        int rawFetchSize = size * FETCH_MULTIPLIER;
        return Math.min(MAX_FETCH_SIZE, Math.max(MIN_FETCH_SIZE, rawFetchSize));
    }

    @Transactional
    protected RollBtiProfileResponse upsertMemberType(Member member, RollBtiType type) {
        MemberRollBtiProfile profile = memberRollBtiProfileRepository.findByMember_Id(member.getId())
                .map(existing -> {
                    existing.updateType(type);
                    return existing;
                })
                .orElseGet(() -> MemberRollBtiProfile.create(member, type));

        MemberRollBtiProfile savedProfile = memberRollBtiProfileRepository.save(profile);
        return RollBtiProfileResponse.of(savedProfile);
    }

    private MemberRollBtiProfile getProfileOrThrow(Long memberId) {
        return memberRollBtiProfileRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new RollBtiException(ErrorCode.ROLL_BTI_PROFILE_NOT_FOUND));
    }
}
