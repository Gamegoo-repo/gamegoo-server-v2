package com.gamegoo.gamegoo_v2.rollbti.domain;

import com.gamegoo.gamegoo_v2.account.member.domain.Member;
import com.gamegoo.gamegoo_v2.core.common.BaseDateTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roll_bti_event")
public class RollBtiEvent extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roll_bti_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RollBtiEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RollBtiType rollBtiType;

    @Column(length = 120)
    private String sessionId;

    @Column(length = 80)
    private String eventSource;

    @Builder
    private RollBtiEvent(Member member, RollBtiEventType eventType, RollBtiType rollBtiType, String sessionId,
                         String eventSource) {
        this.member = member;
        this.eventType = eventType;
        this.rollBtiType = rollBtiType;
        this.sessionId = sessionId;
        this.eventSource = eventSource;
    }

    public static RollBtiEvent create(Member member, RollBtiEventType eventType, RollBtiType rollBtiType,
                                      String sessionId, String eventSource) {
        return RollBtiEvent.builder()
                .member(member)
                .eventType(eventType)
                .rollBtiType(rollBtiType)
                .sessionId(sessionId)
                .eventSource(eventSource)
                .build();
    }
}

