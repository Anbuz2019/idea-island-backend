package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.TopicAutoInvalidRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自动失效规则 MyBatis Mapper，负责 topic_auto_invalid_rule 表的读写。
 */
@Mapper
public interface ITopicAutoInvalidRuleDao {

    List<TopicAutoInvalidRulePO> selectEnabledRules();

    void insert(TopicAutoInvalidRulePO rule);

    void deleteByTopicId(@Param("topicId") Long topicId);

}
