package com.gamegoo.gamegoo_v2.integration.friend;

import com.gamegoo.gamegoo_v2.account.member.domain.LoginType;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.member.domain.MemberRecentStats;
import com.gamegoo.gamegoo_v2.account.member.domain.Tier;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRecentStatsRepository;
import com.gamegoo.gamegoo_v2.account.member.repository.MemberRepository;
import com.gamegoo.gamegoo_v2.core.exception.FriendException;
import com.gamegoo.gamegoo_v2.core.exception.MemberException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.external.socket.SocketService;
import com.gamegoo.gamegoo_v2.notification.repository.NotificationRepository;
import com.gamegoo.gamegoo_v2.notification.service.NotificationService;
import com.gamegoo.gamegoo_v2.social.block.domain.Block;
import com.gamegoo.gamegoo_v2.social.block.repository.BlockRepository;
import com.gamegoo.gamegoo_v2.social.friend.domain.Friend;
import com.gamegoo.gamegoo_v2.social.friend.domain.FriendRequest;
import com.gamegoo.gamegoo_v2.social.friend.domain.FriendRequestStatus;
import com.gamegoo.gamegoo_v2.social.friend.dto.FriendRequestResponse;
import com.gamegoo.gamegoo_v2.social.friend.repository.FriendRepository;
import com.gamegoo.gamegoo_v2.social.friend.repository.FriendRequestRepository;
import com.gamegoo.gamegoo_v2.social.friend.service.FriendFacadeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ActiveProfiles("test")
@SpringBootTest
class FriendRequestFacadeServiceTest {

    @Autowired
    private FriendFacadeService friendFacadeService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    BlockRepository blockRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private MemberRecentStatsRepository memberRecentStatsRepository;

    @MockitoSpyBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private SocketService socketService;

    @MockitoSpyBean
    private NotificationService notificationService;

    private static final String TARGET_EMAIL = "target@naver.com";
    private static final String TARGET_GAMENAME = "target";

    private Member member;

    @BeforeEach
    void setUp() {
        member = createMember("test@gmail.com", "member");
    }

    @AfterEach
    void tearDown() {
        memberRecentStatsRepository.deleteAll();
        friendRepository.deleteAllInBatch();
        friendRequestRepository.deleteAllInBatch();
        blockRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("친구 요청 전송")
    class SendFriendRequestTest {

        @DisplayName("친구 요청 전송 성공")
        @Test
        void sendFriendRequestSucceeds() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // when
            FriendRequestResponse response = friendFacadeService.sendFriendRequest(member, targetMember.getId());

            // then
            assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());
            assertThat(response.getMessage()).isEqualTo("친구 요청 전송 성공");

            // friendRequest 엔티티가 저장되었는지 검증
            boolean exists = friendRequestRepository.existsByFromMemberAndToMemberAndStatus(member, targetMember,
                    FriendRequestStatus.PENDING);
            assertThat(exists).isTrue();

        }

