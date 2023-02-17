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
package com.ydyno.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import com.ydyno.config.MyAiConfig;
import com.ydyno.service.dto.OpenAiRequest;
import com.ydyno.service.dto.OpenAiResult;
import com.ydyno.service.MyAiService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zheng Jie
 * @description OpenAi接口实现类
 * @date 2023-02-15
 **/
@Service
@AllArgsConstructor
public class MyAiServiceImpl implements MyAiService {

    private final MyAiConfig openAiConfig;

    @Override
    public OpenAiResult query(OpenAiRequest openAiDto) {
        // 获取apikey
        String apikey = openAiDto.getApikey();
        //  获取最大返回字符数
        Integer maxTokens = openAiConfig.getMaxTokens();
        // 如果没有传入apikey，则使用配置文件中的
        if(StrUtil.isBlank(apikey)){
            apikey = openAiConfig.getApiKey();
        } else {
            // 如果传入了apikey，max_tokens不能超过模型的上下文长度。大多数模型的上下文长度为 2048 个标记
            maxTokens = 2048;
        }
        // 创建OpenAiService
        OpenAiService service = new OpenAiService(apikey, Duration.ofMinutes(2L));
        // 根据id判断调用哪个接口
        try {
            switch (openAiDto.getId()){
                // 文本问答
                case 1: return textQuiz(maxTokens, openAiDto, service);
                // 图片生成
                case 2: return imageQuiz(openAiDto, service);
                // 余额查询
                case 3: return creditQuery(apikey);
                // 默认返回
                default: return OpenAiResult.builder().title(openAiDto.getText()).html("未知的请求类型").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return OpenAiResult.builder().title(openAiDto.getText()).html("不好意思，接口请求异常，请稍后再试！").build();
        }
    }

    /**
     * 文本问答
     * @param maxTokens 最大字符数
     * @param openAiDto 请求参数
     * @param service OpenAiService
     * @return OpenAiResult
     */
    private OpenAiResult textQuiz(Integer maxTokens, OpenAiRequest openAiDto, OpenAiService service){
        List<String> stopList = null;
        // 获取本次的对话
        String text = openAiDto.getText();
        if(openAiDto.getKeep() == 1){
            // 构建连续对话参数
            stopList = ListUtil.toList("Human:","AI:");
            // 保留上一次的对话
            text = openAiDto.getKeepText();
        }
        // 构建请求参数
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(text)
                .maxTokens(maxTokens)
                .model(openAiConfig.getModel())
                .temperature(openAiConfig.getTemperature())
                .build();
        // 如果是连续对话，添加stop参数
        if(openAiDto.getKeep() == 1){
            completionRequest.setStop(stopList);
        }
        // 调用接口
        String textResult = service.createCompletion(completionRequest).getChoices().get(0).getText();
        // 去除文本前面的所有符号保留英文和汉字
        textResult = textResult.replaceAll("^[^a-zA-Z\\u4e00-\\u9fa5]*", "");
        // 返回结果
        return OpenAiResult.builder()
                .title(openAiDto.getText())
                .html(textResult)
                .build();
    }

    /**
     * 图片请求
     * @param openAiDto  请求参数
     * @param service OpenAiService
     * @return OpenAiResult
     */
    private OpenAiResult imageQuiz(OpenAiRequest openAiDto, OpenAiService service){
        // 构建请求参数
        CreateImageRequest request = CreateImageRequest.builder()
                .prompt(openAiDto.getText())
                .size("256x256")
                .build();
        // 调用接口
        String imageResult = service.createImage(request).getData().get(0).getUrl();
        // 返回结果
        return OpenAiResult.builder()
                .title(openAiDto.getText())
                .url(imageResult)
                .build();
    }

    /**
     * 余额查询
     * @param apikey apikey
     * @return OpenAiResult
     */
    private OpenAiResult creditQuery(String apikey){
        // 调用接口
        String result = HttpRequest.get(openAiConfig.getCreditApi())
                .header(Header.CONTENT_TYPE, "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apikey)
                .execute().body();
        // 解析结果
        JSONObject jsonObject = JSONUtil.parseObj(result);
        // 返回结果
        return OpenAiResult.builder()
                .html(jsonObject.getStr("total_available"))
                .build();
    }
}
