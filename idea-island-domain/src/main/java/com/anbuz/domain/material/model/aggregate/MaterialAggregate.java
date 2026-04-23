package com.anbuz.domain.material.model.aggregate;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialStatusRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资料聚合，负责承载资料主体、元信息、标签和状态历史的完整读模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialAggregate {

    private Material material;
    private MaterialMeta meta;
    private List<MaterialTag> tags;
    private List<MaterialStatusRecord> statusHistory;

}
