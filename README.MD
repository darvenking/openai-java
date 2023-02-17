## OpenAi-Java

一个基于Spring boot 2.6.4，thymeleaf 的 Openai API 的简单包装项目

前端界面基于Hostloc论坛流出的源码修改而来，目前支持的功能如下

- 支持余额查询
- 支持随机获取Key，支持自定义Key
- 支持对话与生成图片，连续对话

**注意**：Openai API 并未开放连续对话的API，因此连续对话功能受上下文Tokens长度影响只能进行简单的对话

### 项目截图

![](https://s2.loli.net/2023/02/17/o93NLaA2d5YwClJ.png)

### Docker一键运行项目

docker镜像地址与教程：https://hub.docker.com/r/dqjdda/openai-java

```bash
# 替换 KEYS 即可
# 参数教程见：https://hub.docker.com/r/dqjdda/openai-java
docker run -d \
--name openai --restart always \
-p 18080:18080 \
-e MAX_TOKENS=300 \
-e TEMPERATURE=0.6 \
-e KEYS=sk-xxxx,sk-xxxx \
dqjdda/openai-java
```

### 运行项目

1. 修改 application.yml 中的 apiKey
2. 运行 OpenAiRun 类
3. 访问 http://localhost:18080

### 部署项目

1. 打包为 war 包后，上传到服务器
2. 运行项目 nohup java -jar openai-1.0.0.war > nohup.out 2>&1 &
3. 访问 http://IP:18080

### nginx 反代
```bash
server
{
    listen 80;
    server_name yourdomain;

    location / {
        proxy_read_timeout 120; 
        proxy_pass http://127.0.0.1:18080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```