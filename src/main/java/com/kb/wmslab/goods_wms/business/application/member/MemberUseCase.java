package com.kb.wmslab.goods_wms.business.application.member;

public interface MemberUseCase {
    MemberResult registerMember(MemberCommand.Register command);
    MemberResult activateMember(Long id);
    MemberResult deactivateMember(Long id);
    MemberResult changeRole(Long id, MemberCommand.ChangeRole command);
    MemberResult getMember(Long id);
}
