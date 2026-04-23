package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.MaterialPO;
import com.anbuz.infrastructure.persistent.po.StatCountPO;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 资料 MyBatis Mapper，负责 material 表的基础读写和列表查询。
 */
@Mapper
public interface IMaterialDao {

    Long insert(MaterialPO material);

    void update(MaterialPO material);

    void clearInvalidation(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    void clearArchivedAt(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

    Optional<MaterialPO> selectById(@Param("id") Long id);

    List<MaterialPO> selectByQuery(MaterialListQuery query);

    long countByQuery(MaterialListQuery query);

    List<StatCountPO> countByStatus(@Param("userId") Long userId, @Param("topicId") Long topicId);

    List<StatCountPO> countByMaterialType(@Param("topicId") Long topicId);

    BigDecimal averageScoreByTopicId(@Param("topicId") Long topicId);

    void updateLastRetrievedAt(@Param("ids") List<Long> ids, @Param("retrievedAt") LocalDateTime retrievedAt);

    List<MaterialPO> selectByStatusAndInboxAtBefore(
            @Param("topicId") Long topicId,
            @Param("status") String status,
            @Param("threshold") LocalDateTime threshold);

    List<MaterialPO> selectByStatusAndUpdatedAtBefore(
            @Param("topicId") Long topicId,
            @Param("status") String status,
            @Param("threshold") LocalDateTime threshold);

}
