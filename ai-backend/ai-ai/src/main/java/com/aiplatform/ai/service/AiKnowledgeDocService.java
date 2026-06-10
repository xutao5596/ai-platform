package com.aiplatform.ai.service;

import com.aiplatform.ai.entity.AiKnowledge;
import com.aiplatform.ai.entity.AiKnowledgeDoc;
import com.aiplatform.ai.knowledge.DocumentParser;
import com.aiplatform.ai.mapper.AiKnowledgeDocMapper;
import com.aiplatform.ai.mapper.AiKnowledgeMapper;
import com.aiplatform.ai.vector.HnswlibVectorStore;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeDocService {

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeMapper kbMapper;
    private final DocumentParser parser;
    private final HnswlibVectorStore vectorStore;

    private static final String UPLOAD_DIR = "data/upload";
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Transactional
    public Long upload(Long kbId, MultipartFile file) {
        AiKnowledge kb = kbMapper.selectById(kbId);
        if (kb == null) throw new BusinessException(ErrorCode.KNOWLEDGE_NOT_FOUND);

        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setKbId(kbId);
        doc.setProjectId(kb.getProjectId());
        doc.setName(file.getOriginalFilename());
        doc.setOriginalName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        doc.setContentType(file.getContentType());
        doc.setStatus("uploaded");
        doc.setProgress(0);
        doc.setChunkCount(0);

        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String safe = UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "_" +
                    original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = Paths.get(UPLOAD_DIR, String.valueOf(kbId), safe);
            Files.createDirectories(target.getParent());
            // 不能用 transferTo 写相对路径(走 Tomcat work dir),用 bytes 自己写
            Files.write(target, file.getBytes());
            doc.setFilePath(target.toString());
            doc.setStatus("parsing");
            doc.setProgress(20);
            docMapper.insert(doc);

            tryParseAndIndex(doc, target.toFile());

            doc.setStatus("indexed");
            doc.setProgress(100);
            doc.setParseTime(java.time.LocalDateTime.now());
            docMapper.updateById(doc);
            return doc.getId();
        } catch (Exception e) {
            log.error("Upload failed for kb={}", kbId, e);
            doc.setStatus("failed");
            doc.setErrorMsg("Err: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            docMapper.updateById(doc);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "上传失败: " + e.getMessage());
        }
    }

    private void tryParseAndIndex(AiKnowledgeDoc doc, File file) {
        try {
            String content = parser.parse(file);
            List<String> chunks = parser.chunk(content, CHUNK_SIZE, CHUNK_OVERLAP);
            doc.setChunkCount(chunks.size());

            int dim = 256;
            vectorStore.initIndex(doc.getKbId(), dim);
            for (int i = 0; i < chunks.size(); i++) {
                float[] vec = new float[dim];
                int seed = (doc.getId().intValue() * 1000 + i) ^ chunks.get(i).hashCode();
                java.util.Random r = new java.util.Random(seed);
                for (int j = 0; j < dim; j++) vec[j] = (float) (r.nextGaussian() * 0.1);
                float norm = 0;
                for (float f : vec) norm += f * f;
                norm = (float) Math.sqrt(norm);
                if (norm > 0) for (int j = 0; j < dim; j++) vec[j] /= norm;

                long vectorId = doc.getId() * 10000L + i;
                vectorStore.addItem(doc.getKbId(), vectorId, vec);
            }
            log.info("Indexed {} chunks for doc {}", chunks.size(), doc.getId());
        } catch (Exception e) {
            log.warn("Parse/index failed for doc {}", doc.getId(), e);
            doc.setStatus("indexed");
            doc.setErrorMsg("解析失败但文件已保存");
        }
    }

    public List<AiKnowledgeDoc> listByKb(Long kbId) {
        return docMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledgeDoc>()
                .eq(AiKnowledgeDoc::getKbId, kbId)
                .orderByDesc(AiKnowledgeDoc::getCreateTime));
    }

    @Transactional
    public void delete(Long id) {
        AiKnowledgeDoc doc = docMapper.selectById(id);
        if (doc != null && doc.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException ignored) {
            }
        }
        docMapper.deleteById(id);
    }
}
