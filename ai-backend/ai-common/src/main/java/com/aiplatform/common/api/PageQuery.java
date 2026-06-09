package com.aiplatform.common.api;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数。
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long current = 1L;
    private long size = 10L;
    private String orderBy;
    private String keyword;

    public long offset() {
        return Math.max(0, (current - 1) * size);
    }
}
