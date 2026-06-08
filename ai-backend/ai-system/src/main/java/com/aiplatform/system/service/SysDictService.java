package com.aiplatform.system.service;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.system.dto.DictItemSaveRequest;
import com.aiplatform.system.dto.DictSaveRequest;
import com.aiplatform.system.entity.SysDict;
import com.aiplatform.system.entity.SysDictItem;
import com.aiplatform.system.mapper.SysDictItemMapper;
import com.aiplatform.system.mapper.SysDictMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper itemMapper;

    public PageResult<SysDict> page(String keyword, long current, long size) {
        Page<SysDict> page = new Page<>(current, size);
        LambdaQueryWrapper<SysDict> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(z -> z.like(SysDict::getTypeCode, keyword).or().like(SysDict::getTypeName, keyword));
        }
        w.orderByDesc(SysDict::getCreateTime);
        Page<SysDict> result = dictMapper.selectPage(page, w);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    public List<SysDict> listAll() {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>().orderByDesc(SysDict::getCreateTime));
    }

    public List<SysDictItem> itemsByType(String typeCode) {
        return itemMapper.selectByTypeCode(typeCode);
    }

    @Transactional
    public Long createDict(DictSaveRequest req) {
        SysDict d = new SysDict();
        d.setTypeCode(req.getTypeCode());
        d.setTypeName(req.getTypeName());
        d.setDescription(req.getDescription());
        d.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        dictMapper.insert(d);
        return d.getId();
    }

    @Transactional
    public void updateDict(DictSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        SysDict d = dictMapper.selectById(req.getId());
        if (d == null) throw new BusinessException(ErrorCode.NOT_FOUND, "字典不存在");
        d.setTypeCode(req.getTypeCode());
        d.setTypeName(req.getTypeName());
        d.setDescription(req.getDescription());
        d.setStatus(req.getStatus());
        dictMapper.updateById(d);
    }

    @Transactional
    public void deleteDict(Long id) {
        SysDict d = dictMapper.selectById(id);
        if (d == null) return;
        dictMapper.deleteById(id);
        itemMapper.deleteByTypeCode(d.getTypeCode());
    }

    @Transactional
    public Long createItem(DictItemSaveRequest req) {
        SysDictItem i = toItemEntity(req);
        i.setId(null);
        itemMapper.insert(i);
        return i.getId();
    }

    @Transactional
    public void updateItem(DictItemSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        SysDictItem i = itemMapper.selectById(req.getId());
        if (i == null) throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在");
        SysDictItem upd = toItemEntity(req);
        upd.setId(i.getId());
        itemMapper.updateById(upd);
    }

    @Transactional
    public void deleteItem(Long id) {
        if (id == null) return;
        itemMapper.deleteById(id);
    }

    private SysDictItem toItemEntity(DictItemSaveRequest req) {
        SysDictItem i = new SysDictItem();
        i.setTypeCode(req.getTypeCode());
        i.setItemKey(req.getItemKey());
        i.setItemValue(req.getItemValue());
        i.setLabel(req.getLabel());
        i.setColor(req.getColor());
        i.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        i.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        i.setRemark(req.getRemark());
        return i;
    }
}
