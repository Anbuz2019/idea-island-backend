package com.anbuz.domain.material.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资料状态记录，负责保存一次资料状态流转的动作、状态和发生时间。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialStatusRecord {

    private String status;
    private String label;
    private LocalDateTime occurredAt;

}
