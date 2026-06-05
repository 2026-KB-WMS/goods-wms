package com.kb.wmslab.goods_wms.repository.member;

import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;
import com.kb.wmslab.goods_wms.business.domain.member.MemberStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String name;
    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime createdAt;

    private MemberJpaEntity(String name, String email, MemberRole role, MemberStatus status, LocalDateTime createdAt) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static MemberJpaEntity from(Member member) {
        MemberJpaEntity entity = new MemberJpaEntity(
                member.getName(), member.getEmail(),
                member.getRole(), member.getStatus(), member.getCreatedAt()
        );
        entity.id = member.getId();
        entity.version = member.getVersion();
        return entity;
    }

    public Member toDomain() {
        return Member.reconstitute(id, name, email, role, status, createdAt, version);
    }
}
