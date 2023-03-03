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

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ydyno.config.OpenAiConfig;
import com.ydyno.enums.YesOrNoEnum;
import com.ydyno.service.WebSocketServer;
import com.ydyno.service.dto.OpenAiRequest;
import com.ydyno.service.dto.OpenAiResult;
import com.ydyno.service.OpenAiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    //存放每个客户端对话的上下文信息。
    private final Cache<String, Set<Map<String,Object>>> contextFifoCache = CacheUtil.newFIFOCache(1000);

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
        // 如果没有传入apikey，则使用配置文件中的
        if(StrUtil.isBlank(apikey)){
            apikey = openAiConfig.getApiKey();
        }
        // 根据id判断调用哪个接口
        try {
            switch (openAiDto.getType()){
                // 文本问答
                case 1:
                    textQuiz(openAiDto, apikey, webSocketServer);
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

    @Override
    public void removeContext(String sid) {
        contextFifoCache.remove(sid);
    }

    /**
     * 文本问答
     *
     * @param openAiDto       请求参数
     * @param apikey          apikey
     * @param webSocketServer /
     */
    private void textQuiz(OpenAiRequest openAiDto, String apikey, WebSocketServer webSocketServer) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("model",openAiConfig.getModel());
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role","user");
        message.put("content",openAiDto.getText());
        messages.add(message);
        //为对话添加上下文内容
        if (openAiDto.getGoon() == YesOrNoEnum.YES.getCode()) {
            Set<Map<String,Object>> strings = contextFifoCache.get(webSocketServer.getSid());
            if (strings != null && !strings.isEmpty()){
                messages.addAll(strings);
            }
        }
        params.put("messages",messages);

        //添加上下文环境
        addContext(openAiDto,webSocketServer);

        // 调用接口
        String result;
        try {
            result = HttpRequest.post(openAiConfig.getOpenaiApi())
                    .header(Header.CONTENT_TYPE, "application/json")
                    .header(Header.AUTHORIZATION, "Bearer " + apikey)
                    .body(JSONUtil.toJsonStr(params))
                    .executeAsync().body();
        }catch (Exception e){
            e.printStackTrace();
            webSocketServer.sendMessage("请求遇到了问题，请稍后再试");
            return;
        }
        if (StrUtil.isBlank(result)) {
            // 发送信息
            webSocketServer.sendMessage("请求遇到了问题，请稍后再试");
        }

        // 发送信息
        webSocketServer.sendMessage(JSONUtil.parse(result).getByPath("choices[0].message.content",String.class));

    }

    /**
     * 添加上下文环境
     * @param openAiDto
     * @param webSocketServer
     */
    private void addContext(OpenAiRequest openAiDto, WebSocketServer webSocketServer){
        if (openAiDto.getGoon() == YesOrNoEnum.YES.getCode()){
            String sid = webSocketServer.getSid();
            Set<Map<String,Object>> list = contextFifoCache.get(sid);
            if (list == null){
                list = new HashSet<>();
            }
            Map<String, Object> message = new HashMap<>();
            message.put("role","user");
            message.put("content",openAiDto.getText());
            list.add(message);
            contextFifoCache.put(sid,list);
        }
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
