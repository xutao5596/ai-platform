package com.aiplatform.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class WebhookSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "URL 不能为空")
    private String url;

    /**
     * 订阅事件类型列表(JSON 数组存储到 events 字段)。
     * 例: ["flow.run.success", "flow.run.failed"]
     */
    private List<String> events;

    /** 1 启用 0 禁用 */
    private Integer status = 1;

    private String description;
}
