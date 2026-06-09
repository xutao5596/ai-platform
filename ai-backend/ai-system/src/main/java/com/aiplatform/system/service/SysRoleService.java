package com.aiplatform.system.service;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.system.dto.RoleSaveRequest;
import com.aiplatform.system.dto.RoleVO;
import com.aiplatform.system.entity.SysRole;
import com.aiplatform.system.entity.SysRoleMenu;
import com.aiplatform.system.entity.SysRolePermission;
import com.aiplatform.system.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    public PageResult<RoleVO> page(String keyword, long current, long size) {
        Page<SysRole> page = new Page<>(current, size);
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(z -> z.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword));
        }
        w.orderByAsc(SysRole::getSortOrder);
        Page<SysRole> result = roleMapper.selectPage(page, w);
        List<RoleVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), vos);
    }

    public List<RoleVO> listAll() {
        List<SysRole> list = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortOrder));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    public RoleVO get(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        return toVO(r);
    }

    @Transactional
    public Long create(RoleSaveRequest req) {
        SysRole r = new SysRole();
        r.setName(req.getName());
        r.setCode(req.getCode());
        r.setDescription(req.getDescription());
        r.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        r.setDataScope(req.getDataScope());
        r.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        roleMapper.insert(r);
        saveRelations(r.getId(), req.getMenuIds(), req.getPermissions());
        return r.getId();
    }

    @Transactional
    public void update(RoleSaveRequest req) {
        SysRole r = roleMapper.selectById(req.getId());
        if (r == null) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        r.setName(req.getName());
        r.setCode(req.getCode());
        r.setDescription(req.getDescription());
        r.setStatus(req.getStatus());
        r.setDataScope(req.getDataScope());
        r.setSortOrder(req.getSortOrder());
        roleMapper.updateById(r);
        saveRelations(r.getId(), req.getMenuIds(), req.getPermissions());
    }

    @Transactional
    public void delete(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) return;
        if ("admin".equalsIgnoreCase(r.getCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统角色不可删除");
        }
        Long userCount = (long) userRoleMapper.selectUserIdsByRoleId(id).size();
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        rolePermissionMapper.deleteByRoleId(id);
    }

    private void saveRelations(Long roleId, List<Long> menuIds, List<String> perms) {
        if (menuIds != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
            for (Long mid : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(mid);
                roleMenuMapper.insert(rm);
            }
        }
        if (perms != null) {
            rolePermissionMapper.deleteByRoleId(roleId);
            for (String p : perms) {
                if (p == null || p.isBlank()) continue;
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermission(p);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    public RoleVO toVO(SysRole r) {
        RoleVO vo = new RoleVO();
        vo.setId(r.getId());
        vo.setName(r.getName());
        vo.setCode(r.getCode());
        vo.setDescription(r.getDescription());
        vo.setStatus(r.getStatus());
        vo.setDataScope(r.getDataScope());
        vo.setSortOrder(r.getSortOrder());
        vo.setMenuIds(menuMapper.selectMenuIdsByRoleId(r.getId()));
        vo.setPermissions(rolePermissionMapper.selectPermissionsByRoleId(r.getId()));
        if (r.getCreateTime() != null) vo.setCreateTime(r.getCreateTime().toString());
        return vo;
    }
}
