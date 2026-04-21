package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.MaterialPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface IMaterialDao {

    Long insert(MaterialPO material);

    void update(MaterialPO material);

    Optional<MaterialPO> selectById(@Param("id") Long id);

    List<MaterialPO> selectByStatusAndInboxAtBefore(
            @Param("topicId") Long topicId,
            @Param("status") String status,
            @Param("threshold") LocalDateTime threshold);

    List<MaterialPO> selectByStatusAndUpdatedAtBefore(
            @Param("topicId") Long topicId,
            @Param("status") String status,
            @Param("threshold") LocalDateTime threshold);

}
