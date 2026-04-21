package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.MaterialTagPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IMaterialTagDao {

    void insertBatch(@Param("tags") List<MaterialTagPO> tags);

    void deleteByMaterialId(@Param("materialId") Long materialId);

    void deleteByMaterialIdAndTagType(@Param("materialId") Long materialId, @Param("tagType") String tagType);

    void deleteByMaterialIdAndGroupKey(@Param("materialId") Long materialId, @Param("tagGroupKey") String tagGroupKey);

    List<MaterialTagPO> selectByMaterialId(@Param("materialId") Long materialId);

    List<MaterialTagPO> selectByMaterialIdAndTagType(@Param("materialId") Long materialId, @Param("tagType") String tagType);

}
