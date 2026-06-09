package com.aiplatform.flow.spi;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行结果。
 */
@Data
public class NodeExecuteResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;
    /** 错误信息(success=false 时) */
    private String errorMsg;
    /** 输出变量(k -> v) */
    private Map<String, Object> output;

    public static NodeExecuteResult success() {
        return success(new HashMap<>());
    }

    public static NodeExecuteResult success(Map<String, Object> output) {
        NodeExecuteResult r = new NodeExecuteResult();
        r.setSuccess(true);
        r.setOutput(output);
        return r;
    }

    public static NodeExecuteResult fail(String errorMsg) {
        NodeExecuteResult r = new NodeExecuteResult();
        r.setSuccess(false);
        r.setErrorMsg(errorMsg);
        return r;
    }
}
