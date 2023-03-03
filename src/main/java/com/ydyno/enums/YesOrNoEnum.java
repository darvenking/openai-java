package com.ydyno.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态举类
 *
 */
@Getter
@AllArgsConstructor
public enum YesOrNoEnum {
    NOT(0, "否"),
    YES(1, "是"),
    ;

    private final int code;
    private final String desc;

}
