
var appServerUrl = "http://livew.mobdsp.com/cb"; var callback = "callback=?";
// var appServerUrl = "http://127.0.0.1:5000"; var callback = "";


(function($){
    // changePage("#LoginPage")
})(jQuery);

// js-Android interface
var refreshWifiList = function () {
    me.requestWifiList();
}
// js-Android interface
var wifiStatusChanged = function () {
    if (window.android != undefined) {

        if (window.android.isWifiAvailable()) {
            // todo: access the special URL, and check the external link status.
            $(".wifiStatus .statusOn").show();
            $(".wifiStatus .statusOff").hide();
        } else {
            $(".wifiStatus .statusOff").show();
            $(".wifiStatus .statusOn").hide();
        }
    } else {
        console.log("window.android undefined.");
    }
}

$("#LoginPage").on("pageshow", function () {
    console.log("login page show");
});

$("#LoginPage").on("pageinit", function () {
    console.log("login page init");
    $("#loginUsername").attr("value", $.cookie("userName"));
    $("#loginPassword").attr("value", $.cookie("passWord"));
    $("#checkbox-1").prop("checked",  $.cookie("rmbUser")).checkboxradio("refresh");
});

$("#RegisterPage").on("pageshow", function () {
    console.log("register page show");
});

$("#MainPage").on("pageshow", function () {
    console.log("main page show");
});

$("#MainPage").on("pageinit", function() {
    console.log("main page init");
    // use fastClick will cause pop to home page when tap the tab on PC.
    $("#connectionBtn").click(function() {me.showTab(0);});
    $("#excellentBtn").click(function() {me.showTab(1);});
    $("#mineBtn").click(function() {me.showTab(2);});

    me.requestAds();
    me.requestAppList();
    me.requestWifiList();
    me.showTab(0);
})

$("#logoutBtn").fastClick(function() {
    changePage("#LoginPage");
});

$("#registBtn").fastClick(function() {
    me.register();
});


$("#loginBtn").fastClick( function() {
    me.login();
});

$("input").bind("focus", function() { 
    if ($(this).attr("value")=='手机号')
        $(this).attr("value",""); 
});

$("#verifyCodeBtn").fastClick(function() {
    me.requestVerifyCode();
});

$(".wifiStatus").fastClick(function() {
    if (window.android != undefined && !window.android.isWifiAvailable()) {
        me.connectWifi(this);
        $(".wifiStatus .statusOff").show();
        $(".wifiStatus .statusOn").hide();
    } else {
        console.log("wifi unavailable or window.android undefined.");
    }
});


