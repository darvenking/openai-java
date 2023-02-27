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
package com.ydyno.service;

import com.ydyno.service.dto.OpenAiRequest;
import com.ydyno.service.dto.OpenAiResult;

/**
 * @author Zheng Jie
 * @description OpenAi服务接口
 * @date 2023-02-15
 **/
public interface OpenAiService {

    /**
     * 查询余额
     * @param openAiDto /
     * @return /
     */
    OpenAiResult creditQuery(OpenAiRequest openAiDto);

    /**
     * 问答，绘画
     *
     * @param openAiDto       /
     * @param webSocketServer /
     */
    void communicate(OpenAiRequest openAiDto, WebSocketServer webSocketServer) throws Exception;
}
