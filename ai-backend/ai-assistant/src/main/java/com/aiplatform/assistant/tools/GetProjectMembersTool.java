package com.aiplatform.assistant.tools;

import com.aiplatform.project.entity.AiProjectMember;
import com.aiplatform.project.mapper.AiProjectMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询项目成员列表。
 */
@Slf4j
@Component
public class GetProjectMembersTool implements AssistantTool {

    private final AiProjectMemberMapper memberMapper;

    public GetProjectMembersTool(AiProjectMemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    public String name() {
        return "project_members";
    }

    @Override
    public String displayName() {
        return "查询项目成员";
    }

    @Override
    public String description() {
        return "查询指定项目的成员列表(用户ID、角色)。";
    }

    @Override
    public String category() {
        return "数据";
    }

    @Override
    public String parametersSchema() {
        return "{\"projectId\":\"number,required\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        Object pid = args.get("projectId");
        if (pid == null) return ToolResult.fail("projectId 不能为空");
        try {
            long projectId = ((Number) pid).longValue();
            List<AiProjectMember> list = memberMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProjectMember>()
                            .eq(AiProjectMember::getProjectId, projectId));
            StringBuilder sb = new StringBuilder();
            sb.append("项目 ").append(projectId).append(" 共 ").append(list.size()).append(" 个成员:\n");
            for (AiProjectMember m : list) {
                sb.append("- userId=").append(m.getUserId())
                        .append(" role=").append(m.getRoleCode()).append("\n");
            }
            return ToolResult.ok(sb.toString(), com.aiplatform.common.util.JsonUtils.toJson(list));
        } catch (Exception e) {
            log.warn("GetProjectMembersTool error", e);
            return ToolResult.fail("查询项目成员失败: " + e.getMessage());
        }
    }
}
