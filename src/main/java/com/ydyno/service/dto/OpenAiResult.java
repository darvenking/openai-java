/*
 *  Copyright 2019-2020 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ydyno.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Zheng Jie
 * @description OpenAi接口返回参数
 * @date 2023-02-15
 **/
@Getter
@Setter
@Builder
public class OpenAiResult {

    /**
     * 状态码
     */
    private Integer code;

    /** 问题 */
    private String title;

    /** 答案 */
    private String html;

    /** 图片答案 */
    private String url;
}
