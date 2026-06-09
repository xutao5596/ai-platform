package com.aiplatform.flow.controller;

import com.aiplatform.common.api.Result;
import com.aiplatform.flow.dto.TriggerSaveRequest;
import com.aiplatform.flow.entity.AiFlowTrigger;
import com.aiplatform.flow.mapper.AiFlowTriggerMapper;
import com.aiplatform.flow.trigger.CronTrigger;
import com.aiplatform.flow.trigger.WebhookTrigger;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.annotation.PreProjectRole;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flow/{id}/triggers")
@RequiredArgsConstructor
public class FlowTriggerController {

    private final AiFlowTriggerMapper triggerMapper;
    private final ProjectRoleChecker projectRoleChecker;
    private final WebhookTrigger webhookTrigger;
    private final CronTrigger cronTrigger;

    @GetMapping
    public Result<List<AiFlowTrigger>> list(@PathVariable Long id) {
        return Result.ok(triggerMapper.selectList(
                new LambdaQueryWrapper<AiFlowTrigger>()
                        .eq(AiFlowTrigger::getFlowId, id)
                        .orderByDesc(AiFlowTrigger::getCreateTime)));
    }

    @PostMapping
    @PreProjectRole("owner")
    @Transactional
    public Result<Long> create(@PathVariable Long id, @RequestBody TriggerSaveRequest req) {
        if (req.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(req.getProjectId(), "owner");
        }
        AiFlowTrigger t = new AiFlowTrigger();
        t.setFlowId(id);
        t.setProjectId(req.getProjectId());
        t.setType(req.getType());
        t.setConfig(req.getConfig());
        t.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        triggerMapper.insert(t);
        // 如果是 cron,注册调度
        if ("cron".equals(req.getType())) {
            cronTrigger.schedule(t);
        }
        return Result.ok(t.getId());
    }

    @PutMapping("/{triggerId}")
    @PreProjectRole("owner")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @PathVariable Long triggerId, @RequestBody TriggerSaveRequest req) {
        AiFlowTrigger t = triggerMapper.selectById(triggerId);
        if (t == null) throw new BusinessException(ErrorCode.NOT_FOUND, "触发器不存在");
        if (!t.getFlowId().equals(id)) throw new BusinessException(ErrorCode.BAD_REQUEST, "触发器不属于该流程");
        if (t.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(t.getProjectId(), "owner");
        }
        if (req.getType() != null) t.setType(req.getType());
        if (req.getConfig() != null) t.setConfig(req.getConfig());
        if (req.getStatus() != null) t.setStatus(req.getStatus());
        triggerMapper.updateById(t);
        // 重新注册 cron
        if ("cron".equals(t.getType())) {
            cronTrigger.schedule(t);
        }
        return Result.ok();
    }

    @DeleteMapping("/{triggerId}")
    @PreProjectRole("owner")
    public Result<Void> delete(@PathVariable Long id, @PathVariable Long triggerId) {
        AiFlowTrigger t = triggerMapper.selectById(triggerId);
        if (t == null) return Result.ok();
        if (t.getProjectId() != null) {
            projectRoleChecker.requireAtLeast(t.getProjectId(), "owner");
        }
        if ("cron".equals(t.getType())) {
            cronTrigger.cancel(triggerId);
        }
        triggerMapper.deleteById(triggerId);
        return Result.ok();
    }
}
