package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.impl.StatusTransitionService;
import com.anbuz.types.enums.MaterialAction;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatusTransitionService 状态机流转")
class StatusTransitionServiceTest {

    @Mock
    private IMaterialRepository materialRepository;

    @InjectMocks
    private StatusTransitionService statusTransitionService;

    private static final Long USER_ID = 1L;
    private static final Long MATERIAL_ID = 100L;

    private Material buildMaterial(MaterialStatus status) {
        return Material.builder()
                .id(MATERIAL_ID)
                .userId(USER_ID)
                .status(status)
                .inboxAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("标记已读（MARK_READ）")
    class MarkRead {

        @Test
        @DisplayName("INBOX 状态下标记已读，应流转至 PENDING_REVIEW")
        void givenInboxMaterial_whenMarkRead_thenStatusIsPendingReview() {
            Material material = buildMaterial(MaterialStatus.INBOX);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            Material result = statusTransitionService.transit(
                    MATERIAL_ID, USER_ID, MaterialAction.MARK_READ, null, null, null);

            assertThat(result.getStatus()).isEqualTo(MaterialStatus.PENDING_REVIEW);
            verify(materialRepository).updateMaterial(any());
        }

        @Test
        @DisplayName("非 INBOX 状态下标记已读，应抛出状态流转异常")
        void givenCollectedMaterial_whenMarkRead_thenThrowsInvalidTransition() {
            Material material = buildMaterial(MaterialStatus.COLLECTED);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.MARK_READ, null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION.getCode());
        }
    }

    @Nested
    @DisplayName("完成评价（COLLECT）")
    class Collect {

        @Test
        @DisplayName("INBOX 状态填写评语和评分，应直接跳转至 COLLECTED")
        void givenInboxMaterial_whenCollectWithCommentAndScore_thenStatusIsCollected() {
            Material material = buildMaterial(MaterialStatus.INBOX);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            Material result = statusTransitionService.transit(
                    MATERIAL_ID, USER_ID, MaterialAction.COLLECT, "很好的文章", new BigDecimal("8.5"), null);

            assertThat(result.getStatus()).isEqualTo(MaterialStatus.COLLECTED);
            assertThat(result.getComment()).isEqualTo("很好的文章");
            assertThat(result.getScore()).isEqualByComparingTo("8.5");
            assertThat(result.getCollectedAt()).isNotNull();
        }

        @Test
        @DisplayName("完成评价缺少评语，应抛出参数校验异常")
        void givenMissingComment_whenCollect_thenThrowsParamInvalid() {
            Material material = buildMaterial(MaterialStatus.PENDING_REVIEW);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.COLLECT, null, new BigDecimal("8.0"), null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }

        @Test
        @DisplayName("完成评价缺少评分，应抛出参数校验异常")
        void givenMissingScore_whenCollect_thenThrowsParamInvalid() {
            Material material = buildMaterial(MaterialStatus.PENDING_REVIEW);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.COLLECT, "好文章", null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }
    }

    @Nested
    @DisplayName("标记失效（INVALIDATE）")
    class Invalidate {

        @Test
        @DisplayName("任意非失效状态填写失效原因，应流转至 INVALID")
        void givenCollectedMaterial_whenInvalidate_thenStatusIsInvalid() {
            Material material = buildMaterial(MaterialStatus.COLLECTED);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            Material result = statusTransitionService.transit(
                    MATERIAL_ID, USER_ID, MaterialAction.INVALIDATE, null, null, "内容已过期");

            assertThat(result.getStatus()).isEqualTo(MaterialStatus.INVALID);
            assertThat(result.getInvalidReason()).isEqualTo("内容已过期");
            assertThat(result.getInvalidAt()).isNotNull();
        }

        @Test
        @DisplayName("标记失效未填写原因，应抛出参数校验异常")
        void givenMissingInvalidReason_whenInvalidate_thenThrowsParamInvalid() {
            Material material = buildMaterial(MaterialStatus.INBOX);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.INVALIDATE, null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }
    }

    @Nested
    @DisplayName("恢复至收件箱（RESTORE）")
    class Restore {

        @Test
        @DisplayName("INVALID 状态恢复，应流转至 INBOX 且清空失效原因")
        void givenInvalidMaterial_whenRestore_thenStatusIsInboxAndReasonCleared() {
            Material material = buildMaterial(MaterialStatus.INVALID);
            material.setInvalidReason("旧原因");
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            Material result = statusTransitionService.transit(
                    MATERIAL_ID, USER_ID, MaterialAction.RESTORE, null, null, null);

            assertThat(result.getStatus()).isEqualTo(MaterialStatus.INBOX);
            assertThat(result.getInvalidReason()).isNull();
            assertThat(result.getInboxAt()).isNotNull();
            verify(materialRepository).clearInvalidation(eq(MATERIAL_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("restore collected")
    class RestoreCollected {

        @Test
        @DisplayName("clears archivedAt after restoring an archived material back to collected")
        void givenArchivedMaterial_whenRestoreCollected_thenClearsArchivedAt() {
            Material material = buildMaterial(MaterialStatus.ARCHIVED);
            material.setArchivedAt(LocalDateTime.now().minusHours(2));
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            Material result = statusTransitionService.transit(
                    MATERIAL_ID, USER_ID, MaterialAction.RESTORE_COLLECTED, null, null, null);

            assertThat(result)
                    .returns(MaterialStatus.COLLECTED, Material::getStatus)
                    .returns(null, Material::getArchivedAt);
            verify(materialRepository).clearArchivedAt(eq(MATERIAL_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("权限校验")
    class Authorization {

        @Test
        @DisplayName("操作他人资料，应抛出无权限异常")
        void givenOtherUserMaterial_whenTransit_thenThrowsForbidden() {
            Material material = buildMaterial(MaterialStatus.INBOX);
            material.setUserId(999L);
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.of(material));

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.MARK_READ, null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("资料不存在，应抛出资源不存在异常")
        void givenNonExistentMaterial_whenTransit_thenThrowsNotFound() {
            when(materialRepository.findById(MATERIAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    statusTransitionService.transit(MATERIAL_ID, USER_ID, MaterialAction.MARK_READ, null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }
    }

}
