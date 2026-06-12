package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("position")
public class Position extends BaseEntity {

    private String positionCode;
    private String positionName;
    private BigDecimal unitPrice;
    private String description;
    private Integer status;
}
