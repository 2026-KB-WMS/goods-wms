package com.kb.wmslab.goods_wms.controller.member;

import com.kb.wmslab.goods_wms.business.application.member.MemberCommand;
import com.kb.wmslab.goods_wms.business.application.member.MemberResult;
import com.kb.wmslab.goods_wms.business.application.member.MemberUseCase;
import com.kb.wmslab.goods_wms.controller.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberUseCase memberUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResult>> register(@RequestBody MemberRequest.RegisterRequest request) {
        MemberResult result = memberUseCase.registerMember(
                new MemberCommand.Register(request.name(), request.email(), request.role())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result, "멤버가 등록되었습니다."));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MemberResult>> activate(@PathVariable Long id) {
        MemberResult result = memberUseCase.activateMember(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "멤버가 활성화되었습니다."));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<MemberResult>> deactivate(@PathVariable Long id) {
        MemberResult result = memberUseCase.deactivateMember(id);
        return ResponseEntity.ok(ApiResponse.ok(result, "멤버가 비활성화되었습니다."));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<MemberResult>> changeRole(
            @PathVariable Long id,
            @RequestBody MemberRequest.ChangeRoleRequest request) {
        MemberResult result = memberUseCase.changeRole(id,
                new MemberCommand.ChangeRole(request.newRole())
        );
        return ResponseEntity.ok(ApiResponse.ok(result, "멤버 역할이 변경되었습니다."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberResult>> getMember(@PathVariable Long id) {
        MemberResult result = memberUseCase.getMember(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
