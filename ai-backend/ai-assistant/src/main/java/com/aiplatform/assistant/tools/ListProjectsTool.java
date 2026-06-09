package com.aiplatform.assistant.tools;

import com.aiplatform.project.entity.AiProject;
import com.aiplatform.project.mapper.AiProjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 列出当前用户可见的项目。
 */
@Slf4j
@Component
public class ListProjectsTool implements AssistantTool {

    private final AiProjectMapper projectMapper;

    public ListProjectsTool(AiProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public String name() {
        return "list_projects";
    }

    @Override
    public String displayName() {
        return "查询项目列表";
    }

    @Override
    public String description() {
        return "查询系统中所有项目(简略:ID、名称、状态)。";
    }

    @Override
    public String category() {
        return "数据";
    }

    @Override
    public String parametersSchema() {
        return "{\"keyword\":\"string,optional(按名称模糊)\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            String keyword = (String) args.get("keyword");
            List<AiProject> list = projectMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProject>()
                            .like(keyword != null && !keyword.isBlank(), AiProject::getName, keyword)
                            .orderByDesc(AiProject::getCreateTime));
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(list.size()).append(" 个项目:\n");
            for (AiProject p : list) {
                sb.append("- [").append(p.getId()).append("] ")
                        .append(p.getName()).append(" (")
                        .append(p.getStatus() == null ? "?" : p.getStatus()).append(")\n");
            }
            Map<String, Object> detail = new HashMap<>();
            detail.put("count", list.size());
            detail.put("items", list);
            return ToolResult.ok(sb.toString(), com.aiplatform.common.util.JsonUtils.toJson(detail));
        } catch (Exception e) {
            log.warn("ListProjectsTool error", e);
            return ToolResult.fail("查询项目失败: " + e.getMessage());
        }
    }
}
