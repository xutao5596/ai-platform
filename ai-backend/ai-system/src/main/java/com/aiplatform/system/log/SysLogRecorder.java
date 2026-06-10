package com.aiplatform.system.log;

import com.aiplatform.framework.log.OperationLogRecorder;
import com.aiplatform.system.entity.SysLog;
import com.aiplatform.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ai-system 侧实现:把 OperationLogSnapshot 转为 SysLog 写入数据库。
 * <p>
 * 切面已经将 record() 调度到 operationLogExecutor 异步线程;
 * 此处直接把 DTO 转 Entity 并调用 SysLogService.saveAsync()(内部已 @Async)
 * 完成落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysLogRecorder implements OperationLogRecorder {

    private final SysLogService sysLogService;

    @Override
    public void record(OperationLogSnapshot snap) {
        try {
            SysLog entity = new SysLog();
            entity.setModule(snap.getModule());
            entity.setAction(snap.getAction());
            entity.setMethod(snap.getMethod());
            entity.setRequestUrl(snap.getRequestUrl());
            entity.setRequestMethod(snap.getRequestMethod());
            entity.setRequestParams(snap.getRequestParams());
            entity.setResponseData(snap.getResponseData());
            entity.setUserId(snap.getUserId());
            entity.setUsername(snap.getUsername());
            entity.setIp(snap.getIp());
            entity.setUserAgent(snap.getUserAgent());
            entity.setCostMs(snap.getCostMs());
            entity.setStatus(snap.getStatus());
            entity.setErrorMsg(snap.getErrorMsg());
            entity.setCreateTime(snap.getCreateTime());
            sysLogService.saveAsync(entity);
        } catch (Exception e) {
            log.warn("SysLogRecorder persist failed: {}", e.getMessage());
        }
    }
}
