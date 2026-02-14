package com.gamegoo.gamegoo_v2.rollbti.domain;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.core.common.BaseDateTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_roll_bti_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_roll_bti_profile_member_id", columnNames = "member_id")
        }
)
public class MemberRollBtiProfile extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_roll_bti_profile_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "roll_bti_type", nullable = false, length = 20)
    private RollBtiType rollBtiType;

    @Builder
    private MemberRollBtiProfile(Member member, RollBtiType rollBtiType) {
        this.member = member;
        this.rollBtiType = rollBtiType;
    }

    public static MemberRollBtiProfile create(Member member, RollBtiType rollBtiType) {
        return MemberRollBtiProfile.builder()
                .member(member)
                .rollBtiType(rollBtiType)
                .build();
    }

    public void updateType(RollBtiType rollBtiType) {
        this.rollBtiType = rollBtiType;
    }
}

