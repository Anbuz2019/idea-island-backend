package com.anbuz.domain.material.model.valobj;

import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资料分页结果，负责承载领域查询后的聚合列表和分页信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialPageResult {

    private List<MaterialAggregate> items;
    private long total;
    private int page;
    private int pageSize;

}
