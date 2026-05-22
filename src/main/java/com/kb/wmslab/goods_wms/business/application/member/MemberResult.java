package com.kb.wmslab.goods_wms.business.application.member;

import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;
import com.kb.wmslab.goods_wms.business.domain.member.MemberStatus;

import java.time.LocalDateTime;

public record MemberResult(
        Long id, String name, String email,
        MemberRole role, MemberStatus status, LocalDateTime createdAt
) {
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(), member.getName(), member.getEmail(),
                member.getRole(), member.getStatus(), member.getCreatedAt()
        );
    }
}
