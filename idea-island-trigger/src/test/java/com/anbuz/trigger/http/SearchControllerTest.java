package com.anbuz.trigger.http;

import com.anbuz.api.http.IMaterialController;
import com.anbuz.api.http.ISearchController;
import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController scenarios")
class SearchControllerTest {

    @Mock
    private IMaterialService materialService;

    @InjectMocks
    private SearchController searchController;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("maps the request into a material search query")
        void givenValidRequest_whenSearch_thenMapsQueryAndReturnsPage() {
            ISearchController.SearchRequest request = new ISearchController.SearchRequest();
            request.setKeyword("redis");
            request.setTopicId(10L);
            request.setStatus(List.of("COLLECTED"));
            request.setMaterialType(List.of("article"));
            request.setSortBy("createdAt");
            request.setSortDirection("DESC");
            request.setTagFilters("""
                    [{"tagGroupKey":"11","tagValues":["backend"]}]
                    """);
            request.setPage(3);
            request.setPageSize(15);

            when(materialService.searchMaterials(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(MaterialPageResult.builder()
                            .items(List.of(MaterialAggregate.builder().material(buildMaterial()).build()))
                            .total(1)
                            .page(3)
                            .pageSize(15)
                            .build());

            UserContext.set(2L);
            try {
                Result<IMaterialController.MaterialPageResponse> result = searchController.search(request);

                ArgumentCaptor<MaterialListQuery> captor = ArgumentCaptor.forClass(MaterialListQuery.class);
                verify(materialService).searchMaterials(org.mockito.ArgumentMatchers.eq(2L), captor.capture());

                assertThat(captor.getValue())
                        .returns(10L, MaterialListQuery::getTopicId)
                        .returns("redis", MaterialListQuery::getKeyword)
                        .returns("createdAt", MaterialListQuery::getSortBy)
                        .returns("DESC", MaterialListQuery::getSortDirection)
                        .returns(3, MaterialListQuery::getPage)
                        .returns(15, MaterialListQuery::getPageSize);
                assertThat(captor.getValue().getTagFilters())
                        .singleElement()
                        .satisfies(tagFilter -> assertThat(tagFilter)
                                .returns("11", MaterialListQuery.TagFilter::getTagGroupKey)
                                .extracting(MaterialListQuery.TagFilter::getTagValues)
                                .asList()
                                .containsExactly("backend"));
                assertThat(result)
                        .returns(0, Result::getCode)
                        .extracting(Result::getData)
                        .extracting(IMaterialController.MaterialPageResponse::total,
                                IMaterialController.MaterialPageResponse::page,
                                IMaterialController.MaterialPageResponse::pageSize)
                        .containsExactly(1L, 3, 15);
                assertThat(result.getData().items())
                        .singleElement()
                        .extracting(IMaterialController.MaterialDetailResponse::highlight)
                        .satisfies(highlight -> assertThat(highlight)
                                .returns("<em>Redis</em> notes", IMaterialController.SearchHighlightResponse::title)
                                .returns("how to use <em>redis</em> safely", IMaterialController.SearchHighlightResponse::rawContent)
                                .returns("great <em>redis</em> patterns", IMaterialController.SearchHighlightResponse::comment));
            } finally {
                UserContext.clear();
            }
        }

        @Test
        @DisplayName("throws param invalid when tagFilters is malformed")
        void givenMalformedTagFilters_whenSearch_thenThrowsParamInvalid() {
            ISearchController.SearchRequest request = new ISearchController.SearchRequest();
            request.setKeyword("redis");
            request.setTagFilters("[");

            UserContext.set(2L);
            try {
                assertThatThrownBy(() -> searchController.search(request))
                        .isInstanceOf(AppException.class)
                        .extracting("code")
                        .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("inbox")
    class Inbox {

        @Test
        @DisplayName("delegates topic id and pagination to the domain service")
        void givenInboxRequest_whenInbox_thenDelegatesWithPagination() {
            ISearchController.InboxRequest request = new ISearchController.InboxRequest();
            request.setTopicId(10L);
            request.setPage(2);
            request.setPageSize(25);

            when(materialService.inbox(2L, 10L, 2, 25))
                    .thenReturn(MaterialPageResult.builder()
                            .items(List.of(MaterialAggregate.builder().material(buildMaterial()).build()))
                            .total(1)
                            .page(2)
                            .pageSize(25)
                            .build());

            UserContext.set(2L);
            try {
                Result<IMaterialController.MaterialPageResponse> result = searchController.inbox(request);

                verify(materialService).inbox(2L, 10L, 2, 25);
                assertThat(result)
                        .returns(0, Result::getCode)
                        .extracting(Result::getData)
                        .extracting(IMaterialController.MaterialPageResponse::total,
                                IMaterialController.MaterialPageResponse::page,
                                IMaterialController.MaterialPageResponse::pageSize)
                        .containsExactly(1L, 2, 25);
            } finally {
                UserContext.clear();
            }
        }
    }

    private Material buildMaterial() {
        return Material.builder()
                .id(100L)
                .userId(2L)
                .topicId(10L)
                .materialType(MaterialType.ARTICLE)
                .status(MaterialStatus.COLLECTED)
                .title("Redis notes")
                .rawContent("how to use redis safely")
                .comment("great redis patterns")
                .build();
    }
}
