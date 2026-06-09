package com.aiplatform.system.service;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.system.dto.UserQuery;
import com.aiplatform.system.dto.UserSaveRequest;
import com.aiplatform.system.dto.UserVO;
import com.aiplatform.system.entity.SysUser;
import com.aiplatform.system.entity.SysUserRole;
import com.aiplatform.system.mapper.SysDeptMapper;
import com.aiplatform.system.mapper.SysRoleMapper;
import com.aiplatform.system.mapper.SysUserMapper;
import com.aiplatform.system.mapper.SysUserRoleMapper;
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
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public PageResult<UserVO> page(UserQuery q) {
        Page<SysUser> page = new Page<>(q.getCurrent(), q.getSize());
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            w.and(z -> z.like(SysUser::getUsername, q.getKeyword())
                    .or().like(SysUser::getRealName, q.getKeyword())
                    .or().like(SysUser::getPhone, q.getKeyword())
                    .or().like(SysUser::getEmail, q.getKeyword()));
        }
        if (q.getDeptId() != null) {
            w.eq(SysUser::getDeptId, q.getDeptId());
        }
        if (q.getStatus() != null) {
            w.eq(SysUser::getStatus, q.getStatus());
        }
        w.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, w);
        List<UserVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), vos);
    }

    public UserVO get(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(u);
    }

    @Transactional
    public Long create(UserSaveRequest req) {
        checkUsername(req.getUsername(), null);
        SysUser u = new SysUser();
        copy(req, u);
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        u.setPassword(PasswordUtil.hash(req.getPassword()));
        u.setAdmin(0);
        u.setStatus(u.getStatus() == null ? 1 : u.getStatus());
        userMapper.insert(u);
        if (req.getRoleIds() != null) {
            assignRoles(u.getId(), req.getRoleIds());
        }
        return u.getId();
    }

    @Transactional
    public void update(UserSaveRequest req) {
        if (req.getId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        }
        SysUser old = userMapper.selectById(req.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!old.getUsername().equals(req.getUsername())) {
            checkUsername(req.getUsername(), req.getId());
        }
        copy(req, old);
        if (StringUtils.hasText(req.getPassword())) {
            old.setPassword(PasswordUtil.hash(req.getPassword()));
        }
        userMapper.updateById(old);
        if (req.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(old.getId());
            assignRoles(old.getId(), req.getRoleIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) return;
        SysUser u = userMapper.selectById(id);
        if (u == null) return;
        if (Integer.valueOf(1).equals(u.getAdmin())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "超级管理员不可删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids != null) {
            ids.forEach(this::delete);
        }
    }

    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PasswordUtil.matches(oldPwd, u.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码错误");
        }
        u.setPassword(PasswordUtil.hash(newPwd));
        userMapper.updateById(u);
    }

    public void resetPassword(Long userId, String newPwd) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        u.setPassword(PasswordUtil.hash(newPwd));
        userMapper.updateById(u);
    }

    public void updateStatus(Long userId, Integer status) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        u.setStatus(status);
        userMapper.updateById(u);
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        for (Long rid : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
    }

    private void checkUsername(String username, Long excludeId) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
    }

    private void copy(UserSaveRequest req, SysUser u) {
        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setNickname(req.getNickname());
        u.setAvatar(req.getAvatar());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setGender(req.getGender());
        u.setDeptId(req.getDeptId());
        u.setStatus(req.getStatus());
        u.setRemark(req.getRemark());
    }

    public UserVO toVO(SysUser u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setEmail(u.getEmail());
        vo.setPhone(u.getPhone());
        vo.setGender(u.getGender());
        vo.setDeptId(u.getDeptId());
        vo.setStatus(u.getStatus());
        vo.setAdmin(u.getAdmin());
        vo.setRemark(u.getRemark());
        if (u.getDeptId() != null) {
            var dept = deptMapper.selectById(u.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        if (u.getCreateTime() != null) {
            vo.setCreateTime(u.getCreateTime().toString());
        }
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(u.getId());
        vo.setRoleIds(roleIds);
        if (roleIds != null && !roleIds.isEmpty()) {
            vo.setRoleCodes(roleMapper.selectBatchIds(roleIds).stream()
                    .map(r -> r.getCode()).collect(Collectors.toList()));
        }
        return vo;
    }
}
