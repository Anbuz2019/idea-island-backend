package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.UserTagGroupPO;
import com.anbuz.infrastructure.persistent.po.UserTagValuePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 标签 MyBatis Mapper，负责主题标签组和值的持久化读写。
 */
@Mapper
public interface ITagDao {

    void insertGroup(UserTagGroupPO group);

    void updateGroup(UserTagGroupPO group);

    void deleteGroupById(@Param("id") Long id);

    Optional<UserTagGroupPO> selectGroupById(@Param("id") Long id);

    List<UserTagGroupPO> selectGroupsByTopicId(@Param("topicId") Long topicId);

    int countGroupsByTopicId(@Param("topicId") Long topicId);

    int countGroupByTopicIdAndName(@Param("topicId") Long topicId, @Param("name") String name);

    void insertValue(UserTagValuePO value);

    void updateValue(UserTagValuePO value);

    void deleteValueById(@Param("id") Long id);

    Optional<UserTagValuePO> selectValueById(@Param("id") Long id);

    List<UserTagValuePO> selectValuesByGroupId(@Param("groupId") Long groupId);

    int countValuesByGroupId(@Param("groupId") Long groupId);

    int countValueByGroupIdAndValue(@Param("groupId") Long groupId, @Param("value") String value);

    long countMaterialReferencesByGroupKey(@Param("groupKey") String groupKey);

    long countMaterialReferencesByGroupKeyAndValue(@Param("groupKey") String groupKey, @Param("value") String value);

    int countMaterialsWithMultipleValuesInGroup(@Param("groupKey") String groupKey);

    void updateMaterialTagValue(@Param("groupKey") String groupKey,
                                @Param("oldValue") String oldValue,
                                @Param("newValue") String newValue);

    List<Long> selectRequiredGroupIdsByTopicId(@Param("topicId") Long topicId);

    void deleteValuesByGroupId(@Param("groupId") Long groupId);

}
