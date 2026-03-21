package com.gamegoo.gamegoo_v2.account.auth.service;

import com.gamegoo.gamegoo_v2.account.auth.domain.Role;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.AdminLoginRequest;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.RefreshTokenRequest;
import com.gamegoo.gamegoo_v2.account.auth.dto.response.TokensResponse;
import com.gamegoo.gamegoo_v2.account.auth.dto.response.RejoinResponse;
import com.gamegoo.gamegoo_v2.account.auth.jwt.JwtProvider;
import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.account.auth.dto.request.RejoinRequest;
import com.gamegoo.gamegoo_v2.account.member.service.BanService;
import com.gamegoo.gamegoo_v2.account.member.service.MemberService;
import com.gamegoo.gamegoo_v2.chat.service.ChatCommandService;
import com.gamegoo.gamegoo_v2.content.board.service.BoardService;
import com.gamegoo.gamegoo_v2.core.exception.AuthException;
import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.external.riot.dto.response.RiotJoinResponse;
import com.gamegoo.gamegoo_v2.social.friend.service.FriendService;
import com.gamegoo.gamegoo_v2.social.manner.service.MannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthFacadeService {

    private final MemberService memberService;
    private final ChatCommandService chatCommandService;
    private final FriendService friendService;
    private final MannerService mannerService;
    private final BoardService boardService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final BanService banService;

    @Value("${admin.common.password}")
    private String adminCommonPassword;

    /**
     * 로그아웃
     *
     * @param member 사용자
     * @return 메세지
     */
    public String logout(Member member) {
        authService.deleteRefreshToken(member);
        return "로그아웃이 완료되었습니다.";
    }

    /**
     * 리프레시 토큰으로 토큰 업데이트
     *
     * @param request 리프레시 토큰
     * @return 사용자 정보
     */
    public TokensResponse updateToken(RefreshTokenRequest request) {
        // refresh 토큰 검증
        authService.verifyRefreshToken(request.getRefreshToken());

        // memberId 조회
        Long memberId = jwtProvider.getMemberId(request.getRefreshToken());
        Role role = jwtProvider.getRole(request.getRefreshToken());

        // jwt 토큰 재발급
        String accessToken = jwtProvider.createAccessToken(memberId, role);
        String refreshToken = jwtProvider.createRefreshToken(memberId, role);

        // memberId로 member 엔티티 조회
        Member member = memberService.findMemberById(memberId);

        // refreshToken 저장
        authService.updateRefreshToken(member, refreshToken);

        return TokensResponse.of(memberId, accessToken, refreshToken);
    }

    /**
     * 회원 탈퇴 처리
     *
     * @param member 탈퇴할 회원
     * @return
     */
    public String blindMember(Member member) {
        // 해당 회원이 속한 모든 채팅방에서 퇴장 처리
        chatCommandService.exitAllChatroom(member);

        // 해당 회원이 보낸 모든 친구 요청 취소 처리
        friendService.cancelAllFriendRequestsByFromMember(member);

        // 해당 회원이 받은 모든 친구 요청 취소 처리
        friendService.cancelAllFriendRequestsByToMember(member);

        // 게시판 글 삭제 처리
        boardService.deleteAllBoardByMember(member);

        // 매너, 비매너 평가 기록 삭제 처리
        mannerService.deleteAllMannerRatingsByMember(member);

        // refresh Token 삭제하기
        authService.deleteRefreshToken(member);

        // Member 테이블에서 blind 처리
        memberService.deactivateMember(member);
        return "탈퇴처리가 완료되었습니다";
    }

    /**
     * 테스트용 access, refresh token 발급
     *
     * @param memberId
     * @return TokensResponse
     */
    public com.gamegoo.gamegoo_v2.test_support.dto.TokensResponse createTestAccessTokenAndRefreshTokens(Long memberId) {
        // jwt 토큰 재발급
        String accessToken = jwtProvider.createAccessToken(memberId, Role.MEMBER);
        String refreshToken = jwtProvider.createRefreshToken(memberId, Role.MEMBER);

        // memberId로 member 엔티티 조회
        Member member = memberService.findMemberById(memberId);

        // refreshToken 저장
        authService.updateRefreshToken(member, refreshToken);
        return com.gamegoo.gamegoo_v2.test_support.dto.TokensResponse.of(accessToken, refreshToken);
    }

    /**
     * 탈퇴한 회원 재가입
     *
     * @param request 재가입 요청
     * @return
     */
    @Transactional
    public RejoinResponse rejoinMember(RejoinRequest request) {
        // 실제 있는 사용자인지 검증
        List<Member> memberByPuuid = memberService.findMemberByPuuid(request.getPuuid());
        if (memberByPuuid.isEmpty()) {
            throw new AuthException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (memberByPuuid.size() > 1) {
            throw new AuthException(ErrorCode.DULPLICATED_MEMBER);
        }

        Member member = memberByPuuid.get(0);
        // 탈퇴한 사용자인지 검증
        if (!member.getBlind()) {
            throw new AuthException(ErrorCode.ACTIVE_MEMBER);
        }

        // 제재 있는지 검증
        // 만료된 제재 자동 해제
        banService.checkBanExpiry(member);

        // 제재 메시지 생성
        String banMessage = null;
        if (member.isBanned()) {
            banMessage = banService.getBanReasonMessage(member.getBanType());
        }

        // 탈퇴 해제
        memberService.activateMember(member);

        // 로그인 진행
        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole());

        // refresh token DB에 저장
        authService.updateRefreshToken(member, refreshToken);

        return RejoinResponse.of(member, accessToken, refreshToken, banMessage);
    }

    /**
     * 관리자 로그인
     *
     * @param request 관리자 로그인 요청 (account, password)
     * @return RiotJoinResponse
     */
    public RiotJoinResponse adminLogin(AdminLoginRequest request) {
        // account를 #으로 파싱하여 gameName과 tag 추출
        String[] accountParts = request.getAccount().split("#");
        if (accountParts.length != 2) {
            throw new AuthException(ErrorCode.INVALID_ADMIN_ACCOUNT_FORMAT);
        }

        String gameName = accountParts[0];
        String tag = accountParts[1];

        // gameName과 tag로 Member 조회
        Member member = memberService.findMemberByGameNameAndTag(gameName, tag);

        // role이 ADMIN인지와 비밀번호가 일치하는지 함께 검증하여 사용자 열거 및 타이밍 공격 방지
        boolean passwordMatches = java.security.MessageDigest.isEqual(
                adminCommonPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        if (member.getRole() != Role.ADMIN || !passwordMatches) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD);
        }

        // JWT 토큰 발급
        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole());

        // refresh token DB에 저장
        authService.updateRefreshToken(member, refreshToken);

        return RiotJoinResponse.of(member, accessToken, refreshToken);
    }

}
