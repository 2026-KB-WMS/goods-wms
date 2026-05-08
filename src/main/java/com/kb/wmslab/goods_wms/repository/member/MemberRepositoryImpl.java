package com.kb.wmslab.goods_wms.repository.member;

import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository jpaRepository;

    @Override
    public Member save(Member member) {
        return jpaRepository.save(MemberJpaEntity.from(member)).toDomain();
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id).map(MemberJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
