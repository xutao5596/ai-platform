package com.aiplatform.ai.service;

import com.aiplatform.ai.dto.ModelSaveRequest;
import com.aiplatform.ai.entity.AiModel;
import com.aiplatform.ai.llm.LlmProviderFactory;
import com.aiplatform.ai.mapper.AiModelMapper;
import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelMapper modelMapper;
    private final LlmProviderFactory llmFactory;

    public PageResult<AiModel> page(String keyword, String provider, Integer status, long current, long size) {
        Page<AiModel> page = new Page<>(current, size);
        LambdaQueryWrapper<AiModel> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(z -> z.like(AiModel::getName, keyword)
                    .or().like(AiModel::getModelName, keyword));
        }
        if (StringUtils.hasText(provider)) w.eq(AiModel::getProvider, provider);
        if (status != null) w.eq(AiModel::getStatus, status);
        w.orderByDesc(AiModel::getIsDefault).orderByDesc(AiModel::getCreateTime);
        Page<AiModel> r = modelMapper.selectPage(page, w);
        return new PageResult<>(r.getCurrent(), r.getSize(), r.getTotal(), r.getRecords());
    }

    public List<AiModel> listEnabled() {
        return modelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .orderByDesc(AiModel::getIsDefault)
                .orderByAsc(AiModel::getName));
    }

    public List<AiModel> listEmbeddingModels() {
        return modelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .isNotNull(AiModel::getEmbeddingModel)
                .orderByDesc(AiModel::getIsDefault));
    }

    public AiModel get(Long id) {
        AiModel m = modelMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模型不存在");
        return m;
    }

    @Transactional
    public Long create(ModelSaveRequest req) {
        log.info("Create model: name={}, provider={}, modelName={}, apiKey.len={}", req.getName(), req.getProvider(), req.getModelName(), req.getApiKey() == null ? 0 : req.getApiKey().length());
        AiModel m = new AiModel();
        copy(req, m);
        if (m.getStatus() == null) m.setStatus(1);
        if (m.getIsDefault() == null) m.setIsDefault(0);
        modelMapper.insert(m);
        llmFactory.invalidateAll();
        return m.getId();
    }

    @Transactional
    public void update(ModelSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiModel m = modelMapper.selectById(req.getId());
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模型不存在");
        copy(req, m);
        modelMapper.updateById(m);
        llmFactory.invalidate(req.getId());
    }

    @Transactional
    public void delete(Long id) {
        AiModel m = modelMapper.selectById(id);
        if (m == null) return;
        modelMapper.deleteById(id);
        llmFactory.invalidate(id);
    }

    public boolean test(Long id) {
        try {
            AiModel m = get(id);
            // 真实实现:用 chat model 发一个简单请求
            return llmFactory.requireModel(id) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void copy(ModelSaveRequest req, AiModel m) {
        m.setId(req.getId());
        m.setName(req.getName());
        m.setProvider(req.getProvider());
        m.setModelName(req.getModelName());
        m.setApiBase(req.getApiBase());
        m.setApiKey(req.getApiKey());
        m.setProxyEnabled(req.getProxyEnabled() == null ? 0 : req.getProxyEnabled());
        m.setProxyUrl(req.getProxyUrl());
        m.setMaxTokens(req.getMaxTokens() == null ? 4096 : req.getMaxTokens());
        m.setTemperature(req.getTemperature() == null ? new java.math.BigDecimal("0.70") : req.getTemperature());
        m.setTopP(req.getTopP());
        m.setEmbeddingModel(req.getEmbeddingModel());
        m.setDimension(req.getDimension());
        m.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        m.setIsDefault(req.getIsDefault() == null ? 0 : req.getIsDefault());
        m.setDescription(req.getDescription());
    }
}
