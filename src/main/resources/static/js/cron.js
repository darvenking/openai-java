let keepText = "";
const apiUrl = "/api/openai";

const apikeyInput = $("#apikey");
const kwTarget = $("#kw-target");

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
            $(".creating-loading").removeClass("isLoading");
        }
    });
}

// 点击事件
function aiClick() {
    const safeHtml = kwTarget.val() || "";
    if (!safeHtml) {
        return toast({ time: 2000, msg: "来问点什么吧" });
    }
    createArticle();
}

// 请求AI回复方法
function createArticle() {
    apikey = apikeyInput.val();
    const safeHtml = window.encodeURIComponent(kwTarget.val()) || "";
    if (!safeHtml) {
        return toast({ time: 2000, msg: "来问点什么吧" });
    }
    let user_id = "";
    let locationHref = window.location.href
        .slice(window.location.href.indexOf("?") + 1)
        .split("&");
    locationHref.forEach(function (val) {
        let parameter = val.slice(0, val.indexOf("=")); //属性
        let data = val.slice(val.indexOf("=") + 1); //值
        if (parameter === "user_id") {
            user_id = data;
        }
    });

    $("#article").removeClass("created");
    $(".creating-loading").addClass("isLoading");

    $.ajax({
        url: apiUrl,
        type: "post",
        data: JSON.stringify({
            text: safeHtml,
            id: $("#id").val(),
            apikey: apikey,
            keep: $("#keep").val(),
            keepText: safeHtml + "\n" + keepText,
        }),
        contentType: "application/json",
        dataType: "json",
        success: function (res) {
            const title = res.title;
            const articleWrapper = $("#article-wrapper");
            const content = res.html;
            const imageurl = res.url;
            const number = new Date().getSeconds();

            keepText = "Human:" + title + " AI:" + content + " " + keepText;

            articleWrapper.append(
                '<li class="article-title">Me: ' + title + "<li>"
            );

            if (imageurl == null) {
                articleWrapper.append(
                    '<li class="article-content" id=content' +
                    number +
                    "><pre></pre></li>"
                );
                let i = 0;
                const interval = setInterval(() => {
                    i++;
                    str = content.substr(0, i);
                    if (i > content.length) {
                        $("#content" + number)
                            .find("pre")
                            .text(str);
                        clearInterval(interval);
                    } else {
                        $("#content" + number)
                            .find("pre")
                            .text(str + "｜");
                    }
                    window.scrollTo(0, document.body.scrollHeight);
                }, 60);
            } else {
                articleWrapper.append(
                    '<li class="article-content" id=content' +
                    number +
                    '><img src="' +
                    imageurl +
                    '" alt=""></li>'
                );
                window.scrollTo(0, document.body.scrollHeight);
            }
            kwTarget.val("");
            $(".creating-loading").removeClass("isLoading");
        },
    });
}

// 连续对话开关
function keepChange() {
    if ($("#keep").val() === "1") {
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