        @DisplayName("친구 요청 전송 실패: 본인 id를 요청한 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenTargetIsSelf() {
            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, member.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.FRIEND_BAD_REQUEST.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 상대가 탈퇴한 회원인 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenTargetIsBlind() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 대상 회원을 탈퇴 처리
            targetMember.updateBlind(true);
            memberRepository.save(targetMember);

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(MemberException.class)
                    .hasMessage(ErrorCode.TARGET_MEMBER_DEACTIVATED.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 내가 상대를 차단한 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenTargetIsBlocked() {
            // given
            member = createMember("test@<EMAIL>", "member");
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 내가 상대를 차단 처리
            blockRepository.save(Block.create(member, targetMember));

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.FRIEND_TARGET_IS_BLOCKED.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 상대가 나를 차단한 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenBlockedByTarget() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 상대가 나를 차단 처리
            blockRepository.save(Block.create(targetMember, member));

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.BLOCKED_BY_FRIEND_TARGET.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 두 회원이 이미 친구 관계인 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenAlreadyFriend() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 두 회원 간 친구 관계 생성
            friendRepository.save(Friend.create(member, targetMember));
            friendRepository.save(Friend.create(targetMember, member));

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.ALREADY_FRIEND.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 내가 보낸 수락 대기 중인 친구 요청이 이미 존재하는 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenPendingRequestToTargetExists() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 나 -> 상대 친구 요청 생성
            friendRequestRepository.save(FriendRequest.create(member, targetMember));

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.MY_PENDING_FRIEND_REQUEST_EXIST.getMessage());
        }

        @DisplayName("친구 요청 전송 실패: 상대가 보낸 수락 대기 중인 친구 요청이 이미 존재하는 경우 예외가 발생한다.")
        @Test
        void sendFriendRequest_shouldThrowWhenPendingRequestToMeExists() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 상대 -> 나 친구 요청 생성
            friendRequestRepository.save(FriendRequest.create(targetMember, member));

            // when // then
            assertThatThrownBy(() -> friendFacadeService.sendFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.TARGET_PENDING_FRIEND_REQUEST_EXIST.getMessage());
        }

    }

    @Nested
    @DisplayName("친구 요청 수락")
    class AcceptFriendRequestTest {

        @DisplayName("친구 요청 수락 성공")
        @Test
        void acceptFriendRequestSucceeds() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 상대 -> 나 친구 요청 생성
            friendRequestRepository.save(FriendRequest.create(targetMember, member));

            // when
            FriendRequestResponse response = friendFacadeService.acceptFriendRequest(member, targetMember.getId());

            // then
            assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());
            assertThat(response.getMessage()).isEqualTo("친구 요청 수락 성공");

            // FriendRequest 엔티티 상태가 정상 변경 되었는지 검증
            boolean exists = friendRequestRepository.existsByFromMemberAndToMemberAndStatus(targetMember, member,
                    FriendRequestStatus.ACCEPTED);
            assertThat(exists).isTrue();

            // Friend 엔티티가 정상 생성 되었는지 검증
            assertThat(friendRepository.existsByFromMemberAndToMember(member, targetMember)).isTrue();
            assertThat(friendRepository.existsByFromMemberAndToMember(targetMember, member)).isTrue();

        }

        @DisplayName("친구 요청 수락 실패: 본인 id를 요청한 경우 예외가 발생한다.")
        @Test
        void acceptFriendRequest_shouldThrowWhenTargetIsSelf() {
            // when // then
            assertThatThrownBy(() -> friendFacadeService.acceptFriendRequest(member, member.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.FRIEND_BAD_REQUEST.getMessage());
        }

        @DisplayName("친구 요청 수락 실패: PENDING 상태인 친구 요청이 없는 경우 예외가 발생한다")
        @Test
        void acceptFriendRequest_shouldThrowWhenNoPendingRequest() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // when // then
            assertThatThrownBy(() -> friendFacadeService.acceptFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.PENDING_FRIEND_REQUEST_NOT_EXIST.getMessage());
        }

    }

    @Nested
    @DisplayName("친구 요청 거절")
    class RejectFriendRequestTest {

        @DisplayName("친구 요청 거절 성공")
        @Test
        void rejectFriendRequestSucceeds() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 상대 -> 나 친구 요청 생성
            friendRequestRepository.save(FriendRequest.create(targetMember, member));

            // when
            FriendRequestResponse response = friendFacadeService.rejectFriendRequest(member, targetMember.getId());

            // then
            assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());
            assertThat(response.getMessage()).isEqualTo("친구 요청 거절 성공");

            // FriendRequest 엔티티 상태가 정상 변경 되었는지 검증
            boolean exists = friendRequestRepository.existsByFromMemberAndToMemberAndStatus(targetMember, member,
                    FriendRequestStatus.REJECTED);
            assertThat(exists).isTrue();
        }

        @DisplayName("친구 요청 거절 실패: 본인 id를 요청한 경우 예외가 발생한다.")
        @Test
        void rejectFriendRequest_shouldThrowWhenTargetIsSelf() {
            // when // then
            assertThatThrownBy(() -> friendFacadeService.rejectFriendRequest(member, member.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.FRIEND_BAD_REQUEST.getMessage());
        }

        @DisplayName("친구 요청 거절 실패: PENDING 상태인 친구 요청이 없는 경우 예외가 발생한다")
        @Test
        void rejectFriendRequest_shouldThrowWhenNoPendingRequest() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // when // then
            assertThatThrownBy(() -> friendFacadeService.rejectFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.PENDING_FRIEND_REQUEST_NOT_EXIST.getMessage());
        }

    }

    @Nested
    @DisplayName("친구 요청 취소")
    class CancelFriendRequestTest {

        @DisplayName("친구 요청 취소 성공")
        @Test
        void cancelFriendRequestSucceeds() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // 나 -> 상대 친구 요청 생성
            friendRequestRepository.save(FriendRequest.create(member, targetMember));

            // when
            FriendRequestResponse response = friendFacadeService.cancelFriendRequest(member, targetMember.getId());

            // then
            assertThat(response.getTargetMemberId()).isEqualTo(targetMember.getId());
            assertThat(response.getMessage()).isEqualTo("친구 요청 취소 성공");

            // FriendRequest 엔티티 상태가 정상 변경 되었는지 검증
            boolean exists = friendRequestRepository.existsByFromMemberAndToMemberAndStatus(member, targetMember,
                    FriendRequestStatus.CANCELLED);
            assertThat(exists).isTrue();
        }

        @DisplayName("친구 요청 취소 실패: 본인 id를 요청한 경우 예외가 발생한다.")
        @Test
        void cancelFriendRequest_shouldThrowWhenTargetIsSelf() {
            // when // then
            assertThatThrownBy(() -> friendFacadeService.cancelFriendRequest(member, member.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.FRIEND_BAD_REQUEST.getMessage());
        }

        @DisplayName("친구 요청 취소 실패: PENDING 상태인 친구 요청이 없는 경우 예외가 발생한다")
        @Test
        void cancelFriendRequest_shouldThrowWhenNoPendingRequest() {
            // given
            Member targetMember = createMember(TARGET_EMAIL, TARGET_GAMENAME);

            // when // then
            assertThatThrownBy(() -> friendFacadeService.cancelFriendRequest(member, targetMember.getId()))
                    .isInstanceOf(FriendException.class)
                    .hasMessage(ErrorCode.PENDING_FRIEND_REQUEST_NOT_EXIST.getMessage());
        }

    }

    private Member createMember(String email, String gameName) {
        Member member = Member.builder()
                .email(email)
                .password("testPassword")
                .profileImage(1)
                .loginType(LoginType.GENERAL)
                .gameName(gameName)
                .tag("TAG")
                .soloTier(Tier.IRON)
                .soloRank(0)
                .soloWinRate(0.0)
                .soloGameCount(0)
                .freeTier(Tier.IRON)
                .freeRank(0)
                .freeWinRate(0.0)
                .freeGameCount(0)
                .isAgree(true)
                .build();

        MemberRecentStats stats = MemberRecentStats.builder().build();
        member.setMemberRecentStats(stats);

        Member savedMember = memberRepository.save(member);
        memberRecentStatsRepository.save(stats);

        return savedMember;
    }

}
