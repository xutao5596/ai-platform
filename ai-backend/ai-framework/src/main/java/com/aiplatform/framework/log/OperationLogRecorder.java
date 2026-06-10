package com.aiplatform.framework.log;

import java.time.LocalDateTime;

/**
 * 审计日志持久化回调。
 * <p>
 * ai-framework 不直接依赖 ai-system,因此框架内只暴露接口;
 * 实际写库实现由 ai-system 提供(SysLogRecorder),通过 ObjectProvider 注入,
 * 不存在时切面安全 no-op。
 */
public interface OperationLogRecorder {

    /**
     * 持久化单条审计日志(由切面异步调用,实现内部也建议异步或快速返回)。
     *
     * @param record 操作日志快照
     */
    void record(OperationLogSnapshot record);

    /**
     * 日志快照(框架侧中性 DTO,避免直接依赖 ai-system 的 SysLog)。
     */
    @lombok.Data
    class OperationLogSnapshot {
        private String module;
        private String action;
        private String method;
        private String requestUrl;
        private String requestMethod;
        private String requestParams;
        private String responseData;
        private Long userId;
        private String username;
        private String ip;
        private String userAgent;
        private Long costMs;
        private Integer status;
        private String errorMsg;
        private LocalDateTime createTime;
    }
}
