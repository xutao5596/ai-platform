package com.aiplatform.project.service;

import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.dto.MemberAddRequest;
import com.aiplatform.project.entity.AiProject;
import com.aiplatform.project.entity.AiProjectMember;
import com.aiplatform.project.mapper.AiProjectMapper;
import com.aiplatform.project.mapper.AiProjectMemberMapper;
import com.aiplatform.system.entity.SysUser;
import com.aiplatform.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final AiProjectMemberMapper memberMapper;
    private final AiProjectMapper projectMapper;
    private final SysUserMapper userMapper;

    public List<Map<String, Object>> listMembers(Long projectId) {
        List<AiProjectMember> members = memberMapper.selectByProjectId(projectId);
        if (members.isEmpty()) return List.of();
        List<Long> uids = members.stream().map(AiProjectMember::getUserId).toList();
        List<SysUser> users = userMapper.selectBatchIds(uids);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        return members.stream().map(m -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", m.getId());
            vo.put("userId", m.getUserId());
            vo.put("username", m.getUsername());
            vo.put("realName", userMap.get(m.getUserId()) == null ? null : userMap.get(m.getUserId()).getRealName());
            vo.put("avatar", userMap.get(m.getUserId()) == null ? null : userMap.get(m.getUserId()).getAvatar());
            vo.put("roleCode", m.getRoleCode());
            vo.put("joinTime", m.getJoinTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Long addMember(Long projectId, MemberAddRequest req) {
        AiProject p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        Long userId = req.getUserId();
        if (userId == null && req.getUsername() != null) {
            SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername()).last("LIMIT 1"));
            if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            userId = u.getId();
        }
        if (userId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "userId 或 username 至少一个必填");
        AiProjectMember exist = memberMapper.selectByProjectAndUser(projectId, userId);
        if (exist != null) throw new BusinessException(ErrorCode.PROJECT_MEMBER_EXISTS);
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        AiProjectMember m = new AiProjectMember();
        m.setProjectId(projectId);
        m.setUserId(userId);
        m.setUsername(u.getUsername());
        m.setRoleCode(req.getRoleCode());
        m.setJoinTime(LocalDateTime.now());
        memberMapper.insert(m);
        return m.getId();
    }

    @Transactional
    public void updateRole(Long projectId, Long userId, String roleCode) {
        AiProjectMember m = memberMapper.selectByProjectAndUser(projectId, userId);
        if (m == null) throw new BusinessException(ErrorCode.PROJECT_NO_PERMISSION);
        m.setRoleCode(roleCode);
        memberMapper.updateById(m);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        AiProject p = projectMapper.selectById(projectId);
        if (p != null && p.getOwnerId() != null && p.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能移除项目所有者");
        }
        memberMapper.deleteByProjectAndUser(projectId, userId);
    }

    public List<Map<String, Object>> myProjects() {
        Long uid = UserContext.getUserId();
        if (uid == null) return List.of();
        List<AiProjectMember> my = memberMapper.selectByUserId(uid);
        if (my.isEmpty()) return List.of();
        List<Long> pids = my.stream().map(AiProjectMember::getProjectId).toList();
        List<AiProject> projects = projectMapper.selectBatchIds(pids);
        return projects.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("code", p.getCode());
            m.put("description", p.getDescription());
            m.put("icon", p.getIcon());
            m.put("status", p.getStatus());
            AiProjectMember mem = my.stream().filter(x -> x.getProjectId().equals(p.getId())).findFirst().orElse(null);
            m.put("roleCode", mem == null ? null : mem.getRoleCode());
            return m;
        }).collect(Collectors.toList());
    }
}
