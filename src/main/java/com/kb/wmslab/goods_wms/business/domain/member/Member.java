package com.kb.wmslab.goods_wms.business.domain.member;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InactiveEntityException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Member {

    private final Long id;
    private String name;
    private String email;
    private MemberRole role;
    private MemberStatus status;
    private final LocalDateTime createdAt;

    private Member(Long id, String name, String email, MemberRole role, MemberStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Member create(String name, String email, MemberRole role) {
        return new Member(null, name, email, role, MemberStatus.ACTIVE, LocalDateTime.now());
    }

    public static Member reconstitute(Long id, String name, String email, MemberRole role, MemberStatus status, LocalDateTime createdAt) {
        return new Member(id, name, email, role, status, createdAt);
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = MemberStatus.INACTIVE;
    }

    public void changeRole(MemberRole newRole) {
        this.role = newRole;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public void validateCanHandleInbound() {
        if (!isActive()) throw new InactiveEntityException("Member", id);
        if (role != MemberRole.INBOUND_HANDLER && role != MemberRole.ADMIN) {
            throw new IllegalStateException("입고 처리 권한이 없습니다: " + role);
        }
    }

    public void validateCanHandleOutbound() {
        if (!isActive()) throw new InactiveEntityException("Member", id);
        if (role != MemberRole.OUTBOUND_HANDLER && role != MemberRole.ADMIN) {
            throw new IllegalStateException("출고 처리 권한이 없습니다: " + role);
        }
    }
}
