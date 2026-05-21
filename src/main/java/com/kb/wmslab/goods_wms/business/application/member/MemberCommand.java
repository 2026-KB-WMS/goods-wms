package com.kb.wmslab.goods_wms.business.application.member;

import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;

public class MemberCommand {
    public record Register(String name, String email, MemberRole role) {}
    public record ChangeRole(MemberRole newRole) {}
}
