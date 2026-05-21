package com.kb.wmslab.goods_wms.business.application.member;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EntityNotFoundException;
import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public MemberResult registerMember(MemberCommand.Register command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다: " + command.email());
        }
        Member member = Member.create(command.name(), command.email(), command.role());
        return MemberResult.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberResult activateMember(Long id) {
        Member member = findById(id);
        member.activate();
        return MemberResult.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberResult deactivateMember(Long id) {
        Member member = findById(id);
        member.deactivate();
        return MemberResult.from(memberRepository.save(member));
    }

    @Override
    @Transactional
    public MemberResult changeRole(Long id, MemberCommand.ChangeRole command) {
        Member member = findById(id);
        member.changeRole(command.newRole());
        return MemberResult.from(memberRepository.save(member));
    }

    @Override
    public MemberResult getMember(Long id) {
        return MemberResult.from(findById(id));
    }

    private Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member", id));
    }
}
