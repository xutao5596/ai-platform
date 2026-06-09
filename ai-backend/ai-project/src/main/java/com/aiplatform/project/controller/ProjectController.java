package com.aiplatform.project.controller;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.api.Result;
import com.aiplatform.framework.security.RequiresPermissions;
import com.aiplatform.project.annotation.PreProjectRole;
import com.aiplatform.project.dto.ProjectQuery;
import com.aiplatform.project.dto.ProjectSaveRequest;
import com.aiplatform.project.dto.ProjectVO;
import com.aiplatform.project.service.AiProjectService;
import com.aiplatform.project.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private final AiProjectService projectService;
    private final ProjectMemberService memberService;

    @GetMapping("/page")
    public Result<PageResult<ProjectVO>> page(ProjectQuery q) {
        return Result.ok(projectService.pageMy(q));
    }

    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine() {
        return Result.ok(memberService.myProjects());
    }

    @GetMapping("/{id}")
    @PreProjectRole
    public Result<ProjectVO> get(@PathVariable("id") Long id) {
        return Result.ok(projectService.get(id));
    }

    @PostMapping
    @RequiresPermissions("project:create")
    public Result<Long> create(@RequestBody @Valid ProjectSaveRequest req) {
        return Result.ok(projectService.create(req));
    }

    @PutMapping
    @PreProjectRole("admin")
    public Result<Void> update(@RequestBody @Valid ProjectSaveRequest req) {
        projectService.update(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreProjectRole("owner")
    public Result<Void> delete(@PathVariable("id") Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{projectId}/members")
    @PreProjectRole
    public Result<List<Map<String, Object>>> listMembers(@PathVariable("projectId") Long projectId) {
        return Result.ok(memberService.listMembers(projectId));
    }
}
