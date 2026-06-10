package com.aiplatform.ai.service;

import com.aiplatform.ai.entity.AiMcp;
import com.aiplatform.ai.mapper.AiMcpMapper;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiMcpService {

    private final AiMcpMapper mcpMapper;

    public List<AiMcp> listAll() {
        return mcpMapper.selectList(new LambdaQueryWrapper<AiMcp>()
                .eq(AiMcp::getStatus, 1)
                .orderByDesc(AiMcp::getCreateTime));
    }

    public AiMcp get(Long id) {
        AiMcp m = mcpMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND, "MCP 不存在");
        return m;
    }

    @Transactional
    public Long create(AiMcp req) {
        req.setId(null);
        if (req.getStatus() == null) req.setStatus(1);
        mcpMapper.insert(req);
        return req.getId();
    }

    @Transactional
    public void update(AiMcp req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiMcp old = mcpMapper.selectById(req.getId());
        if (old == null) throw new BusinessException(ErrorCode.NOT_FOUND, "MCP 不存在");
        mcpMapper.updateById(req);
    }

    @Transactional
    public void delete(Long id) {
        mcpMapper.deleteById(id);
    }
}
