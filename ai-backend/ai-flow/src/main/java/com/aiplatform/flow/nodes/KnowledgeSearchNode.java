package com.aiplatform.flow.nodes;

import com.aiplatform.ai.entity.AiKnowledge;
import com.aiplatform.ai.entity.AiKnowledgeChunk;
import com.aiplatform.ai.mapper.AiKnowledgeChunkMapper;
import com.aiplatform.ai.mapper.AiKnowledgeMapper;
import com.aiplatform.ai.vector.EmbeddingService;
import com.aiplatform.ai.vector.HnswlibVectorStore;
import com.aiplatform.flow.spi.FlowNode;
import com.aiplatform.flow.spi.NodeContext;
import com.aiplatform.flow.spi.NodeExecuteResult;
import com.aiplatform.flow.spi.NodeSchema;
import com.aiplatform.flow.spi.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索节点:在指定知识库中检索与 query 最相关的 topK 条内容。
 *
 * 配置:
 *  - kbIds: 知识库 ID 列表(逗号分隔),可选
 *  - query: 检索 query 字符串(支持 {{var}})
 *  - topK: 返回条数
 *  - outputKey: 输出变量 Key(默认 "kbResults")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchNode implements FlowNode {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final HnswlibVectorStore vectorStore;

    @Override
    public String getTypeKey() {
        return "knowledge_search";
    }

    @Override
    public String getDisplayName() {
        return "知识库检索";
    }

    @Override
    public String getCategory() {
        return "ai";
    }

    @Override
    public String getIcon() {
        return "mdi:database-search";
    }

    @Override
    public String getColor() {
        return "#E6A23C";
    }

    @Override
    public String getDescription() {
        return "在知识库中检索与 query 最相关的 topK 条内容。";
    }

    @Override
    public NodeSchema getSchema() {
        return NodeSchema.of(
                List.of(
                        new Property("kbIds", "知识库", "kb", false,
                                null, "逗号分隔的知识库 ID,留空则检索项目下所有", null),
                        new Property("query", "检索内容", "textarea", true,
                                "{{query}}", "支持 {{var}} 占位符", null),
                        new Property("topK", "返回条数", "number", false,
                                3, "返回 topK 条结果", null),
                        new Property("outputKey", "输出变量 Key", "string", false,
                                "kbResults", "把检索结果写入 variables 的 key", null)
                ),
                List.of(
                        new Property("chunks", "检索片段", "json", false, null, "命中的 chunk 列表", null),
                        new Property("context", "拼接上下文", "string", false, null, "把多个 chunk 拼接的文本", null)
                )
        );
    }

    @Override
    public NodeExecuteResult execute(NodeContext ctx) {
        String query = ctx.getConfigString("query");
        if (query == null) query = ctx.getConfigString("queryTemplate");
        if (query != null) {
            // 渲染 {{var}}
            if (ctx.getVariables() != null) {
                for (Map.Entry<String, Object> e : ctx.getVariables().entrySet()) {
                    String token = "{{" + e.getKey() + "}}";
                    if (query.contains(token)) {
                        query = query.replace(token, e.getValue() == null ? "" : String.valueOf(e.getValue()));
                    }
                }
            }
        }
        if (query == null || query.isBlank()) {
            return NodeExecuteResult.fail("知识库检索 query 不能为空");
        }
        int topK = readInt(ctx, "topK", 3);
        String outputKey = ctx.getConfigString("outputKey");
        if (outputKey == null || outputKey.isBlank()) outputKey = "kbResults";

        try {
            // 1. 取知识库列表
            List<AiKnowledge> kbs;
            String kbIdsStr = ctx.getConfigString("kbIds");
            if (kbIdsStr != null && !kbIdsStr.isBlank()) {
                List<Long> ids = new ArrayList<>();
                for (String s : kbIdsStr.split(",")) {
                    if (!s.isBlank()) ids.add(Long.parseLong(s.trim()));
                }
                kbs = knowledgeMapper.selectBatchIds(ids);
            } else if (ctx.getProjectId() != null) {
                kbs = knowledgeMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledge>()
                                .eq(AiKnowledge::getProjectId, ctx.getProjectId()));
            } else {
                kbs = knowledgeMapper.selectList(null);
            }
            if (kbs == null || kbs.isEmpty()) {
                Map<String, Object> out = new HashMap<>();
                out.put("chunks", List.of());
                out.put("context", "");
                out.put(outputKey, List.of());
                return NodeExecuteResult.success(out);
            }

            // 2. 计算 query 向量(简化:256 维)
            int dim = 256;
            float[] qv = embeddingService.embed(query, dim);

            // 3. 检索
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
                    m.put("chunkId", chunk.getId());
                    m.put("content", chunk.getContent());
                    m.put("score", s.score());
                    hits.add(m);
                }
            }
            // 按 score 排序
            hits.sort((a, b) -> Float.compare(
                    ((Number) b.get("score")).floatValue(),
                    ((Number) a.get("score")).floatValue()));
            if (hits.size() > topK) hits = hits.subList(0, topK);

            // 4. 拼 context
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> h : hits) {
                sb.append(h.get("content")).append("\n\n");
            }

            Map<String, Object> out = new HashMap<>();
            out.put("chunks", hits);
            out.put("context", sb.toString());
            out.put(outputKey, hits);
            return NodeExecuteResult.success(out);
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return NodeExecuteResult.fail("知识库检索失败: " + e.getMessage());
        }
    }

    private Integer readInt(NodeContext ctx, String key, int def) {
        Object v = ctx.getConfig(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
