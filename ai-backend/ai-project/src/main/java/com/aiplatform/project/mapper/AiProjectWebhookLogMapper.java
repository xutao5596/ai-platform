package com.aiplatform.project.mapper;

import com.aiplatform.project.entity.AiProjectWebhookLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiProjectWebhookLogMapper extends BaseMapper<AiProjectWebhookLog> {

    /**
     * 分页按 webhookId 倒序查询投递日志。
     */
    default IPage<AiProjectWebhookLog> selectPageByWebhook(IPage<AiProjectWebhookLog> page, Long webhookId) {
        return selectPage(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiProjectWebhookLog>()
                        .eq(AiProjectWebhookLog::getWebhookId, webhookId)
                        .orderByDesc(AiProjectWebhookLog::getId));
    }
}
