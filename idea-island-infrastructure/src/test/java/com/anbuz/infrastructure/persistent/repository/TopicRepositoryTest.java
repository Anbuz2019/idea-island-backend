package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.infrastructure.persistent.dao.ITagDao;
import com.anbuz.infrastructure.persistent.dao.ITopicAutoInvalidRuleDao;
import com.anbuz.infrastructure.persistent.dao.ITopicDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TopicRepository.class)
@DisplayName("TopicRepository H2 integration tests")
class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @SuppressWarnings("unused")
    private ITopicDao topicDao;

    @Autowired
    @SuppressWarnings("unused")
    private ITagDao tagDao;

    @Autowired
    @SuppressWarnings("unused")
    private ITopicAutoInvalidRuleDao topicAutoInvalidRuleDao;

    @Nested
    @DisplayName("topic")
    class TopicPersistence {

        @Test
        @DisplayName("saves, finds, updates, lists and deletes topics")
        void givenTopic_whenPersistTopic_thenSupportsTopicLifecycle() {
            Topic topic = buildTopic(1L, "后端");

            topicRepository.saveTopic(topic);

            assertThat(topic.getId()).isNotNull();
            assertThat(topicRepository.findTopicById(topic.getId()))
                    .hasValueSatisfying(saved -> assertThat(saved)
                            .returns("后端", Topic::getName)
                            .returns(1, Topic::getStatus));
            assertThat(topicRepository.existsByUserIdAndName(1L, "后端")).isTrue();
            assertThat(topicRepository.countTopicsByUserId(1L)).isEqualTo(1L);
            assertThat(topicRepository.findTopicsByUserId(1L))
                    .extracting(Topic::getName)
                    .contains("后端");

            topic.setDescription("updated");
            topic.setStatus(0);
            topic.setUpdatedAt(LocalDateTime.now());
            topicRepository.updateTopic(topic);

            assertThat(topicRepository.findTopicById(topic.getId()))
                    .hasValueSatisfying(updated -> assertThat(updated)
                            .returns("updated", Topic::getDescription)
                            .returns(0, Topic::getStatus));

            topicRepository.deleteTopic(topic.getId());
            assertThat(topicRepository.findTopicById(topic.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("tag set")
    class TagSetPersistence {

        @Test
        @DisplayName("saves, lists, updates and deletes tag groups and values")
        void givenTagGroupAndValue_whenPersistTagSet_thenSupportsTagSetLifecycle() {
            Topic topic = savedTopic(2L, "产品");
            UserTagGroup group = buildGroup(topic.getId(), "阶段");
            topicRepository.saveTagGroup(group);
            UserTagValue value = buildValue(group.getId(), "需求分析");
            topicRepository.saveTagValue(value);

            assertThat(group.getId()).isNotNull();
            assertThat(value.getId()).isNotNull();
            assertThat(topicRepository.findTagGroupsByTopicId(topic.getId()))
                    .extracting(UserTagGroup::getName)
                    .containsExactly("阶段");
            assertThat(topicRepository.findTagValuesByGroupId(group.getId()))
                    .extracting(UserTagValue::getValue)
                    .containsExactly("需求分析");
            assertThat(topicRepository.existsTagGroupByTopicIdAndName(topic.getId(), "阶段")).isTrue();
            assertThat(topicRepository.existsTagValueByGroupIdAndValue(group.getId(), "需求分析")).isTrue();
            assertThat(topicRepository.findRequiredTagGroupIdsByTopicId(topic.getId())).containsExactly(group.getId());

            group.setName("流程");
            group.setRequired(false);
            group.setUpdatedAt(LocalDateTime.now());
            topicRepository.updateTagGroup(group);
            value.setValue("方案设计");
            topicRepository.updateTagValue(value);

            assertThat(topicRepository.findTagGroupById(group.getId()))
                    .hasValueSatisfying(updated -> assertThat(updated)
                            .returns("流程", UserTagGroup::getName)
                            .returns(false, UserTagGroup::getRequired));
            assertThat(topicRepository.findTagValueById(value.getId()))
                    .hasValueSatisfying(updated -> assertThat(updated)
                            .returns("方案设计", UserTagValue::getValue));

            topicRepository.deleteTagValue(value.getId());
            topicRepository.deleteTagGroup(group.getId());
            assertThat(topicRepository.findTagValueById(value.getId())).isEmpty();
            assertThat(topicRepository.findTagGroupById(group.getId())).isEmpty();
        }

        @Test
        @DisplayName("counts and updates material tag references by tag group key and value")
        void givenMaterialTags_whenQueryReferences_thenReturnsReferenceCountsAndCascadeUpdatesValue() {
            Topic topic = savedTopic(3L, "运营");
            UserTagGroup group = buildGroup(topic.getId(), "阶段");
            topicRepository.saveTagGroup(group);
            String groupKey = String.valueOf(group.getId());
            jdbcTemplate.update("""
                    INSERT INTO material_tag(material_id, tag_type, tag_group_key, tag_value, created_at)
                    VALUES (?, 'user', ?, ?, ?), (?, 'user', ?, ?, ?)
                    """, 100L, groupKey, "需求分析", LocalDateTime.now(),
                    100L, groupKey, "方案设计", LocalDateTime.now());

            assertThat(topicRepository.countMaterialReferencesByGroupId(group.getId())).isEqualTo(2L);
            assertThat(topicRepository.countMaterialReferencesByValue(group.getId(), "需求分析")).isEqualTo(1L);
            assertThat(topicRepository.existsMultiValueUsageInGroup(group.getId())).isTrue();

            topicRepository.updateMaterialTagValue(group.getId(), "需求分析", "调研");

            Long updatedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1)
                    FROM material_tag
                    WHERE tag_group_key = ? AND tag_value = ?
                    """, Long.class, groupKey, "调研");
            assertThat(updatedCount).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("auto invalid rule")
    class AutoInvalidRulePersistence {

        @Test
        @DisplayName("saves, lists and deletes enabled auto invalid rules")
        void givenAutoInvalidRules_whenPersistRules_thenFindsEnabledRulesOnly() {
            Topic topic = savedTopic(4L, "投资");
            TopicAutoInvalidRule rule = TopicAutoInvalidRule.builder()
                    .topicId(topic.getId())
                    .ruleType("INBOX_TIMEOUT")
                    .thresholdDays(90)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            topicRepository.saveAutoInvalidRule(rule);

            assertThat(topicRepository.findEnabledAutoInvalidRules())
                    .anySatisfy(saved -> assertThat(saved)
                            .returns(topic.getId(), TopicAutoInvalidRule::getTopicId)
                            .returns("INBOX_TIMEOUT", TopicAutoInvalidRule::getRuleType)
                            .returns(90, TopicAutoInvalidRule::getThresholdDays)
                            .returns(true, TopicAutoInvalidRule::getEnabled));

            topicRepository.deleteAutoInvalidRulesByTopicId(topic.getId());
            assertThat(topicRepository.findEnabledAutoInvalidRules())
                    .noneSatisfy(saved -> assertThat(saved.getTopicId()).isEqualTo(topic.getId()));
        }
    }

    private Topic savedTopic(Long userId, String name) {
        Topic topic = buildTopic(userId, name);
        topicRepository.saveTopic(topic);
        return topic;
    }

    private Topic buildTopic(Long userId, String name) {
        LocalDateTime now = LocalDateTime.now();
        return Topic.builder()
                .userId(userId)
                .name(name)
                .description(name + "资料")
                .status(1)
                .materialCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserTagGroup buildGroup(Long topicId, String name) {
        LocalDateTime now = LocalDateTime.now();
        return UserTagGroup.builder()
                .topicId(topicId)
                .name(name)
                .color("#FFAA00")
                .exclusive(true)
                .required(true)
                .sortOrder(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserTagValue buildValue(Long groupId, String value) {
        return UserTagValue.builder()
                .groupId(groupId)
                .value(value)
                .color("#00AAFF")
                .sortOrder(1)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
