package com.aiplatform.assistant.tools;

import com.aiplatform.ai.entity.AiKnowledge;
import com.aiplatform.ai.entity.AiKnowledgeChunk;
import com.aiplatform.ai.mapper.AiKnowledgeChunkMapper;
import com.aiplatform.ai.mapper.AiKnowledgeMapper;
import com.aiplatform.ai.vector.EmbeddingService;
import com.aiplatform.ai.vector.HnswlibVectorStore;
import com.aiplatform.assistant.tools.AssistantTool;
import com.aiplatform.assistant.tools.ToolContext;
import com.aiplatform.assistant.tools.ToolResult;
import com.aiplatform.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索工具:在指定知识库列表上做相似度检索,返回 topK 条 chunk。
 * PoC:使用 hash-based embedding(EmbeddingService)+ 内存向量库。
 */
@Slf4j
@Component
public class KnowledgeSearchTool implements AssistantTool {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final HnswlibVectorStore vectorStore;

    public KnowledgeSearchTool(AiKnowledgeMapper knowledgeMapper,
                               AiKnowledgeChunkMapper chunkMapper,
                               EmbeddingService embeddingService,
                               HnswlibVectorStore vectorStore) {
        this.knowledgeMapper = knowledgeMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Override
    public String name() {
        return "search_kb";
    }

    @Override
    public String displayName() {
        return "知识库检索";
    }

    @Override
    public String description() {
        return "在指定知识库 ID 列表(逗号分隔)中检索与 query 最相关的 topK 条内容。"
                + "返回内容片段及得分。可选参数 kbIds(逗号分隔),不传则在项目下所有 kb 中检索。";
    }

    @Override
    public String category() {
        return "AI";
    }

    @Override
    public String parametersSchema() {
        return "{\"query\":\"string,required\",\"kbIds\":\"string,optional(逗号分隔)\",\"topK\":\"number,default:3\"}";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        String query = (String) args.get("query");
        if (query == null || query.isBlank()) return ToolResult.fail("query 不能为空");
        String kbIdsStr = (String) args.get("kbIds");
        int topK = args.get("topK") == null ? 3 : ((Number) args.get("topK")).intValue();
        Long projectId = context.getProjectId();

        try {
            List<AiKnowledge> kbs;
            if (kbIdsStr != null && !kbIdsStr.isBlank()) {
                List<Long> ids = new ArrayList<>();
                for (String s : kbIdsStr.split(",")) {
                    if (!s.isBlank()) ids.add(Long.parseLong(s.trim()));
                }
                kbs = knowledgeMapper.selectBatchIds(ids);
            } else if (projectId != null) {
                kbs = knowledgeMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledge>()
                        .eq(AiKnowledge::getProjectId, projectId));
            } else {
                kbs = knowledgeMapper.selectList(null);
            }
            if (kbs.isEmpty()) {
                if (kbIdsStr != null && !kbIdsStr.isBlank()) {
                    return ToolResult.fail("指定的 kbIds 不存在: " + kbIdsStr);
                }
                return ToolResult.ok("未找到可用知识库");
            }

            int dim = 256; // PoC 维度,统一 hash-based
            float[] qv = embeddingService.embed(query, dim);
            List<Map<String, Object>> hits = new ArrayList<>();
            for (AiKnowledge kb : kbs) {
                List<HnswlibVectorStore.ScoredResult> sr = vectorStore.search(kb.getId(), qv, topK);
                for (HnswlibVectorStore.ScoredResult s : sr) {
                    AiKnowledgeChunk chunk = chunkMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledgeChunk>()
                                    .eq(AiKnowledgeChunk::getVectorId, s.vectorId()));
                    if (chunk == null) continue;
                    Map<String, Object> m = new HashMap<>();
                    m.put("kbId", kb.getId());
                    m.put("kbName", kb.getName());
                    m.put("chunk", truncate(chunk.getContent(), 500));
                    m.put("score", s.score());
                    hits.add(m);
                }
            }
            if (hits.isEmpty()) return ToolResult.ok("未检索到相关结果");
            hits.sort((a, b) -> Float.compare(((Number) b.get("score")).floatValue(), ((Number) a.get("score")).floatValue()));
            if (hits.size() > topK) hits = hits.subList(0, topK);
            String summary = String.format("检索到 %d 条相关结果", hits.size());
            String detail = JsonUtils.toJson(hits);
            return ToolResult.ok(summary, detail);
        } catch (Exception e) {
            log.warn("KnowledgeSearchTool error", e);
            return ToolResult.fail("知识库检索失败: " + e.getMessage());
        }
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }
}
