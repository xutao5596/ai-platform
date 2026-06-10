package com.aiplatform.project.mapper;

import com.aiplatform.project.entity.AiProjectWebhook;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiProjectWebhookMapper extends BaseMapper<AiProjectWebhook> {

    /**
     * 分页查询某项目下的 Webhook(按 id 倒序)。
     */
    default IPage<AiProjectWebhook> selectPageByProject(IPage<AiProjectWebhook> page, Long projectId) {
        return selectPage(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProjectWebhook>()
                        .eq(AiProjectWebhook::getProjectId, projectId)
                        .orderByDesc(AiProjectWebhook::getId));
    }

    /**
     * 列出某项目下全部启用的 Webhook(供事件分发用)。
     */
    default List<AiProjectWebhook> selectActiveByProject(Long projectId) {
        return selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProjectWebhook>()
                        .eq(AiProjectWebhook::getProjectId, projectId)
                        .eq(AiProjectWebhook::getStatus, 1));
    }
}
