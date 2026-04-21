package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.TopicPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ITopicDao {

    void insert(TopicPO topic);

    void update(TopicPO topic);

    void deleteById(@Param("id") Long id);

    Optional<TopicPO> selectById(@Param("id") Long id);

    List<TopicPO> selectByUserId(@Param("userId") Long userId);

    int countByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

}
