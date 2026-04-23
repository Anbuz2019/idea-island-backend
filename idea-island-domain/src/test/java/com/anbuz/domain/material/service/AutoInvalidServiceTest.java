package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.impl.AutoInvalidService;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.enums.MaterialStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoInvalidService 自动失效领域服务")
class AutoInvalidServiceTest {

    @Mock
    private ITopicRepository topicRepository;

    @Mock
    private IMaterialRepository materialRepository;

    @InjectMocks
    private AutoInvalidService autoInvalidService;

    @Nested
    @DisplayName("扫描过期资料")
    class InvalidateExpiredMaterials {

        @Test
        @DisplayName("INBOX 超时规则命中资料时，更新为 INVALID 并写入原因")
        void givenInboxTimeoutRuleWithMatches_whenInvalidateExpiredMaterials_thenInvalidatesMaterials() {
            TopicAutoInvalidRule rule = TopicAutoInvalidRule.builder()
                    .topicId(10L)
                    .ruleType("INBOX_TIMEOUT")
                    .thresholdDays(90)
                    .enabled(true)
                    .build();
            Material material = Material.builder()
                    .id(100L)
                    .topicId(10L)
                    .status(MaterialStatus.INBOX)
                    .build();
            when(topicRepository.findEnabledAutoInvalidRules()).thenReturn(List.of(rule));
            when(materialRepository.findByTopicIdAndStatusAndInboxAtBefore(eq(10L), eq(MaterialStatus.INBOX),
                    any(LocalDateTime.class))).thenReturn(List.of(material));

            autoInvalidService.invalidateExpiredMaterials();

            ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
            verify(materialRepository).updateMaterial(captor.capture());
            assertThat(captor.getValue())
                    .returns(MaterialStatus.INVALID, Material::getStatus)
                    .returns("系统自动失效：超过 90 天未处理", Material::getInvalidReason)
                    .satisfies(updated -> assertThat(updated.getInvalidAt()).isNotNull())
                    .satisfies(updated -> assertThat(updated.getUpdatedAt()).isNotNull());
        }

        @Test
        @DisplayName("PENDING_REVIEW 超时规则使用 updatedAt 查询，未命中时不写库")
        void givenPendingReviewTimeoutRuleWithoutMatches_whenInvalidateExpiredMaterials_thenDoesNotWrite() {
            TopicAutoInvalidRule rule = TopicAutoInvalidRule.builder()
                    .topicId(10L)
                    .ruleType("PENDING_REVIEW_TIMEOUT")
                    .thresholdDays(60)
                    .enabled(true)
                    .build();
            when(topicRepository.findEnabledAutoInvalidRules()).thenReturn(List.of(rule));
            when(materialRepository.findByTopicIdAndStatusAndUpdatedAtBefore(eq(10L),
                    eq(MaterialStatus.PENDING_REVIEW), any(LocalDateTime.class))).thenReturn(List.of());

            autoInvalidService.invalidateExpiredMaterials();

            verify(materialRepository).findByTopicIdAndStatusAndUpdatedAtBefore(eq(10L),
                    eq(MaterialStatus.PENDING_REVIEW), any(LocalDateTime.class));
            verify(materialRepository, never()).findByTopicIdAndStatusAndInboxAtBefore(any(), any(), any());
            verify(materialRepository, never()).updateMaterial(any());
        }

        @Test
        @DisplayName("未知规则类型时跳过，不影响后续扫描")
        void givenUnknownRuleType_whenInvalidateExpiredMaterials_thenSkipsRule() {
            TopicAutoInvalidRule rule = TopicAutoInvalidRule.builder()
                    .topicId(10L)
                    .ruleType("UNKNOWN")
                    .thresholdDays(1)
                    .enabled(true)
                    .build();
            when(topicRepository.findEnabledAutoInvalidRules()).thenReturn(List.of(rule));

            autoInvalidService.invalidateExpiredMaterials();

            verify(materialRepository, never()).findByTopicIdAndStatusAndInboxAtBefore(any(), any(), any());
            verify(materialRepository, never()).findByTopicIdAndStatusAndUpdatedAtBefore(any(), any(), any());
            verify(materialRepository, never()).updateMaterial(any());
        }
    }
}
