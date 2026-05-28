package com.kb.wmslab.goods_wms.controller.member;

import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;

public class MemberRequest {
    public record RegisterRequest(String name, String email, MemberRole role) {}
    public record ChangeRoleRequest(MemberRole newRole) {}
}
