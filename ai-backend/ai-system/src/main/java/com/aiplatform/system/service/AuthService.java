package com.aiplatform.system.service;

import cn.hutool.core.util.IdUtil;
import com.aiplatform.common.api.Result;
import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.framework.jwt.JwtTokenProvider;
import com.aiplatform.system.dto.LoginRequest;
import com.aiplatform.system.dto.LoginResponse;
import com.aiplatform.system.entity.*;
import com.aiplatform.system.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysDeptMapper deptMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest req, String clientIp) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        if (!PasswordUtil.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        LoginUser loginUser = buildLoginUser(user);
        loginUser.setLoginIp(clientIp);
        loginUser.setLoginTime(System.currentTimeMillis());
        String access = jwtTokenProvider.generateAccessToken(loginUser);
        String refresh = jwtTokenProvider.generateRefreshToken(loginUser);
        loginUser.setToken(access);
        loginUser.setExpireTime(System.currentTimeMillis() + jwtTokenProvider.getAccessTtlSeconds() * 1000L);

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        resp.setExpiresIn(jwtTokenProvider.getAccessTtlSeconds());
        resp.setUser(toUserInfo(loginUser));
        return resp;
    }

    public void logout() {
        UserContext.clear();
    }

    public LoginResponse refresh(String refreshToken) {
        var claims = jwtTokenProvider.parse(refreshToken);
        if (claims == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token 无效");
        }
        Long uid = Long.valueOf(claims.getSubject());
        SysUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LoginUser loginUser = buildLoginUser(user);
        String access = jwtTokenProvider.generateAccessToken(loginUser);
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(access);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtTokenProvider.getAccessTtlSeconds());
        resp.setUser(toUserInfo(loginUser));
        return resp;
    }

    public LoginResponse.UserInfo profile() {
        LoginUser u = UserContext.get();
        if (u == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(u.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toUserInfo(buildLoginUser(user));
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser.LoginUserBuilder b = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .deptId(user.getDeptId())
                .admin(Integer.valueOf(1).equals(user.getAdmin()));
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) {
                b.deptName(dept.getName());
            }
        }
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        Set<String> roleCodes = new HashSet<>();
        Set<String> perms = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            for (SysRole r : roles) {
                if (Integer.valueOf(0).equals(r.getStatus())) continue;
                roleCodes.add(r.getCode());
                List<String> ps = rolePermissionMapper.selectPermissionsByRoleId(r.getId());
                if (ps != null) perms.addAll(ps);
            }
        }
        return b.roles(roleCodes).permissions(perms).build();
    }

    public LoginResponse.UserInfo toUserInfo(LoginUser u) {
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setId(u.getUserId());
        info.setUsername(u.getUsername());
        info.setRealName(u.getRealName());
        info.setNickname(u.getRealName());
        info.setAvatar(u.getAvatar());
        info.setDeptId(u.getDeptId());
        info.setDeptName(u.getDeptName());
        info.setAdmin(u.getAdmin());
        info.setRoles(u.getRoles() == null ? List.of() : List.copyOf(u.getRoles()));
        info.setPermissions(u.getPermissions() == null ? List.of() : List.copyOf(u.getPermissions()));
        return info;
    }
}
