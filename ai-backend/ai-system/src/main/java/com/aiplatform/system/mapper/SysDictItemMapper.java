package com.aiplatform.system.mapper;

import com.aiplatform.system.entity.SysDictItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    @Select("SELECT * FROM sys_dict_item WHERE type_code = #{typeCode} AND deleted = 0 ORDER BY sort_order")
    List<SysDictItem> selectByTypeCode(@Param("typeCode") String typeCode);

    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_dict_item WHERE type_code = #{typeCode}")
    int deleteByTypeCode(@Param("typeCode") String typeCode);
}
