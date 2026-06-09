package com.aiplatform.ai.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * 文档解析服务:基于 Apache Tika 提取文本。
 */
@Slf4j
@Service
public class DocumentParser {

    private final Tika tika = new Tika();

    public String parse(File file) throws IOException {
        try {
            String content = tika.parseToString(file);
            log.info("Parsed {} chars from {}", content.length(), file.getName());
            return content;
        } catch (TikaException e) {
            throw new IOException("Tika parse failed: " + e.getMessage(), e);
        }
    }

    /**
     * 简单分段:按双换行或固定长度切分。
     */
    public java.util.List<String> chunk(String content, int chunkSize, int overlap) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (content == null || content.isBlank()) return out;
        String text = content.replaceAll("\\s+", " ").trim();
        if (text.length() <= chunkSize) {
            out.add(text);
            return out;
        }
        int step = Math.max(1, chunkSize - overlap);
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(text.length(), i + chunkSize);
            String s = text.substring(i, end);
            if (!s.isBlank()) out.add(s);
            if (end == text.length()) break;
        }
        return out;
    }
}
