package com.aiplatform.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DictItemSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "字典类型不能为空")
    private String typeCode;
    @NotBlank(message = "字典项 key 不能为空")
    private String itemKey;
    @NotBlank(message = "字典项 value 不能为空")
    private String itemValue;
    private String label;
    private String color;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
