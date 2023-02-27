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
package com.ydyno.config;

import cn.hutool.core.collection.ListUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Random;

/**
 * @author Zheng Jie
 * @description OpenAi配置类
 * @date 2023-02-15
 **/
@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {

    /**
     * OpenAi的key
     */
    private String keys;

    /**
     * OpenAi的model
     */
    private String model;

    /**
     * OpenAi的max_tokens
     */
    private Integer maxTokens;

    /**
     * OpenAi的temperature
     */
    private Double temperature;

    /**
     * 综合问答API
     */
    private String openaiApi;

    /**
     * 图片问答API
     */
    private String imageApi;

    /**
     * OpenAi的creditApi
     */
    private String creditApi;

    /**
     * 随机获取一个ApiKey
     * @return ApiKey
     */
    public String getApiKey() {
        // 转为 List
        List<String> keyList = ListUtil.toList(keys.split(","));
        if(keyList.size() == 1){
            return keyList.get(0);
        }
        // 打乱顺序
        Random random = new Random();
        // 获取0~list.size()之间的随机数
        int index = random.nextInt(keyList.size());
        // 获取一个 key
        return keyList.get(index);
    }
}
