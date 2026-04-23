package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.model.entity.UserTagGroup;
import com.anbuz.domain.topic.model.entity.UserTagValue;
import com.anbuz.infrastructure.persistent.dao.ITagDao;
import com.anbuz.infrastructure.persistent.dao.ITopicAutoInvalidRuleDao;
import com.anbuz.infrastructure.persistent.dao.ITopicDao;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicRepository duplicate-key scenarios")
class TopicRepositoryDuplicateKeyTest {

    @Mock
    private ITopicDao topicDao;

    @Mock
    private ITagDao tagDao;

    @Mock
    private ITopicAutoInvalidRuleDao topicAutoInvalidRuleDao;

    @Nested
    @DisplayName("duplicate key translation")
    class DuplicateKeyTranslation {

        @Test
        @DisplayName("duplicate topic name is translated to business conflict")
        void givenDuplicateTopicName_whenSaveTopic_thenThrowsBusinessConflict() {
            TopicRepository repository = repository();
            doThrow(new DuplicateKeyException("duplicate topic")).when(topicDao).insert(any());

            assertThatThrownBy(() -> repository.saveTopic(topic()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "主题名称已存在");
        }

        @Test
        @DisplayName("duplicate tag group name is translated to business conflict")
        void givenDuplicateTagGroupName_whenSaveTagGroup_thenThrowsBusinessConflict() {
            TopicRepository repository = repository();
            doThrow(new DuplicateKeyException("duplicate group")).when(tagDao).insertGroup(any());

            assertThatThrownBy(() -> repository.saveTagGroup(tagGroup()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "标签组名称已存在");
        }

        @Test
        @DisplayName("duplicate tag value is translated to business conflict")
        void givenDuplicateTagValue_whenSaveTagValue_thenThrowsBusinessConflict() {
            TopicRepository repository = repository();
            doThrow(new DuplicateKeyException("duplicate value")).when(tagDao).insertValue(any());

            assertThatThrownBy(() -> repository.saveTagValue(tagValue()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "标签值已存在");
        }

        @Test
        @DisplayName("duplicate auto invalid rule is translated to business conflict")
        void givenDuplicateRule_whenSaveAutoInvalidRule_thenThrowsBusinessConflict() {
            TopicRepository repository = repository();
            doThrow(new DuplicateKeyException("duplicate rule")).when(topicAutoInvalidRuleDao).insert(any());

            assertThatThrownBy(() -> repository.saveAutoInvalidRule(autoInvalidRule()))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "自动失效规则已存在");
        }
    }

    private TopicRepository repository() {
        return new TopicRepository(topicDao, tagDao, topicAutoInvalidRuleDao);
    }

    private Topic topic() {
        LocalDateTime now = LocalDateTime.now();
        return Topic.builder()
                .userId(1L)
                .name("后端")
                .status(1)
                .materialCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserTagGroup tagGroup() {
        LocalDateTime now = LocalDateTime.now();
        return UserTagGroup.builder()
                .topicId(10L)
                .name("阶段")
                .exclusive(true)
                .required(false)
                .sortOrder(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UserTagValue tagValue() {
        return UserTagValue.builder()
                .groupId(20L)
                .value("需求分析")
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TopicAutoInvalidRule autoInvalidRule() {
        LocalDateTime now = LocalDateTime.now();
        return TopicAutoInvalidRule.builder()
                .topicId(10L)
                .ruleType("INBOX_TIMEOUT")
                .thresholdDays(90)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
