package com.gamegoo.gamegoo_v2.rollbti.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.MemberChampion;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.content.board.dto.response.ChampionStatsResponse;
import com.gamegoo.gamegoo_v2.core.exception.MemberException;
import com.gamegoo.gamegoo_v2.core.exception.RollBtiException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiCompatibilityOrder;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiGuestResult;
import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiRecommendationBucket;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiEvent;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiEventRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiGuestResultSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.request.RollBtiSaveRequest;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiCompatibilityResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiGuestResultResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiGuestResultSaveResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiParticipationCountResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiProfileResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationCursorResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiPublicRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiMemberCardResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationCursorResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendedMemberResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiRecommendationResponse;
import com.gamegoo.gamegoo_v2.rollbti.dto.response.RollBtiTypeSummaryResponse;
import com.gamegoo.gamegoo_v2.rollbti.repository.RollBtiGuestResultRepository;
import com.gamegoo.gamegoo_v2.rollbti.repository.MemberRollBtiProfileRepository;
import com.gamegoo.gamegoo_v2.rollbti.repository.RollBtiEventRepository;
import com.gamegoo.gamegoo_v2.social.block.service.BlockService;
import com.gamegoo.gamegoo_v2.social.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    private static final int RESULT_ID_LENGTH = 9;
    private static final int RESULT_ID_RETRY_LIMIT = 20;
    private static final char[] RESULT_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final String EVENT_SAVED_MESSAGE = "롤BTI 이벤트가 저장되었습니다.";
    private static final String RESULT_ID_GENERATION_FAILED_MESSAGE = "롤BTI 결과 공유 ID 생성에 실패했습니다.";
    private static final String RESULT_PAYLOAD_SERIALIZATION_FAILED_MESSAGE = "롤BTI 결과 payload 직렬화에 실패했습니다.";
    private static final String RESULT_PAYLOAD_DESERIALIZATION_FAILED_MESSAGE = "롤BTI 결과 payload 역직렬화에 실패했습니다.";

    private final MemberRepository memberRepository;
    private final MemberRollBtiProfileRepository memberRollBtiProfileRepository;
    private final RollBtiEventRepository rollBtiEventRepository;
    private final RollBtiGuestResultRepository rollBtiGuestResultRepository;
    private final RollBtiGuestResultSaver rollBtiGuestResultSaver;
    private final RollBtiCatalogService rollBtiCatalogService;
    private final FriendService friendService;
    private final BlockService blockService;
    private final ObjectMapper objectMapper;

    @Value("${roll-bti.participant-count-adjustment:0}")
    private long participantCountAdjustment;

    private final SecureRandom secureRandom = new SecureRandom();

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

    public RollBtiRecommendationResponse getMyRecommendations(
            Member member,
            Integer size,
            Integer page,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier) {
        RollBtiType type = getProfileOrThrow(member.getId()).getRollBtiType();
        return getRecommendations(type, size, page, compatibilityOrder, tier, member);
    }

    public RollBtiRecommendationCursorResponse getMyRecommendationsWithCursor(
            Member member,
            Integer size,
            Long cursorMemberId,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier) {
        RollBtiType type = getProfileOrThrow(member.getId()).getRollBtiType();
        return getRecommendationsWithCursor(type, size, cursorMemberId, compatibilityOrder, tier, member);
    }

    public RollBtiRecommendationCursorResponse getMyRecommendationsByBucketWithCursor(
            Member member,
            RollBtiRecommendationBucket bucket,
            Integer size,
            Long cursorMemberId,
            Tier tier) {
        RollBtiType type = getProfileOrThrow(member.getId()).getRollBtiType();
        return getRecommendationsByBucketWithCursor(type, bucket, size, cursorMemberId, tier, member);
    }

    public RollBtiRecommendationResponse getRecommendationsByType(
            RollBtiType type,
            Integer size,
            Integer page,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier,
            Long excludeMemberId) {
        return getRecommendations(type, size, page, compatibilityOrder, tier, resolveRequesterMember(excludeMemberId));
    }

    public RollBtiRecommendationCursorResponse getRecommendationsByTypeWithCursor(
            RollBtiType type,
            Integer size,
            Long cursorMemberId,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier,
            Long excludeMemberId) {
        return getRecommendationsWithCursor(type, size, cursorMemberId, compatibilityOrder, tier,
                resolveRequesterMember(excludeMemberId));
    }

    public RollBtiRecommendationCursorResponse getRecommendationsByTypeAndBucketWithCursor(
            RollBtiType type,
            RollBtiRecommendationBucket bucket,
            Integer size,
            Long cursorMemberId,
            Tier tier,
            Long excludeMemberId) {
        return getRecommendationsByBucketWithCursor(type, bucket, size, cursorMemberId, tier,
                resolveRequesterMember(excludeMemberId));
    }

    public RollBtiPublicRecommendationResponse getPublicRecommendations(
            Member requesterMember,
            Integer size,
            Integer page,
            Tier tier) {
        int normalizedSize = normalizeSize(size);
        int normalizedPage = normalizePage(page);
        List<RollBtiMemberCardResponse> sortedRecommendations = getSortedPublicRecommendations(tier, requesterMember);

        int startIndex = (normalizedPage - 1) * normalizedSize;
        if (startIndex >= sortedRecommendations.size()) {
            return RollBtiPublicRecommendationResponse.of(
                    normalizedPage,
                    normalizedSize,
                    List.of(),
                    false
            );
        }

        int endIndex = Math.min(startIndex + normalizedSize, sortedRecommendations.size());
        boolean hasNext = endIndex < sortedRecommendations.size();
        List<RollBtiMemberCardResponse> recommendations = sortedRecommendations.subList(startIndex, endIndex);

        return RollBtiPublicRecommendationResponse.of(
                normalizedPage,
                normalizedSize,
                recommendations,
                hasNext
        );
    }

    public RollBtiPublicRecommendationCursorResponse getPublicRecommendationsWithCursor(
            Member requesterMember,
            Integer size,
            Long cursorMemberId,
            Tier tier) {
        int normalizedSize = normalizeSize(size);
        List<RollBtiMemberCardResponse> sortedRecommendations = getSortedPublicRecommendations(tier, requesterMember);

        int startIndex = 0;
        if (cursorMemberId != null) {
            int cursorIndex = findPublicCursorIndex(sortedRecommendations, cursorMemberId);
            if (cursorIndex < 0) {
                return RollBtiPublicRecommendationCursorResponse.of(
                        normalizedSize,
                        List.of(),
                        false,
                        null
                );
            }
            startIndex = cursorIndex + 1;
        }

        if (startIndex >= sortedRecommendations.size()) {
            return RollBtiPublicRecommendationCursorResponse.of(
                    normalizedSize,
                    List.of(),
                    false,
                    null
            );
        }

        int endIndex = Math.min(startIndex + normalizedSize, sortedRecommendations.size());
        boolean hasNext = endIndex < sortedRecommendations.size();
        List<RollBtiMemberCardResponse> recommendations = sortedRecommendations.subList(startIndex, endIndex);
        Long nextCursorMemberId = hasNext ? recommendations.get(recommendations.size() - 1).getMemberId() : null;

        return RollBtiPublicRecommendationCursorResponse.of(
                normalizedSize,
                recommendations,
                hasNext,
                nextCursorMemberId
        );
    }

    public RollBtiParticipationCountResponse getParticipationCount() {
        long totalParticipants = rollBtiEventRepository.countDistinctParticipantsByCompleteTest()
                + participantCountAdjustment;
        return RollBtiParticipationCountResponse.of(totalParticipants);
    }

    public RollBtiGuestResultSaveResponse saveGuestResult(RollBtiGuestResultSaveRequest request) {
        String payload = serializePayload(request.getResultPayload());

        for (int i = 0; i < RESULT_ID_RETRY_LIMIT; i++) {
            String resultId = generateRandomResultId();
            var saved = rollBtiGuestResultSaver.trySave(
                    resultId, request.getType(), payload, request.getSessionId());
            if (saved.isPresent()) {
                return RollBtiGuestResultSaveResponse.of(saved.get());
            }
            log.warn("롤BTI 결과 ID 충돌 발생, 재시도합니다. resultId={}, attempt={}", resultId, i + 1);
        }
        throw new RollBtiException(ErrorCode._INTERNAL_SERVER_ERROR, RESULT_ID_GENERATION_FAILED_MESSAGE);
    }

    public RollBtiGuestResultResponse getGuestResultByResultId(String resultId) {
        RollBtiGuestResult result = rollBtiGuestResultRepository.findByResultId(resultId)
                .orElseThrow(() -> new RollBtiException(ErrorCode.ROLL_BTI_RESULT_NOT_FOUND));
        return RollBtiGuestResultResponse.of(result, deserializePayload(result.getResultPayload()));
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

    private RollBtiRecommendationResponse getRecommendations(
            RollBtiType requesterType,
            Integer size,
            Integer page,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier,
            Member requesterMember) {
        int normalizedSize = normalizeSize(size);
        int normalizedPage = normalizePage(page);

        List<RollBtiRecommendedMemberResponse> sortedRecommendations =
                getSortedRecommendations(requesterType, compatibilityOrder, tier, requesterMember);

        int startIndex = (normalizedPage - 1) * normalizedSize;
        if (startIndex >= sortedRecommendations.size()) {
            return RollBtiRecommendationResponse.of(
                    requesterType,
                    normalizedPage,
                    normalizedSize,
                    List.of(),
                    false
            );
        }

        int endIndex = Math.min(startIndex + normalizedSize, sortedRecommendations.size());
        boolean hasNext = endIndex < sortedRecommendations.size();
        List<RollBtiRecommendedMemberResponse> recommendations = sortedRecommendations.subList(startIndex, endIndex);

        return RollBtiRecommendationResponse.of(
                requesterType,
                normalizedPage,
                normalizedSize,
                recommendations,
                hasNext
        );
    }

    private RollBtiRecommendationCursorResponse getRecommendationsWithCursor(
            RollBtiType requesterType,
            Integer size,
            Long cursorMemberId,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier,
            Member requesterMember) {
        int normalizedSize = normalizeSize(size);
        List<RollBtiRecommendedMemberResponse> sortedRecommendations =
                getSortedRecommendations(requesterType, compatibilityOrder, tier, requesterMember);

        int startIndex = 0;
        if (cursorMemberId != null) {
            int cursorIndex = findCursorIndex(sortedRecommendations, cursorMemberId);
            if (cursorIndex < 0) {
                return RollBtiRecommendationCursorResponse.of(
                        requesterType,
                        normalizedSize,
                        List.of(),
                        false,
                        null
                );
            }
            startIndex = cursorIndex + 1;
        }

        if (startIndex >= sortedRecommendations.size()) {
            return RollBtiRecommendationCursorResponse.of(
                    requesterType,
                    normalizedSize,
                    List.of(),
                    false,
                    null
            );
        }

        int endIndex = Math.min(startIndex + normalizedSize, sortedRecommendations.size());
        boolean hasNext = endIndex < sortedRecommendations.size();
        List<RollBtiRecommendedMemberResponse> recommendations = sortedRecommendations.subList(startIndex, endIndex);
        Long nextCursorMemberId = hasNext ? recommendations.get(recommendations.size() - 1).getMemberId() : null;

        return RollBtiRecommendationCursorResponse.of(
                requesterType,
                normalizedSize,
                recommendations,
                hasNext,
                nextCursorMemberId
        );
    }

    private RollBtiRecommendationCursorResponse getRecommendationsByBucketWithCursor(
            RollBtiType requesterType,
            RollBtiRecommendationBucket bucket,
            Integer size,
            Long cursorMemberId,
            Tier tier,
            Member requesterMember) {
        int normalizedSize = normalizeSize(size);
        List<RollBtiRecommendedMemberResponse> recommendationsByBucket =
                getSortedRecommendations(requesterType, RollBtiCompatibilityOrder.HIGH, tier, requesterMember).stream()
                        .filter(recommendation -> matchesBucket(recommendation.getCompatibilityScore(), bucket))
                        .toList();

        int startIndex = 0;
        if (cursorMemberId != null) {
            int cursorIndex = findCursorIndex(recommendationsByBucket, cursorMemberId);
            if (cursorIndex < 0) {
                return RollBtiRecommendationCursorResponse.of(
                        requesterType,
                        normalizedSize,
                        List.of(),
                        false,
                        null
                );
            }
            startIndex = cursorIndex + 1;
        }

        if (startIndex >= recommendationsByBucket.size()) {
            return RollBtiRecommendationCursorResponse.of(
                    requesterType,
                    normalizedSize,
                    List.of(),
                    false,
                    null
            );
        }

        int endIndex = Math.min(startIndex + normalizedSize, recommendationsByBucket.size());
        boolean hasNext = endIndex < recommendationsByBucket.size();
        List<RollBtiRecommendedMemberResponse> recommendations =
                recommendationsByBucket.subList(startIndex, endIndex);
        Long nextCursorMemberId = hasNext ? recommendations.get(recommendations.size() - 1).getMemberId() : null;

        return RollBtiRecommendationCursorResponse.of(
                requesterType,
                normalizedSize,
                recommendations,
                hasNext,
                nextCursorMemberId
        );
    }

    private List<RollBtiRecommendedMemberResponse> getSortedRecommendations(
            RollBtiType requesterType,
            RollBtiCompatibilityOrder compatibilityOrder,
            Tier tier,
            Member requesterMember) {
        Long excludeMemberId = requesterMember != null ? requesterMember.getId() : null;
        List<MemberRollBtiProfile> candidateProfiles =
                memberRollBtiProfileRepository.findRecommendationCandidates(tier, excludeMemberId);
        Set<RollBtiType> goodMatches = rollBtiCatalogService.getGoodMatches(requesterType);
        Set<RollBtiType> badMatches = rollBtiCatalogService.getBadMatches(requesterType);
        RecommendationRelationContext relationContext = buildRelationContext(requesterMember, candidateProfiles);

        return candidateProfiles.stream()
                .map(profile -> {
                    Member targetMember = profile.getMember();
                    RollBtiType targetType = profile.getRollBtiType();
                    int compatibilityScore = calculateCompatibilityScore(
                            requesterType,
                            targetType,
                            goodMatches,
                            badMatches
                    );
                    RecommendationRelation relation = relationContext.get(targetMember.getId());
                    return RollBtiRecommendedMemberResponse.of(
                            targetMember.getId(),
                            targetMember.getGameName(),
                            targetMember.getTag(),
                            targetMember.getProfileImage(),
                            targetMember.getMannerLevel(),
                            targetMember.getMainP(),
                            targetMember.getSubP(),
                            targetMember.getMike(),
                            targetType,
                            compatibilityScore,
                            relation.blocked(),
                            relation.friendRequestReceived(),
                            relation.friendRequestSent(),
                            relation.friend(),
                            relation.nonFriend(),
                            getRecommendationUpdatedAt(profile),
                            getRecommendedChampionStats(targetMember));
                })
                .sorted(getRecommendationComparator(compatibilityOrder))
                .collect(Collectors.toList());
    }

    private List<RollBtiMemberCardResponse> getSortedPublicRecommendations(Tier tier, Member requesterMember) {
        List<MemberRollBtiProfile> candidateProfiles =
                memberRollBtiProfileRepository.findRecommendationCandidates(tier, null);
        RecommendationRelationContext relationContext = buildRelationContext(requesterMember, candidateProfiles);

        return candidateProfiles.stream()
                .map(profile -> toMemberCardResponse(profile, relationContext))
                .sorted(getPublicRecommendationComparator())
                .toList();
    }

    private List<RollBtiMemberCardResponse> getSortedPublicRecommendations(Tier tier) {
        return memberRollBtiProfileRepository.findRecommendationCandidates(tier, null).stream()
                .map(this::toMemberCardResponse)
                .sorted(getPublicRecommendationComparator())
                .toList();
    }

    private int findCursorIndex(List<RollBtiRecommendedMemberResponse> recommendations, Long cursorMemberId) {
        for (int i = 0; i < recommendations.size(); i++) {
            if (Objects.equals(recommendations.get(i).getMemberId(), cursorMemberId)) {
                return i;
            }
        }
        return -1;
    }

    private int findPublicCursorIndex(List<RollBtiMemberCardResponse> recommendations, Long cursorMemberId) {
        for (int i = 0; i < recommendations.size(); i++) {
            if (Objects.equals(recommendations.get(i).getMemberId(), cursorMemberId)) {
                return i;
            }
        }
        return -1;
    }

    private Comparator<RollBtiRecommendedMemberResponse> getRecommendationComparator(
            RollBtiCompatibilityOrder compatibilityOrder) {
        Comparator<RollBtiRecommendedMemberResponse> scoreComparator =
                Comparator.comparingInt(RollBtiRecommendedMemberResponse::getCompatibilityScore);

        if (compatibilityOrder != RollBtiCompatibilityOrder.LOW) {
            scoreComparator = scoreComparator.reversed();
        }

        return scoreComparator
                .thenComparing(RollBtiRecommendedMemberResponse::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RollBtiRecommendedMemberResponse::getMemberId, Comparator.reverseOrder());
    }

    private boolean matchesBucket(int compatibilityScore, RollBtiRecommendationBucket bucket) {
        return switch (bucket) {
            case GOOD -> compatibilityScore >= 90;
            case NORMAL -> compatibilityScore >= 50 && compatibilityScore < 90;
            case BAD -> compatibilityScore < 50;
        };
    }

    private Comparator<RollBtiMemberCardResponse> getPublicRecommendationComparator() {
        return Comparator.comparing(
                        RollBtiMemberCardResponse::getGameName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(
                        RollBtiMemberCardResponse::getTag,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(RollBtiMemberCardResponse::getMemberId);
    }

    private List<ChampionStatsResponse> getRecommendedChampionStats(Member member) {
        if (member.getMemberChampionList() == null) {
            return List.of();
        }

        return member.getMemberChampionList().stream()
                .filter(memberChampion -> memberChampion.getGames() > 0)
                .sorted(Comparator.comparingInt(MemberChampion::getGames).reversed())
                .limit(4)
                .map(ChampionStatsResponse::from)
                .toList();
    }

    private java.time.LocalDateTime getRecommendationUpdatedAt(MemberRollBtiProfile profile) {
        return profile.getUpdatedAt() != null ? profile.getUpdatedAt() : profile.getCreatedAt();
    }

    private RollBtiMemberCardResponse toMemberCardResponse(MemberRollBtiProfile profile) {
        return toMemberCardResponse(profile, RecommendationRelationContext.empty());
    }

    private RollBtiMemberCardResponse toMemberCardResponse(
            MemberRollBtiProfile profile,
            RecommendationRelationContext relationContext) {
        Member targetMember = profile.getMember();
        RecommendationRelation relation = relationContext.get(targetMember.getId());
        return RollBtiMemberCardResponse.of(
                targetMember.getId(),
                targetMember.getGameName(),
                targetMember.getTag(),
                targetMember.getProfileImage(),
                targetMember.getMannerLevel(),
                targetMember.getMainP(),
                targetMember.getSubP(),
                targetMember.getMike(),
                profile.getRollBtiType(),
                relation.blocked(),
                relation.friendRequestReceived(),
                relation.friendRequestSent(),
                relation.friend(),
                relation.nonFriend(),
                getRecommendationUpdatedAt(profile),
                getRecommendedChampionStats(targetMember)
        );
    }

    private Member resolveRequesterMember(Long requesterMemberId) {
        if (requesterMemberId == null) {
            return null;
        }
        return memberRepository.findById(requesterMemberId).orElse(null);
    }

    private RecommendationRelationContext buildRelationContext(
            Member requesterMember,
            List<MemberRollBtiProfile> candidateProfiles) {
        if (requesterMember == null || candidateProfiles.isEmpty()) {
            return RecommendationRelationContext.empty();
        }

        List<Long> targetMemberIds = candidateProfiles.stream()
                .map(profile -> profile.getMember().getId())
                .distinct()
                .toList();

        return new RecommendationRelationContext(
                blockService.hasBlockedTargetMembersBatch(requesterMember, targetMemberIds),
                friendService.isFriendBatch(requesterMember, targetMemberIds),
                friendService.getFriendRequestMemberIdBatch(requesterMember, targetMemberIds),
                requesterMember.getId()
        );
    }

    private record RecommendationRelationContext(
            java.util.Map<Long, Boolean> blockedMap,
            java.util.Map<Long, Boolean> friendMap,
            java.util.Map<Long, Long> friendRequestMemberIdMap,
            Long requesterMemberId) {

        private static RecommendationRelationContext empty() {
            return new RecommendationRelationContext(java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), null);
        }

        private RecommendationRelation get(Long targetMemberId) {
            if (requesterMemberId == null) {
                return new RecommendationRelation(null, null, null, null, null);
            }
            boolean blocked = blockedMap.getOrDefault(targetMemberId, false);
            boolean friend = friendMap.getOrDefault(targetMemberId, false);
            Long requestMemberId = friendRequestMemberIdMap.get(targetMemberId);
            boolean friendRequestSent = requesterMemberId != null && requesterMemberId.equals(requestMemberId);
            boolean friendRequestReceived = requestMemberId != null && requestMemberId.equals(targetMemberId);
            boolean nonFriend = !blocked && !friend && !friendRequestSent && !friendRequestReceived;
            return new RecommendationRelation(blocked, friendRequestReceived, friendRequestSent, friend, nonFriend);
        }
    }

    private record RecommendationRelation(
            Boolean blocked,
            Boolean friendRequestReceived,
            Boolean friendRequestSent,
            Boolean friend,
            Boolean nonFriend) {
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

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new RollBtiException(ErrorCode._BAD_REQUEST);
        }
        return page;
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

    private String generateRandomResultId() {
        StringBuilder sb = new StringBuilder(RESULT_ID_LENGTH);
        for (int i = 0; i < RESULT_ID_LENGTH; i++) {
            sb.append(RESULT_ID_CHARS[secureRandom.nextInt(RESULT_ID_CHARS.length)]);
        }
        return sb.toString();
    }

    private String serializePayload(JsonNode resultPayload) {
        try {
            return objectMapper.writeValueAsString(resultPayload);
        } catch (JsonProcessingException e) {
            log.error("롤BTI 결과 payload 직렬화 실패", e);
            throw new RollBtiException(ErrorCode._INTERNAL_SERVER_ERROR, RESULT_PAYLOAD_SERIALIZATION_FAILED_MESSAGE);
        }
    }

    private JsonNode deserializePayload(String resultPayload) {
        try {
            return objectMapper.readTree(resultPayload);
        } catch (JsonProcessingException e) {
            log.error("롤BTI 결과 payload 역직렬화 실패", e);
            throw new RollBtiException(ErrorCode._INTERNAL_SERVER_ERROR, RESULT_PAYLOAD_DESERIALIZATION_FAILED_MESSAGE);
        }
    }
}
