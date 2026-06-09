package com.aiplatform.system.service;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.system.dto.LogQuery;
import com.aiplatform.system.entity.SysLog;
import com.aiplatform.system.mapper.SysLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogMapper logMapper;

    public PageResult<SysLog> page(LogQuery q) {
        Page<SysLog> page = new Page<>(q.getCurrent(), q.getSize());
        LambdaQueryWrapper<SysLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getModule())) w.eq(SysLog::getModule, q.getModule());
        if (StringUtils.hasText(q.getAction())) w.like(SysLog::getAction, q.getAction());
        if (StringUtils.hasText(q.getUsername())) w.like(SysLog::getUsername, q.getUsername());
        if (q.getStatus() != null) w.eq(SysLog::getStatus, q.getStatus());
        if (q.getStartTime() != null) w.ge(SysLog::getCreateTime, q.getStartTime());
        if (q.getEndTime() != null) w.le(SysLog::getCreateTime, q.getEndTime());
        w.orderByDesc(SysLog::getCreateTime);
        Page<SysLog> result = logMapper.selectPage(page, w);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Async
    public void saveAsync(SysLog log) {
        try {
            logMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    public void delete(List<Long> ids) {
        if (ids != null) {
            for (Long id : ids) {
                logMapper.deleteById(id);
            }
        }
    }

    public void clear() {
        logMapper.delete(new LambdaQueryWrapper<>());
    }
}
