package com.kb.wmslab.goods_wms.repository.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    boolean existsByEmail(String email);
}
