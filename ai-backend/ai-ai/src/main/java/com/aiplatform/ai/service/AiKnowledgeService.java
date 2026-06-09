package com.aiplatform.ai.service;

import com.aiplatform.ai.dto.KnowledgeSaveRequest;
import com.aiplatform.ai.entity.AiKnowledge;
import com.aiplatform.ai.entity.AiModel;
import com.aiplatform.ai.mapper.AiKnowledgeMapper;
import com.aiplatform.ai.mapper.AiModelMapper;
import com.aiplatform.ai.vector.HnswlibVectorStore;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiKnowledgeService {

    private final AiKnowledgeMapper kbMapper;
    private final AiModelMapper modelMapper;
    private final HnswlibVectorStore vectorStore;

    public List<AiKnowledge> listByProject(Long projectId) {
        return kbMapper.selectList(new LambdaQueryWrapper<AiKnowledge>()
                .eq(AiKnowledge::getProjectId, projectId)
                .orderByDesc(AiKnowledge::getCreateTime));
    }

    public List<AiKnowledge> listAll() {
        return kbMapper.selectList(new LambdaQueryWrapper<AiKnowledge>()
                .orderByDesc(AiKnowledge::getCreateTime));
    }

    public AiKnowledge get(Long id) {
        AiKnowledge k = kbMapper.selectById(id);
        if (k == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        return k;
    }

    @Transactional
    public Long create(KnowledgeSaveRequest req) {
        AiKnowledge k = new AiKnowledge();
        k.setProjectId(req.getProjectId());
        k.setName(req.getName());
        k.setDescription(req.getDescription());
        k.setModelId(req.getModelId());
        k.setChunkSize(req.getChunkSize() == null ? 500 : req.getChunkSize());
        k.setChunkOverlap(req.getChunkOverlap() == null ? 50 : req.getChunkOverlap());
        k.setSep(req.getSep() == null ? "\\n\\n" : req.getSep());
        k.setStatus(1);
        k.setDocCount(0);
        k.setChunkCount(0);
        // 预加载向量索引
        if (req.getModelId() != null) {
            AiModel m = modelMapper.selectById(req.getModelId());
            if (m != null && m.getDimension() != null) {
                vectorStore.initIndex(null, m.getDimension());
            }
        }
        kbMapper.insert(k);
        return k.getId();
    }

    @Transactional
    public void update(KnowledgeSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiKnowledge k = kbMapper.selectById(req.getId());
        if (k == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);
        k.setName(req.getName());
        k.setDescription(req.getDescription());
        k.setModelId(req.getModelId());
        if (req.getChunkSize() != null) k.setChunkSize(req.getChunkSize());
        if (req.getChunkOverlap() != null) k.setChunkOverlap(req.getChunkOverlap());
        if (req.getSep() != null) k.setSep(req.getSep());
        if (req.getStatus() != null) k.setStatus(req.getStatus());
        kbMapper.updateById(k);
    }

    @Transactional
    public void delete(Long id) {
        AiKnowledge k = kbMapper.selectById(id);
        if (k == null) return;
        kbMapper.deleteById(id);
        // 清向量索引(Sprint 2.1 完整实现)
    }
}
