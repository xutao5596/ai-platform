package com.aiplatform.project.mapper;

import com.aiplatform.project.entity.AiProjectMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiProjectMemberMapper extends BaseMapper<AiProjectMember> {

    @Select("SELECT * FROM ai_project_member WHERE project_id = #{projectId}")
    List<AiProjectMember> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM ai_project_member WHERE user_id = #{userId}")
    List<AiProjectMember> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM ai_project_member WHERE project_id = #{projectId} AND user_id = #{userId} LIMIT 1")
    AiProjectMember selectByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Delete("DELETE FROM ai_project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    int deleteByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Delete("DELETE FROM ai_project_member WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Long projectId);
}
