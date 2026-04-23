package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.MaterialMetaPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 资料元信息 MyBatis Mapper，负责 material_meta 表的读写。
 */
@Mapper
public interface IMaterialMetaDao {

    void insert(MaterialMetaPO meta);

    void update(MaterialMetaPO meta);

    Optional<MaterialMetaPO> selectByMaterialId(@Param("materialId") Long materialId);

}
