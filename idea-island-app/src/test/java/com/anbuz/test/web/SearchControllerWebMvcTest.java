package com.anbuz.test.web;

import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.trigger.http.SearchController;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import com.anbuz.types.model.ErrorCode;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController MockMvc scenarios")
class SearchControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private IMaterialService materialService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SearchController searchController = new SearchController(materialService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(testUserContextFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("returns param invalid when tagFilters is not valid json")
        void givenMalformedTagFilters_whenSearch_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/search")
                            .header(TEST_USER_HEADER, "1")
                            .param("keyword", "material")
                            .param("tagFilters", "["))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }

        @Test
        @DisplayName("returns highlight fields for matched search content")
        void givenMatchedKeyword_whenSearch_thenReturnsHighlightedFields() throws Exception {
            when(materialService.searchMaterials(org.mockito.ArgumentMatchers.eq(1L), any()))
                    .thenReturn(MaterialPageResult.builder()
                            .items(List.of(MaterialAggregate.builder().material(Material.builder()
                                    .id(100L)
                                    .userId(1L)
                                    .topicId(10L)
                                    .materialType(MaterialType.ARTICLE)
                                    .status(MaterialStatus.COLLECTED)
                                    .title("Redis notes")
                                    .rawContent("redis is fast")
                                    .comment("redis checklist")
                                    .build()).build()))
                            .total(1)
                            .page(1)
                            .pageSize(20)
                            .build());

            mockMvc.perform(get("/api/v1/search")
                            .header(TEST_USER_HEADER, "1")
                            .param("keyword", "redis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.items[0].highlight.title").value("<em>Redis</em> notes"))
                    .andExpect(jsonPath("$.data.items[0].highlight.rawContent").value("<em>redis</em> is fast"))
                    .andExpect(jsonPath("$.data.items[0].highlight.comment").value("<em>redis</em> checklist"));
        }
    }

    @Nested
    @DisplayName("inbox")
    class Inbox {

        @Test
        @DisplayName("returns param invalid when page is less than one")
        void givenPageZero_whenInbox_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/inbox")
                            .header(TEST_USER_HEADER, "1")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(materialService);
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request.getParameter(TEST_USER_HEADER);
            if (userId == null && request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest) {
                userId = httpServletRequest.getHeader(TEST_USER_HEADER);
            }

            if (userId != null && !userId.isBlank()) {
                UserContext.set(Long.parseLong(userId));
            }

            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
        };
    }
}
