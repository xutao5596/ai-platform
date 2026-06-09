package com.aiplatform.flow.spi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 节点属性项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 字段 Key */
    private String key;
    /** 字段 Label(中文) */
    private String label;
    /** 字段类型:string/number/boolean/select/json/textarea/model/kb/prompt */
    private String type;
    /** 是否必填 */
    private boolean required;
    /** 默认值 */
    private Object defaultValue;
    /** 描述/帮助 */
    private String description;
    /** select 类型时的可选项 */
    private List<Option> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String label;
        private String value;
    }
}
