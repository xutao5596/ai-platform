package com.aiplatform.system.service;

import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.system.dto.DeptSaveRequest;
import com.aiplatform.system.entity.SysDept;
import com.aiplatform.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    public List<SysDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder));
    }

    public List<Map<String, Object>> tree() {
        List<SysDept> all = listAll();
        Map<Long, List<SysDept>> map = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() == null ? 0L : d.getParentId()));
        return buildTree(map, 0L);
    }

    private List<Map<String, Object>> buildTree(Map<Long, List<SysDept>> map, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SysDept> children = map.getOrDefault(parentId, new ArrayList<>());
        for (SysDept d : children) {
            Map<String, Object> node = new java.util.HashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("name", d.getName());
            node.put("code", d.getCode());
            node.put("leader", d.getLeader());
            node.put("phone", d.getPhone());
            node.put("email", d.getEmail());
            node.put("sortOrder", d.getSortOrder());
            node.put("status", d.getStatus());
            node.put("children", buildTree(map, d.getId()));
            result.add(node);
        }
        return result;
    }

    @Transactional
    public Long create(DeptSaveRequest req) {
        SysDept d = toEntity(req);
        d.setId(null);
        if (d.getParentId() == null) d.setParentId(0L);
        d.setStatus(d.getStatus() == null ? 1 : d.getStatus());
        deptMapper.insert(d);
        return d.getId();
    }

    @Transactional
    public void update(DeptSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        SysDept old = deptMapper.selectById(req.getId());
        if (old == null) throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        SysDept upd = toEntity(req);
        upd.setId(old.getId());
        deptMapper.updateById(upd);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        Long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "存在子部门,不可删除");
        }
        deptMapper.deleteById(id);
    }

    private SysDept toEntity(DeptSaveRequest req) {
        SysDept d = new SysDept();
        d.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        d.setName(req.getName());
        d.setCode(req.getCode());
        d.setLeader(req.getLeader());
        d.setPhone(req.getPhone());
        d.setEmail(req.getEmail());
        d.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        d.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        return d;
    }
}
