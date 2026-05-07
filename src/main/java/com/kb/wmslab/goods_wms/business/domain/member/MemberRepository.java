package com.kb.wmslab.goods_wms.business.domain.member;

import java.util.Optional;

public interface MemberRepository {
    Member save(Member member);
    Optional<Member> findById(Long id);
    boolean existsByEmail(String email);
}
