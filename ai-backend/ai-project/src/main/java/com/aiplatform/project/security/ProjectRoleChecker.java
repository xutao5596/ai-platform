package com.aiplatform.project.security;

import com.aiplatform.common.constant.CommonConstants;
import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.entity.AiProject;
import com.aiplatform.project.entity.AiProjectMember;
import com.aiplatform.project.mapper.AiProjectMemberMapper;
import com.aiplatform.project.mapper.AiProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 项目权限工具:校验当前用户对某项目的角色。
 */
@Component
@RequiredArgsConstructor
public class ProjectRoleChecker {

    private static final Map<String, Integer> LEVELS = Map.of(
            CommonConstants.PROJECT_OWNER, 4,
            CommonConstants.PROJECT_ADMIN, 3,
            CommonConstants.PROJECT_DEVELOPER, 2,
            CommonConstants.PROJECT_VIEWER, 1
    );

    private final AiProjectMapper projectMapper;
    private final AiProjectMemberMapper memberMapper;

    public AiProject requireProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目 id 不能为空");
        }
        AiProject p = projectMapper.selectById(projectId);
        if (p == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return p;
    }

    public String currentRole(Long projectId) {
        LoginUser u = UserContext.get();
        if (u == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (Boolean.TRUE.equals(u.getAdmin())) {
            return CommonConstants.PROJECT_OWNER;
        }
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PROJECT_NO_PERMISSION);
        }
        AiProjectMember m = memberMapper.selectByProjectAndUser(projectId, u.getUserId());
        if (m == null) {
            throw new BusinessException(ErrorCode.PROJECT_NO_PERMISSION);
        }
        return m.getRoleCode();
    }

    public void requireAtLeast(Long projectId, String required) {
        String current = currentRole(projectId);
        Integer cur = LEVELS.getOrDefault(current, 0);
        Integer need = LEVELS.getOrDefault(required, 1);
        if (cur < need) {
            throw new BusinessException(ErrorCode.PROJECT_NO_PERMISSION);
        }
    }
}
