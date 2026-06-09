package com.aiplatform.project.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.project.annotation.PreProjectRole;
import com.aiplatform.project.dto.MemberAddRequest;
import com.aiplatform.project.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/project/member")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    @PostMapping("/{projectId}")
    @PreProjectRole("admin")
    public Result<Long> add(@PathVariable("projectId") Long projectId, @RequestBody @Valid MemberAddRequest req) {
        return Result.ok(memberService.addMember(projectId, req));
    }

    @PutMapping("/{projectId}/role")
    @PreProjectRole("admin")
    public Result<Void> updateRole(@PathVariable("projectId") Long projectId, @RequestBody Map<String, Object> body) {
        Object uid = body == null ? null : body.get("userId");
        Object role = body == null ? null : body.get("roleCode");
        Long userId = uid == null ? null : (uid instanceof Number n ? n.longValue() : Long.parseLong(uid.toString()));
        memberService.updateRole(projectId, userId, role == null ? null : role.toString());
        return Result.ok();
    }

    @DeleteMapping("/{projectId}/{userId}")
    @PreProjectRole("admin")
    public Result<Void> remove(@PathVariable("projectId") Long projectId, @PathVariable("userId") Long userId) {
        memberService.removeMember(projectId, userId);
        return Result.ok();
    }
}
