package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.infrastructure.persistent.dao.IMaterialDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialMetaDao;
import com.anbuz.infrastructure.persistent.dao.IMaterialTagDao;
import com.anbuz.types.enums.TagType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialRepository duplicate-key scenarios")
class MaterialRepositoryDuplicateKeyTest {

    @Mock
    private IMaterialDao materialDao;

    @Mock
    private IMaterialMetaDao materialMetaDao;

    @Mock
    private IMaterialTagDao materialTagDao;

    @Nested
    @DisplayName("save tags")
    class SaveTags {

        @Test
        @DisplayName("translates duplicate tag rows into business conflict")
        void givenDuplicateTagRows_whenSaveTags_thenThrowsBusinessConflict() {
            MaterialRepository materialRepository = new MaterialRepository(materialDao, materialMetaDao, materialTagDao);
            doThrow(new DuplicateKeyException("duplicate material tags")).when(materialTagDao).insertBatch(anyList());

            assertThatThrownBy(() -> materialRepository.saveTags(List.of(buildTag())))
                    .isInstanceOf(AppException.class)
                    .extracting("code", "message")
                    .containsExactly(ErrorCode.BUSINESS_CONFLICT.getCode(), "duplicate material tags");
        }
    }

    private MaterialTag buildTag() {
        return MaterialTag.builder()
                .materialId(1L)
                .tagType(TagType.USER)
                .tagGroupKey("1")
                .tagValue("analysis")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
