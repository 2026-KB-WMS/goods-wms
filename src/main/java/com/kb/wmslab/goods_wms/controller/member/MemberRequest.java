package com.kb.wmslab.goods_wms.controller.member;

import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MemberRequest {
    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotNull MemberRole role
    ) {}

    public record ChangeRoleRequest(@NotNull MemberRole newRole) {}
}