var me = {
    countDownSeconds : 0, 
    kuLianWifi : {"wifilist": [{"SSID":"SuperMary", "password":"mary8888"},{"SSID":"SuperMary-5G", "password":"mary8888"}]},

    showTab : function(idx) {
        var tabs = new Array("connectionView", "choiceView", "mineView");
        var titles = new Array("连接", "精选", "我的");
        for (var i = 0; i < tabs.length; i++) {
            if (i == idx) {
                $("#" + tabs[i]).show();
                $("#appListTitle").text(titles[i]);
                $("#" + tabs[i] + "Btn").addClass("ui-btn-active");
            } else {
                $("#" + tabs[i]).hide();
                $("#" + tabs[i] + "Btn").removeClass("ui-btn-active");
            }
        }
        currentCat = tabs[idx];
        if (idx == 1) {
            slide.show();
        } else {
            slide.hide();
        }
    },

    requestAds : function()
    {
        // var url=appServerUrl+"/ads?"+callback;
        var url = "json/ads.json";
        console.log("requestAds:"+url);
        $.get(url, function(data, status) {
            var obj = eval("(" + data +")");
            me.parseAds(obj);
            slide.init();
        });
    },

    parseAds : function(data)
    {
    // console.log(data);
        // var obj = eval("("+data+")"); // json to object
        var html = me.adsTemplate(data);

        $("#fouce").empty();
        $("#fouce").append(html);
    },

    adsTemplate : function(data)
    {
        var data = data.adlist;
        var arrHtml = new Array();

        for (var i = 0; i < data.length; i++) {
            arrHtml.push("<li>");
            arrHtml.push("<a href=\"" + data[i].Link + "\">");
            arrHtml.push("<img src=\"" + data[i].ImageSrc + "\" />");
            arrHtml.push("</a>");
            arrHtml.push("</li>");
        }
        return arrHtml.join("");
    },

    requestWifiList : function()
    {
        if (window.android == undefined) {
            // var url=appServerUrl+"/ads?"+callback;
            var url = "json/wifilist.json";
            console.log("requestWifiList:"+url);
            // $.get(url, function(data, status) {
            //     // var obj = eval("(" + data +")");
            //     me.parseWifiList(data);
            // });
            $.getJSON(url, function(data) {
                me.parseWifiList(data);
            });
        } else {
            var jsonStr= window.android.wifiListJsonString();
            var obj = eval("(" + jsonStr +")");
            me.parseWifiList(obj);
        }
    },

    parseWifiList : function(data)
    {
    // console.log(data);
        // var obj = eval("("+data+")"); // json to object
        var html = me.wifiListTemplate(data);

        $("#connectionView .wifi-list").empty();
        $("#connectionView .wifi-list").append(html);

        $("#connectionView .wifi-list li").fastClick(function() {
           me.connectWifi(this);
        });

    },

    wifiListTemplate : function(res) {

        var data = res.wifilist;

        var arrHtml = new Array();
        var arrKuLianWifi = me.kuLianWifi.wifilist;

        for (var i = 0; i < data.length; i++) {

            // if (panle.find("#myId" + data[i].AppId).length == 0) {
                var isKuLian = false;
                var passwd = "";
                for (var j = 0; j < arrKuLianWifi.length; j++) {
                    if (arrKuLianWifi[j].SSID == data[i].SSID) {
                        isKuLian = true;
                        passwd = arrKuLianWifi[j].password;
                        $(".wifiStatus").data("wifissid", data[i].SSID);
                        $(".wifiStatus").data("wifipasswd", passwd);
                        break;
                    }
                }

                var level = Math.abs(data[i].level);
                if (level > 90) { level = 1;
                } else if (level > 70) { level = 2;
                } else if (level > 50) { level = 3;
                } else {                 level = 4; }
                arrHtml.push("<li data-wifissid='"+data[i].SSID+"' data-wifipasswd='"+passwd+"' class=\"index-item list-index\" >"); // style=\"display:none;\"
                arrHtml.push("<div class=\"index-item-main\">");
                arrHtml.push("<dl class=\"clearfix\">");
                arrHtml.push("<dt class=\"item-icon\">");
                arrHtml.push("<img src=\"images/wifi_signal_"+ level +".png\" />");
                arrHtml.push("</dt>");
                arrHtml.push("<dd class=\"item-title\">");
                arrHtml.push("<div class=\"wifi-SSID\">");
                arrHtml.push(subString.autoAddEllipsis(data[i].SSID, 22, true));
                arrHtml.push("</div>");
                if (isKuLian) {
                    arrHtml.push("<div class=\"wifi-desc\">可连接</div>");
                }
                arrHtml.push("</dd></dl></div>");
                arrHtml.push("</li>");
            // }
        }

        return arrHtml.join("");
    },

    connectWifi : function (obj) {
        console.log("connectWifi " + $(obj).data("wifissid"));
        showLoader("正在连接Wifi，请稍候");
        setTimeout("hideLoader()", 3000);

        if (window.android != undefined) {
            window.android.connectWifi($(obj).data("wifissid"), $(obj).data("wifipasswd"));
        } else {
            console.log("try to connect wifi but window.android is undefined");
        }
    },

    requestAppList : function()
    {
    	showLoader();
        // +currentCat+
        var url = appServerUrl+"/applist?"+callback;
        console.log(url);
        $.getJSON(url, function(data) {
    		hideLoader();
    		me.parseAppList(data);
    	});
    },

    parseAppList : function(data)
    {
    // console.log(data);
    	// var obj = eval("("+data+")"); // json to object
    	var html = me.appListTemplate(data);

        // $("#"+currentCat+" .app-list").empty();
        // $("#"+currentCat+" .app-list").append(html);

        $(".app-list").empty();
        $(".app-list").append(html);

        $(".app-list li").fastClick(function() {
           me.clickOnApp(this);
        });
    },

    appListTemplate : function(res) {

        var data = res.applist;

        var arrHtml = new Array();

        for (var i = 0; i < data.length; i++) {

            var isAppInstalled = false;
            if (window.android != undefined && window.android.isAppInstalled(data[i].AppName, 1)) {
                isAppInstalled = true;
            }

            arrHtml.push("<li data-appid='" + data[i].AppId + "' id=\"myId" + data[i].AppId +"\" class=\"index-item list-index\" >");
            arrHtml.push("<div class=\"index-item-main\">");
            arrHtml.push("<dl class=\"clearfix\">");
            arrHtml.push("<dt class=\"item-icon\"><span class=\"app-tags hide\"></span>");
            arrHtml.push("<img src=\"" + data[i].AppLogo + "\" />");
            arrHtml.push("</dt>");
            arrHtml.push("<dd class=\"item-title\">");
            arrHtml.push("<div class=\"item-title-sname\">");
            arrHtml.push("<div class=\"baiying-name\">");
            arrHtml.push(subString.autoAddEllipsis(data[i].AppName, 22, true) + "</div></div></dd>");
            arrHtml.push("<dd class=\"item-star\">");
            arrHtml.push("<span class=\"score-star\"><span style=\"width:" + data[i].AppScore + "%;\"></span></span>");

            if (data[i].AppSize != "") {
                arrHtml.push("<span class=\"new-item-size\">" + data[i].AppSize + "</span>");
            }

            arrHtml.push("</dd>");
            arrHtml.push("<dd>");
            arrHtml.push("<div class=\"xiaobian-comment\">");
            arrHtml.push(data[i].BriefSummary == "" ? "暂无介绍" : data[i].BriefSummary);
            arrHtml.push("</div></dd></dl></div>");

            if (isAppInstalled) {
                arrHtml.push("<div class='installStatus'>已安装</div>");
            } else {
                arrHtml.push("<div class='coin_num' >+10</div>");
                arrHtml.push("<img class='coin_icon' src='images/coins.png' />");
            }

            arrHtml.push("</li>");
        }

        return arrHtml.join("");
    },

    clickOnApp : function (obj)
    {
        me.requestAppDetail($(obj).data("appid"));
    },

    requestAppDetail : function (appId)
    {
        var url = appServerUrl+"/appdetail?"+callback+"&appid="+appId;
        console.log(url);
        showLoader();
        $.getJSON(url, function(data) {
            hideLoader();
            me.parseAppDetail(data);
        });
    },

    parseAppDetail : function (data)
    {
    	$("#appDetail").empty();
        // var obj = eval("("+data+")");
        var html = me.appDetailTemplate(data.detail_info);

        $("#appDetail").append(html);

        $(".content-BaiYingFreeDownload").fastClick(function() {
           me.downloadApp(this);
        });

        $.mobile.changePage("#AppDetailPage", "slideup");
    },

    downloadApp : function (obj)
    {
        if ($(obj).data("installed") == 'YES') {
            showLoader("您已经安装了这个软件");
            setTimeout("hideLoader()", 2000);
            return;
        }
        if (window.android != undefined) {
            window.android.downloadApp($(obj).data("appurl"));
            showLoader("已加入下载队列");
            setTimeout("hideLoader()", 2000);
        }
    },

    appDetailTemplate : function(data)
    {
        var arrHtml  = new Array();
        arrHtml.push(me.appIntroTemplate(data));

        arrHtml.push("<div><img style='"+ data.ImageStyle +"' src='" + data.ImageSrc + "'>");
        for (var i = 0; i < data.ImageSrcList.length; i++) {
          arrHtml.push("<img style='"+ data.ImageStyle +"' src='" + data.ImageSrcList[i] + "'>");
        }
        arrHtml.push("</div>");
        arrHtml.push(me.descriptionTemplate(data))
        arrHtml.push(me.commentTemplate(data));
        return arrHtml.join("");
    },

    appIntroTemplate : function (data) {

        var isAppInstalled = false;
        if (window.android != undefined && window.android.isAppInstalled(data.AppName, 1)) {
            isAppInstalled = true;
        }

        var arrHtml = new Array();
        arrHtml.push("<section class=\"intro\">");
        arrHtml.push("<div class=\"icon-brief\">");
        arrHtml.push("<div class=\"icon\">");
        arrHtml.push("<img src=\"" + data.AppLogo + "\" alt=\"\" />");
        arrHtml.push("</div>");
        arrHtml.push("<div class=\"content-brief\">");
        arrHtml.push("<span class=\"sname contentAppName\">" + data.AppName+ "</span>");
        arrHtml.push("<br>");
        arrHtml.push("<span class=\"score-star\">");
        arrHtml.push("<span style=\"width: " + data.AppScore + "%;\">");
        arrHtml.push("</span>");
        arrHtml.push("</span>");
        arrHtml.push("<br>");
        arrHtml.push("<div class=\"download_size\">");
        arrHtml.push("<span>");
        arrHtml.push("v" + subString.autoAddEllipsis(data.AppVersion, 10, false) + "&nbsp;|&nbsp;" + data.AppSize);
        arrHtml.push("</span>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");

        arrHtml.push("</div>");
        var gaAppName = data.AppName.replace(/\"/g, "”").replace(/'/g, "’");

        arrHtml.push("<div id=\"divdownarea\" class=\"down-area\">");
        arrHtml.push("<div class=\"content-btn-con\">");
        arrHtml.push("<a class=\"content-BaiYingFreeDownload\" data-appurl=\"" + data.AppSource + "\"");
        if (isAppInstalled) {
            arrHtml.push("data-installed='YES' >已安装</a>");
        } else {
            arrHtml.push("data-installed='NO' >下载</a>");
        }
        arrHtml.push("</div>");

        arrHtml.push("<div id=\"divDownloadPanle\" class=\"content-btn-con\">");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</section>");

        return arrHtml.join("");
    },

    descriptionTemplate : function (data) {
        var arrHtml = new Array();
        arrHtml.push("<section class=\"description\">");
        arrHtml.push("<div class=\"content-navdes-wrapper\">");
        arrHtml.push("<div class=\"des-main\">");
        arrHtml.push("<div class=\"des-indent des-short\">");
        arrHtml.push("<div class=\"des-short-content\">");
        arrHtml.push("<p>" + data.BriefSummary + "</p>");
        arrHtml.push("</div>");
        arrHtml.push("<div class=\"des-long-content\">");
        arrHtml.push("<p>" + data.BriefSummary + "</p>");
        arrHtml.push("<br />");
        arrHtml.push("<p>" + data.AppSummary + "</p>");
        arrHtml.push("</div>");
        arrHtml.push("<a id=\"desmore\" href=\"javascript:void(0);\" onclick=\"$('.des-short-content,.des-long-content').toggle();if($('.des-short-content').is(':visible')){scrollToObj($('.description'));$('#desmore').text('了解更多')}else{$('#desmore').text('收起')}\">了解更多");
        arrHtml.push("</a>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</section>");
        return arrHtml.join("");
    },

    commentTemplate : function (res) {
        var data = res;
        var arrHtml = new Array();

        arrHtml.push("<section class=\"comment-area content-comment-area\">");
        arrHtml.push("<div class=\"pingfen-tab\">");
        arrHtml.push("评论");
        arrHtml.push("</div>");
        arrHtml.push("<div class=\"comment-list\">");
        if (data.AppCommentList.length > 0) {
            arrHtml.push("<ul class=\"comment-list-inner\">");
            arrHtml.push(me.getCommentHtml(data.AppCommentList));
            arrHtml.push("</ul>");
        }
        else {
            arrHtml.push("<span class=\"no-msg\">暂无评论</span>");
        }
        arrHtml.push("</div>");
        arrHtml.push("</section>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        return arrHtml.join("");
    },

    getCommentHtml : function (data) {

        // var panle = $("#" + content.currentObj.getPanleId());
        var arrHtml = new Array();

        for (var i = 0; i < data.length; i++) {

            // if (panle.find("#myId" + data[i].Id).length == 0) {
                arrHtml.push("<li id=\"myId" + data[i].Id + "\" class=\"comment-item template\">");
                arrHtml.push("<div class=\"comment-name-version\">");
                arrHtml.push("<span class=\"comment-name\">");
                arrHtml.push(data[i].UserName);// arrHtml.push(subString.autoAddEllipsis(data[i].UserName, 12, true));
                arrHtml.push("</span>");
                arrHtml.push("<span class=\"comment-version\">");
                arrHtml.push("&nbsp;");
                arrHtml.push("<span class=\"score-star\">");
                arrHtml.push("<span style=\"width: " + data[i].Score + "%;\">");
                arrHtml.push("</span>");
                arrHtml.push("</span>");
                arrHtml.push("</span>");
                arrHtml.push("<span class=\"comment-time\">");
                arrHtml.push(data[i].CommentCreateTime);
                arrHtml.push("</span>");
                arrHtml.push("</div>");
                arrHtml.push("<div class=\"comment-content\">");
                arrHtml.push(data[i].CommentContent);
                arrHtml.push("</div> ");
                arrHtml.push("</li>");
            // }
        }
        return arrHtml.join("");
    },

    //获取查询参数
    parseQueryString : function () {
        var str = window.location.search;
        var objURL = {};
        str.replace(
            new RegExp("([^?=&]+)(=([^&]*))?", "g"),
            function ($0, $1, $2, $3) {
                objURL[$1] = $3;
            }
        );
        return objURL;
    },

    requestVerifyCode : function() {
        var phone_number = $("#registPhoneNumber").val();
        if (phone_number == '' || phone_number == '手机号' || !isPhoneNumber(phone_number)) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return;
        }
        var url = appServerUrl+"/appverifycode?"+callback+"&phone_number="+phone_number;
        console.log(url);
        $.getJSON(url, function(data) {
            if (data.ret_code == 0) {
                showLoader("验证码已通过短信发送");
                setTimeout("hideLoader()", 2000);
                $("#verifyCodeBtn").addClass("text_disabled");
                me.countDownSeconds = 60;
                setTimeout("me.countDown()", 1000);
                $("#verifyCodeBtn").attr("disabled","disabled");
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 3000);
            }
        }
    )},

    countDown : function() {
        $("#verifyCodeBtn").text(me.countDownSeconds + "秒");
        me.countDownSeconds = me.countDownSeconds - 1;
        if (me.countDownSeconds <= 0) {
            $("#verifyCodeBtn").removeClass("text_disabled").text("获取验证码");
            $("#verifyCodeBtn").attr("disabled","");
        } else {
            setTimeout("me.countDown()", 1000);
        }
    },

    validLogin : function() {
        if ($("#loginUsername").val()=='' || $("#loginUsername").val()=='手机号' || !isPhoneNumber($("#loginUsername").val())) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#loginPassword").val()=='') {
            showLoader("请填写密码");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        return true;
    },

    validRegist : function() {
        if ($("#registPhoneNumber").val()=='' || $("#registPhoneNumber").val()=='手机号' || !isPhoneNumber($("#registPhoneNumber").val())) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#registVerifyCode").val()=='') {
            showLoader("请填写验证码");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#registPassword").val()=='') {
            showLoader("请填写密码");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#repeatPassword").val()!=$("#registPassword").val()) {
            showLoader("两次输入的密码不一致");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        return true;
    },

    register : function () {
        if (me.validRegist()) {
            var phone_number = $("#registPhoneNumber").val();
            var passwd       = $("#registPassword").val();
            var verify_code  = $("#registVerifyCode").val();
            var url = appServerUrl+"/appregister?"+callback+"&phone_number="+phone_number+"&passwd="+passwd+"&verify_code="+verify_code;
            console.log(url);

            $.getJSON(url, function(data) {
                if (data.ret_code == 0) {
                    showLoader("注册成功");
                    setTimeout("hideLoader()", 2000);

                    changePage("#MainPage");
                    $("#coin").text("0");
                    $("#account").text(phone_number);
                } else {
                    showLoader(data.ret_msg, true);
                    setTimeout("hideLoader()", 3000);
                }
            });
        }
    },

    login : function() {
        if (me.validLogin()) {
            var phone_number = $("#loginUsername").val();
            var passwd       = $("#loginPassword").val();
            var url = appServerUrl+"/applogin?"+callback+"&phone_number="+phone_number+"&passwd="+passwd;
            console.log(url);
            $.getJSON(url, function(data) {
                if (data.ret_code == 0) {
                    changePage("#MainPage");
                    console.log("login success, coin num:" + data.coin_num);
                    if (data.coin_num == undefined) {
                        data.coin_num = 0;
                    }

                    $("#account").text(phone_number);
                    $("#coin").text(data.coin_num);

                    if ($("#checkbox-1").prop("checked") == true) { 
                        $.cookie("rmbUser", "true", { expires: 365 });
                        $.cookie("userName", phone_number, { expires: 365 });
                        $.cookie("passWord", passwd, { expires: 365 });
                    } else {
                        $.cookie("rmbUser", "false", { expires: -1 }); 
                        $.cookie("userName", '', { expires: -1 }); 
                        $.cookie("passWord", '', { expires: -1 }); 
                    }

                } else {
                    showLoader(data.ret_msg);
                    setTimeout("hideLoader()", 3000);
                }
            });
        }
    }
}; // end of var me




