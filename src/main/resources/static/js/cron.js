let keepText = "";
let tempKeepText = "";
const apiUrl = "/api/openai";
const uuid = crypto.randomUUID();
let number = new Date().getSeconds();

// 建立WS连接
var path= window.location.protocol+'//' + window.location.host
socket = new WebSocket((path + "/api/ws/" + uuid).replace("http", "ws")
    .replace("https", "wss"));

//打开事件
socket.onopen = function () {
    console.log("Socket连接已建立，正在等待数据...");
};

//关闭事件
socket.onclose = function () {
    toast({ time: 5000, msg: "Socket连接关闭了，请尝试刷新页面重新连接" });
};

//发生了错误事件
socket.onerror = function () {
    toast({ time: 5000, msg: "Socket连接发生了错误，请尝试刷新页面" });
}

const apikeyInput = $("#apikey");
const kwTarget = $("#kw-target");
const keepVal = $("#keep");
const idVal = $("#id");

// 读取localStorage内的数据
let apikey = localStorage.getItem("apikey");

// apikey 如果不为空，那么查询余额
if (apikey != null) {
    apikeyInput.val(apikey);
    keyclick();
}

// 监听输入问题回车事件
kwTarget.keydown(function (e) {
    // 当 keyCode 是13时,是回车操作
    if (e.keyCode === 13) {
        aiClick();
    }
});

// 监听Key回车事件
apikeyInput.keydown(function (e) {
    // 当 keyCode 是13时,是回车操作
    if (e.keyCode === 13) {
        // 查询余额
        keyclick();
    }
});

// 查询余额方法
function keyclick() {
    apikey = apikeyInput.val();
    const currentBalance = $("#currentBalance");
    // 将输入的apikey存入localStorage
    localStorage.setItem("apikey", apikey);
    if (!apikey) {
        currentBalance.html("");
        return;
    }
    $.ajax({
        url: apiUrl,
        type: "post",
        data: JSON.stringify({ id: 3, apikey: apikey }),
        contentType: "application/json",
        dataType: "json",
        success: function (res) {
            if (res.code > 200){
                toast({ time: 3000, msg: res.html });
            } else {
                toast({ time: 2000, msg: "当前 APIKey 余额已刷新" });
                currentBalance.html(
                    "&nbsp;&nbsp当前余额：$" + parseFloat(res.html).toFixed(2)
                );
            }
            kwTarget.val("");
        }
    });
}

// 点击事件
function aiClick() {
    const title = kwTarget.val()
    if (!title) {
        return toast({ time: 2000, msg: "来问点什么吧" });
    }
    createArticle(title);
}

// 请求AI回复方法
function createArticle(title) {
    apikey = apikeyInput.val();

    $("#article").removeClass("created");

    const id = idVal.val()

    if(keepVal.val() === "1"){
        keepText = tempKeepText + keepText;
        tempKeepText = "Human:" + title + " AI:"
    }

    const data = JSON.stringify({
        text: title, id: id, apikey: apikey, keep: keepVal.val(), keepText: title + (keepText ? "\n" + keepText : ""),
    })

    const articleWrapper = $("#article-wrapper");

    articleWrapper.append(
        '<li class="article-title">Me: ' + title + "<li>"
    );

    $(".creating-loading").addClass("isLoading");

    number = new Date().getSeconds();

    kwTarget.val("")

    if(idVal.val() === "1"){
        articleWrapper.append(
            '<li class="article-content hide-class" id=content' +
            number +
            "><pre></pre></li>"
        );
    }

    socket.send(data)
}

//获得消息事件
socket.onmessage = function (msg) {
    const id = idVal.val()
    const contentHtml = $("#content" + number)
    const articleWrapper = $("#article-wrapper");
    if(id === "1"){
        tempKeepText += msg.data;
        contentHtml.removeClass("hide-class");
        contentHtml.find("pre").html(contentHtml.find("pre").html() + msg.data);
    } else {
        articleWrapper.append(
            '<li class="article-content" id=content' + number + '><img src="' + msg.data + '" alt=""></li>'
        );
    }
    $(".creating-loading").removeClass("isLoading");
};

// 连续对话开关
function keepChange() {
    if (keepVal.val() === "1") {
        toast({
            time: 4000,
            msg: "连续对话已打开，请求受Token的长度影响，建议使用自己的APIKey",
        });
    } else {
        toast({ time: 2000, msg: "连续对话已关闭" });
    }
}

// 清空聊天记录
function clearReply() {
    keepText = "";
    $("#article-wrapper").html("");
    return toast({ time: 2000, msg: "聊天记录已清空！" });
}