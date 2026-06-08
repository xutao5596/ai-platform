package com.aiplatform.project.service;

import com.aiplatform.common.api.PageResult;
import com.aiplatform.common.constant.CommonConstants;
import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.dto.ProjectQuery;
import com.aiplatform.project.dto.ProjectSaveRequest;
import com.aiplatform.project.dto.ProjectVO;
import com.aiplatform.project.entity.AiProject;
import com.aiplatform.project.entity.AiProjectMember;
import com.aiplatform.project.mapper.AiProjectMapper;
import com.aiplatform.project.mapper.AiProjectMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiProjectService {

    private final AiProjectMapper projectMapper;
    private final AiProjectMemberMapper memberMapper;

    public PageResult<ProjectVO> pageMy(ProjectQuery q) {
        LoginUser u = UserContext.get();
        if (u == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Page<AiProject> page = new Page<>(q.getCurrent(), q.getSize());
        List<AiProject> result;
        long total;
        if (Boolean.TRUE.equals(u.getAdmin())) {
            LambdaQueryWrapper<AiProject> w = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(q.getKeyword())) {
                w.and(z -> z.like(AiProject::getName, q.getKeyword())
                        .or().like(AiProject::getCode, q.getKeyword()));
            }
            if (q.getStatus() != null) w.eq(AiProject::getStatus, q.getStatus());
            w.orderByDesc(AiProject::getCreateTime);
            Page<AiProject> pg = projectMapper.selectPage(page, w);
            result = pg.getRecords();
            total = pg.getTotal();
        } else {
            List<AiProjectMember> myMemberships = memberMapper.selectByUserId(u.getUserId());
            if (myMemberships.isEmpty()) {
                return new PageResult<>(q.getCurrent(), q.getSize(), 0, List.of());
            }
            List<Long> pids = myMemberships.stream().map(AiProjectMember::getProjectId).collect(Collectors.toList());
            LambdaQueryWrapper<AiProject> w = new LambdaQueryWrapper<>();
            w.in(AiProject::getId, pids);
            if (StringUtils.hasText(q.getKeyword())) {
                w.and(z -> z.like(AiProject::getName, q.getKeyword())
                        .or().like(AiProject::getCode, q.getKeyword()));
            }
            if (q.getStatus() != null) w.eq(AiProject::getStatus, q.getStatus());
            w.orderByDesc(AiProject::getCreateTime);
            Page<AiProject> pg = projectMapper.selectPage(page, w);
            result = pg.getRecords();
            total = pg.getTotal();
        }
        List<ProjectVO> vos = result.stream().map(p -> toVO(p, u.getUserId())).collect(Collectors.toList());
        return new PageResult<>(page.getCurrent(), page.getSize(), total, vos);
    }

    public ProjectVO get(Long id) {
        AiProject p = projectMapper.selectById(id);
        if (p == null) throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        Long uid = UserContext.getUserId();
        return toVO(p, uid);
    }

    @Transactional
    public Long create(ProjectSaveRequest req) {
        LoginUser u = UserContext.get();
        AiProject p = new AiProject();
        p.setName(req.getName());
        p.setCode(req.getCode());
        p.setDescription(req.getDescription());
        p.setIcon(req.getIcon());
        p.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        p.setOwnerId(u.getUserId());
        projectMapper.insert(p);
        AiProjectMember m = new AiProjectMember();
        m.setProjectId(p.getId());
        m.setUserId(u.getUserId());
        m.setUsername(u.getUsername());
        m.setRoleCode(CommonConstants.PROJECT_OWNER);
        m.setJoinTime(LocalDateTime.now());
        memberMapper.insert(m);
        return p.getId();
    }

    @Transactional
    public void update(ProjectSaveRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "id 不能为空");
        AiProject p = projectMapper.selectById(req.getId());
        if (p == null) throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        p.setName(req.getName());
        p.setCode(req.getCode());
        p.setDescription(req.getDescription());
        p.setIcon(req.getIcon());
        p.setStatus(req.getStatus());
        projectMapper.updateById(p);
    }

    @Transactional
    public void delete(Long id) {
        AiProject p = projectMapper.selectById(id);
        if (p == null) return;
        projectMapper.deleteById(id);
        memberMapper.deleteByProjectId(id);
    }

    public ProjectVO toVO(AiProject p, Long currentUserId) {
        ProjectVO vo = new ProjectVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setDescription(p.getDescription());
        vo.setIcon(p.getIcon());
        vo.setStatus(p.getStatus());
        vo.setOwnerId(p.getOwnerId());
        vo.setCreateTime(p.getCreateTime());
        if (currentUserId != null) {
            AiProjectMember m = memberMapper.selectByProjectAndUser(p.getId(), currentUserId);
            if (m != null) vo.setRoleCode(m.getRoleCode());
        }
        vo.setMemberCount(memberMapper.selectByProjectId(p.getId()).size());
        return vo;
    }
}
