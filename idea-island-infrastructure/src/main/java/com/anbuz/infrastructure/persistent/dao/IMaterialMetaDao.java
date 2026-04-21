package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.MaterialMetaPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface IMaterialMetaDao {

    void insert(MaterialMetaPO meta);

    void update(MaterialMetaPO meta);

    Optional<MaterialMetaPO> selectByMaterialId(@Param("materialId") Long materialId);

}
