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
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ydyno.config.OpenAiConfig;
import com.ydyno.service.WebSocketServer;
import com.ydyno.service.dto.OpenAiRequest;
import com.ydyno.service.dto.OpenAiResult;
import com.ydyno.service.OpenAiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Zheng Jie
 * @description OpenAi接口实现类
 * @date 2023-02-15
 **/
@Slf4j
@Service
@AllArgsConstructor
public class OpenAiServiceImpl implements OpenAiService {

    private final OpenAiConfig openAiConfig;

    @Override
    public OpenAiResult creditQuery(OpenAiRequest openAiDto) {
        // 获取apikey
        String apikey = openAiDto.getApikey();
        if(StrUtil.isBlank(apikey)){
            apikey = openAiConfig.getApiKey();
        }
        try {
            // 调用接口
            String result = HttpRequest.get(openAiConfig.getCreditApi())
                    .header(Header.CONTENT_TYPE, "application/json")
                    .header(Header.AUTHORIZATION, "Bearer " + apikey)
                    .execute().body();
            // 判断是否请求出错
            if(result.contains("server_error")){
                throw new RuntimeException("请求ChatGPT官方服务器出错");
            }
            // 解析结果
            JSONObject jsonObject = JSONUtil.parseObj(result);
            // 返回结果
            return OpenAiResult.builder()
                    .code(200)
                    .html(jsonObject.getStr("total_available"))
                    .build();
        } catch (Exception e){
            return OpenAiResult.builder().code(400).title(openAiDto.getText()).html("请求ChatGPT服务异常，请稍后再试！").build();
        }
    }

    @Override
    public void communicate(OpenAiRequest openAiDto, WebSocketServer webSocketServer) throws Exception {
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
        // 根据id判断调用哪个接口
        try {
            switch (openAiDto.getId()){
                // 文本问答
                case 1:
                    textQuiz(maxTokens, openAiDto, apikey, webSocketServer);
                    break;
                // 图片生成
                case 2:
                    imageQuiz(openAiDto, apikey, webSocketServer);
                    break;
                // 默认
                default:
                    webSocketServer.sendMessage("未知的请求类型");
            }
        } catch (Exception e){
            e.printStackTrace();
            webSocketServer.sendMessage("请求ChatGPT服务异常，请稍后再试！");
        }
    }

    /**
     * 文本问答
     *
     * @param maxTokens       最大字符数
     * @param openAiDto       请求参数
     * @param apikey          apikey
     * @param webSocketServer /
     */
    private void textQuiz(Integer maxTokens, OpenAiRequest openAiDto, String apikey, WebSocketServer webSocketServer) throws Exception {
        List<String> stopList = null;
        // 获取本次的对话
        String text = openAiDto.getText();
        if(openAiDto.getKeep() == 1){
            // 构建连续对话参数
            stopList = ListUtil.toList("Human:","AI:");
            // 保留上一次的对话
            text = openAiDto.getKeepText();
        }

        Map<String, Object> params = MapUtil.ofEntries(
                MapUtil.entry("prompt", text),
                MapUtil.entry("max_tokens", maxTokens),
                MapUtil.entry("stream", true),
                MapUtil.entry("logprobs", 0),
                MapUtil.entry("model", openAiConfig.getModel()),
                MapUtil.entry("temperature", openAiConfig.getTemperature())
        );

        // 如果是连续对话，添加stop参数
        if(openAiDto.getKeep() == 1){
            params.put("stop", stopList);
        }

        // 调用接口
        HttpResponse result;
        try {
            result = HttpRequest.post(openAiConfig.getOpenaiApi())
                    .header(Header.CONTENT_TYPE, "application/json")
                    .header(Header.AUTHORIZATION, "Bearer " + apikey)
                    .body(JSONUtil.toJsonStr(params))
                    .executeAsync();
        }catch (Exception e){
            e.printStackTrace();
            webSocketServer.sendMessage("请求遇到了问题，请稍后再试");
            return;
        }

        // 处理数据
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(result.bodyStream()));
        boolean flag = false;
        while((line = reader.readLine()) != null){
            Pattern p = Pattern.compile("\"text\": \"(.*?)\"");
            Matcher m = p.matcher(line);
            if(m.find()) {

                // 过滤开头多余\n
                if(!"\\n".equals(m.group(1)) && !flag) {
                    flag = true;
                }

                // 将\n和\t替换为html中的换行和制表
                String data = UnicodeUtil.toString(m.group(1)).replace("\\n", "\n")
                        .replace("\\t", "\t");

                // 发送信息
                if(flag) {
                    webSocketServer.sendMessage(data);
                }
            }
        }
        reader.close();
    }

    /**
     * 图片请求
     *
     * @param openAiDto       请求参数
     * @param apikey          apiKey
     * @param webSocketServer /
     */
    private void imageQuiz(OpenAiRequest openAiDto, String apikey, WebSocketServer webSocketServer) throws IOException {
        // 请求参数
        Map<String, Object> params = MapUtil.ofEntries(
                MapUtil.entry("prompt", openAiDto.getText()),
                MapUtil.entry("size", "256x256")
        );
        // 调用接口
        String result = HttpRequest.post(openAiConfig.getImageApi())
                .header(Header.CONTENT_TYPE, "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apikey)
                .body(JSONUtil.toJsonStr(params))
                .execute().body();
        // 正则匹配出结果
        Pattern p = Pattern.compile("\"url\": \"(.*?)\"");
        Matcher m = p.matcher(result);
        if (m.find()){
            webSocketServer.sendMessage(m.group(1));
        } else {
            webSocketServer.sendMessage("图片生成失败！");
        }
    }
}
