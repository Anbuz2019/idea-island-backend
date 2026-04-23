package com.anbuz.trigger.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.anbuz.api.http.IMaterialController;
import com.anbuz.api.http.ISearchController;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.service.IMaterialService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索 HTTP 适配器，负责把搜索和收件箱请求转换为资料列表查询。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController implements ISearchController {

    private final IMaterialService materialService;

    @Override
    public Result<IMaterialController.MaterialPageResponse> search(@Valid SearchRequest request) {
        Long userId = UserContext.currentUserId();
        log.debug("Search materials userId={} topicId={} page={} pageSize={} keywordPresent={}",
                userId, request.getTopicId(), request.getPage(), request.getPageSize(), request.getKeyword() != null);
        return Result.ok(MaterialApiMapper.toSearchPageResponse(materialService.searchMaterials(userId, MaterialListQuery.builder()
                        .topicId(request.getTopicId())
                        .keyword(request.getKeyword())
                        .statuses(request.getStatus())
                        .materialTypes(request.getMaterialType())
                        .scoreMin(request.getScoreMin())
                        .scoreMax(request.getScoreMax())
                        .createdStart(request.getCreatedStart())
                        .createdEnd(request.getCreatedEnd())
                        .sortBy(request.getSortBy())
                        .sortDirection(request.getSortDirection())
                        .tagFilters(parseTagFilters(request.getTagFilters()))
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .build()),
                request.getKeyword()));
    }

    @Override
    public Result<IMaterialController.MaterialPageResponse> inbox(@Valid InboxRequest request) {
        Long userId = UserContext.currentUserId();
        log.debug("Load inbox userId={} topicId={} page={} pageSize={}",
                userId, request.getTopicId(), request.getPage(), request.getPageSize());
        return Result.ok(MaterialApiMapper.toPageResponse(
                materialService.inbox(userId, request.getTopicId(), request.getPage(), request.getPageSize())
        ));
    }

    private List<MaterialListQuery.TagFilter> parseTagFilters(String tagFilters) {
        if (tagFilters == null || tagFilters.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(tagFilters, new TypeReference<>() {});
        } catch (Exception e) {
            throw new AppException(ErrorCode.PARAM_INVALID, "tagFilters 格式非法");
        }
    }
}
