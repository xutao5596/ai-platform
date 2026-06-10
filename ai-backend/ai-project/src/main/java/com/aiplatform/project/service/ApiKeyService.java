package com.aiplatform.project.service;

import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.common.util.IdUtils;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.project.dto.ApiKeySaveRequest;
import com.aiplatform.project.dto.ApiKeyVO;
import com.aiplatform.project.entity.AiProjectApiKey;
import com.aiplatform.project.mapper.AiProjectApiKeyMapper;
import com.aiplatform.project.security.ProjectRoleChecker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final AiProjectApiKeyMapper apiKeyMapper;
    private final ProjectRoleChecker projectRoleChecker;

    public List<ApiKeyVO> list(Long projectId) {
        projectRoleChecker.requireAtLeast(projectId, "admin");
        List<AiProjectApiKey> rows = apiKeyMapper.selectList(
                new LambdaQueryWrapper<AiProjectApiKey>()
                        .eq(AiProjectApiKey::getProjectId, projectId)
                        .orderByDesc(AiProjectApiKey::getCreateTime));
        return rows.stream().map(k -> toVO(k, false)).collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyVO create(Long projectId, ApiKeySaveRequest req) {
        projectRoleChecker.requireAtLeast(projectId, "admin");
        projectRoleChecker.requireProject(projectId);
        AiProjectApiKey k = new AiProjectApiKey();
        k.setProjectId(projectId);
        k.setName(req.getName());
        k.setApiKey(generateApiKey());
        k.setApiSecret(generateApiSecret());
        k.setScopes(serializeScopes(req.getScopes()));
        k.setRateLimit(req.getRateLimit() == null ? 60 : req.getRateLimit());
        k.setExpiresAt(req.getExpiresAt());
        k.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        apiKeyMapper.insert(k);
        // 创建时返回明文 secret(只此一次)
        return toVO(k, true);
    }

    @Transactional
    public ApiKeyVO update(Long projectId, Long id, ApiKeySaveRequest req) {
        projectRoleChecker.requireAtLeast(projectId, "admin");
        AiProjectApiKey k = mustGet(projectId, id);
        if (StringUtils.hasText(req.getName())) k.setName(req.getName());
        if (req.getScopes() != null) k.setScopes(serializeScopes(req.getScopes()));
        if (req.getRateLimit() != null) k.setRateLimit(req.getRateLimit());
        if (req.getExpiresAt() != null) k.setExpiresAt(req.getExpiresAt());
        if (req.getStatus() != null) k.setStatus(req.getStatus());
        apiKeyMapper.updateById(k);
        return toVO(k, false);
    }

    @Transactional
    public void delete(Long projectId, Long id) {
        projectRoleChecker.requireAtLeast(projectId, "admin");
        AiProjectApiKey k = mustGet(projectId, id);
        apiKeyMapper.deleteById(k.getId());
    }

    @Transactional
    public ApiKeyVO resetSecret(Long projectId, Long id) {
        projectRoleChecker.requireAtLeast(projectId, "admin");
        AiProjectApiKey k = mustGet(projectId, id);
        k.setApiSecret(generateApiSecret());
        apiKeyMapper.updateById(k);
        return toVO(k, true);
    }

    private AiProjectApiKey mustGet(Long projectId, Long id) {
        AiProjectApiKey k = apiKeyMapper.selectById(id);
        if (k == null || !k.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API Key 不存在");
        }
        return k;
    }

    public ApiKeyVO toVO(AiProjectApiKey k, boolean withSecret) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(k.getId());
        vo.setName(k.getName());
        vo.setApiKey(k.getApiKey());
        if (withSecret) {
            vo.setApiSecret(k.getApiSecret());
        }
        vo.setMaskedSecret(maskSecret(k.getApiSecret()));
        vo.setScopes(deserializeScopes(k.getScopes()));
        vo.setRateLimit(k.getRateLimit());
        vo.setExpiresAt(k.getExpiresAt());
        vo.setStatus(k.getStatus());
        vo.setLastUsedTime(k.getLastUsedTime());
        vo.setLastUsedIp(k.getLastUsedIp());
        vo.setCreatedAt(k.getCreateTime());
        return vo;
    }

    private String generateApiKey() {
        return "ak_" + IdUtils.uuid();
    }

    private String generateApiSecret() {
        return "sk_" + IdUtils.uuid();
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) return "********";
        return secret.substring(0, 3) + "********" + secret.substring(secret.length() - 4);
    }

    private String serializeScopes(List<String> scopes) {
        if (scopes == null) return "[]";
        return JsonUtils.toJson(scopes);
    }

    private List<String> deserializeScopes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        List<String> list = JsonUtils.fromJson(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
        });
        return list == null ? Collections.emptyList() : list;
    }
}
