package com.aiplatform.system.service;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.system.dto.MenuSaveRequest;
import com.aiplatform.system.dto.MenuTreeNode;
import com.aiplatform.system.entity.SysMenu;
import com.aiplatform.system.mapper.SysMenuMapper;
import com.aiplatform.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<MenuTreeNode> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
        return buildTree(all, 0L);
    }

    public List<MenuTreeNode> listForCurrentUser() {
        LoginUser u = UserContext.get();
        if (u == null) return List.of();
        if (Boolean.TRUE.equals(u.getAdmin())) {
            return tree();
        }
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(u.getUserId());
        if (roleIds == null || roleIds.isEmpty()) return List.of();
        Set<Long> menuIdSet = new HashSet<>();
        for (Long rid : roleIds) {
            menuIdSet.addAll(menuMapper.selectMenuIdsByRoleId(rid));
        }
        if (menuIdSet.isEmpty()) return List.of();
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIdSet);
        return buildTree(menus, 0L);
    }

    public List<String> permissionsForCurrentUser() {
        LoginUser u = UserContext.get();
        if (u == null || u.getPermissions() == null) return List.of();
        return new ArrayList<>(u.getPermissions());
    }

    public List<MenuTreeNode> buildTree(List<SysMenu> all, Long parentId) {
        Map<Long, List<SysMenu>> map = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        List<SysMenu> roots = map.getOrDefault(parentId, List.of());
        return roots.stream().map(m -> toNode(m, map)).collect(Collectors.toList());
    }

    private MenuTreeNode toNode(SysMenu m, Map<Long, List<SysMenu>> map) {
        MenuTreeNode n = new MenuTreeNode();
        n.setId(m.getId());
        n.setParentId(m.getParentId());
        n.setName(m.getName());
        n.setTitle(m.getTitle());
        n.setPath(m.getPath());
        n.setComponent(m.getComponent());
        n.setIcon(m.getIcon());
        n.setPermCode(m.getPermCode());
        n.setType(m.getType());
        n.setSortOrder(m.getSortOrder());
        n.setVisible(m.getVisible());
        n.setStatus(m.getStatus());
        List<SysMenu> children = map.getOrDefault(m.getId(), List.of());
        n.setChildren(children.stream().map(c -> toNode(c, map)).collect(Collectors.toList()));
        return n;
    }

    @Transactional
    public Long create(MenuSaveRequest req) {
        SysMenu m = toEntity(req);
        m.setId(null);
        if (m.getParentId() == null) m.setParentId(0L);
        menuMapper.insert(m);
        return m.getId();
    }

    @Transactional
    public void update(MenuSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        SysMenu m = menuMapper.selectById(req.getId());
        if (m == null) throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        SysMenu upd = toEntity(req);
        upd.setId(m.getId());
        menuMapper.updateById(upd);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "存在子菜单,不可删除");
        }
        menuMapper.deleteById(id);
        menuMapper.deleteRoleMenusByMenuId(id);
    }

    private SysMenu toEntity(MenuSaveRequest req) {
        SysMenu m = new SysMenu();
        m.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        m.setName(req.getName());
        m.setTitle(req.getTitle());
        m.setPath(req.getPath());
        m.setComponent(req.getComponent());
        m.setIcon(req.getIcon());
        m.setPermCode(req.getPermCode());
        m.setType(req.getType());
        m.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        m.setVisible(req.getVisible() == null ? 1 : req.getVisible());
        m.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        m.setRedirect(req.getRedirect());
        m.setRemark(req.getRemark());
        return m;
    }
}
