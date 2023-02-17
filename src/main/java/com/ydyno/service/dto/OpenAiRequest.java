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

import lombok.Data;

/**
 * @author Zheng Jie
 * @description OpenAi接口请求参数
 * @date 2023-02-15
 **/
@Data
public class OpenAiRequest {

    /**
     * 请求类型：1文本，2图片，3余额
     */
    private Integer id;

    /**
     * 连续对话
     */
    private Integer keep;

    /**
     * 问题
     */
    private String text;

    /**
     * 连续对话的问题
     */
    private String keepText;

    /**
     * apiKey
     */
    private String apikey;
}
