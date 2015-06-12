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
    console.log("wifiStatusChanged.");
    if (window.android != undefined) {
        if (window.android.isWifiAvailable()) {
            var url="http://sucrq.tuancity.com/v1.1/?surl=http://ht.yeahwifi.com/guide/succeed/?sid=yeahwifi_222&tk=123456&uid=yeahwifi_222";
            console.log(url);
            $.get(url, function(data, status) {
                console.log("access ok.");
            });
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
    $("#loginUsername").attr("value", localStorage.getItem("userName"));
    $("#loginPassword").attr("value", localStorage.getItem("passWord"));
    $("#checkbox-1").prop("checked",  localStorage.getItem("rmbUser")).checkboxradio("refresh");
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

$(".verifyCodeBtn").fastClick(function() {
    me.requestVerifyCode();
});

$(".changePwdBtn").fastClick(function() {
    me.isChangingPassword = true;
    changePage("#RegisterPage");
});

$("#toRegistBtn").fastClick(function() {
    me.isChangingPassword = false;
    changePage("#RegisterPage");
});

$(".wifiStatus").fastClick(function() {
    me.connectWifi(this);
            //     $(".wifiStatus .statusOn").show();
            // $(".wifiStatus .statusOff").hide();

});


var me = {
    countDownSeconds : 0, 
    isChangingPassword : false,
    currentTabIdx : 0,
    // kuLianWifi : {"wifilist": [{"SSID":"SuperMary", "password":"mary8888"},{"SSID":"SuperMary-5G", "password":"mary8888"}]},
    kuLianWifi : {"wifilist": [{"SSID":"豪普生达", "password":""}]},

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
        me.currentTabIdx = idx;
        if (idx == 1 && slide.isInited == true) {
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
            if (me.currentTabIdx == 1) {
                $(".fouce").show();
            }
        });
    },

    parseAds : function(data)
    {
    // console.log(data);
        // var obj = eval("("+data+")"); // json to object
        var html = me.adsTemplate(data);

        $("#adlist").empty();
        $("#adlist").append(html);
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
        }

        return arrHtml.join("");
    },

    connectWifi : function (obj) {
        if (window.android != undefined) {
            var ssid = $(obj).data("wifissid");
            var pwd = $(obj).data("wifipasswd");
            if (ssid == undefined) {
                ssid = me.kuLianWifi.wifilist[0].SSID;
                pwd = me.kuLianWifi.wifilist[0].password;
            }

            console.log("connectWifi " + ssid);
            showLoader("正在连接Wifi，请稍候");
            setTimeout("hideLoader()", 3000);

            window.android.connectWifi(ssid, pwd);
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
        $(".app-list .installBtn").fastClick(function() {
           me.downloadApp(this);
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
            // arrHtml.push("<span class=\"score-star\"><span style=\"width:" + data[i].AppScore + "%;\"></span></span>");

            if (data[i].AppSize != "") {
                arrHtml.push("<span class=\"new-item-size\">" + data[i].AppSize + "</span>");
            }

            arrHtml.push("</dd>");
            arrHtml.push("<dd>");
            arrHtml.push("<div class=\"xiaobian-comment\">");
            arrHtml.push(data[i].BriefSummary == "" ? "暂无介绍" : data[i].BriefSummary);
            arrHtml.push("</div></dd></dl></div>");

            arrHtml.push("<div class='coin_num' >+10</div>");
            arrHtml.push("<img class='coin_icon' src='images/coins.png' />");

            if (isAppInstalled) {
                arrHtml.push("<div class='ui-btn installBtn inactive' data-installed='YES' ></div>");
            } else {
                arrHtml.push("<div class='ui-btn installBtn' data-installed='NO' data-appurl=\""+data.AppSource+"\" data-appid="+data.AppId+"></div>");
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
        
            var phone_number = $(".acount_list #account").text();
            var appId = $(obj).data("addid");
            var url = appServerUrl+"/download_report?"+callback+"&appid="+appId+"&phone_number="+phone_number;
            console.log(url);
            $.getJSON(url, function(data) {
                console.log(data);
            });
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
        arrHtml.push("<a class=\"content-BaiYingFreeDownload\" data-appurl=\""+data.AppSource+"\" data-appid="+data.AppId);
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

        arrHtml.push("<div class=\"des-long-content\">");
        arrHtml.push("<p>" + data.BriefSummary + "</p>");
        arrHtml.push("<br />");
        arrHtml.push("<p>" + data.AppSummary + "</p>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</section>");
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
                $(".verifyCodeBtn").addClass("text_disabled");
                me.countDownSeconds = 60;
                setTimeout("me.countDown()", 1000);
                $(".verifyCodeBtn").attr("disabled","disabled");
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 3000);
            }
        }
    )},

    countDown : function() {
        $(".verifyCodeBtn").text(me.countDownSeconds + "秒");
        me.countDownSeconds = me.countDownSeconds - 1;
        if (me.countDownSeconds <= 0) {
            $(".verifyCodeBtn").removeClass("text_disabled").text("获取验证码");
            $(".verifyCodeBtn").attr("disabled","");
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

    validResetPwd : function() {
        if ($("#changePwdPhoneNumber").val()=='' || $("#changePwdPhoneNumber").val()=='手机号' || !isPhoneNumber($("#changePwdPhoneNumber").val())) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#changePwdVerifyCode").val()=='') {
            showLoader("请填写验证码");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#newPassword").val()=='') {
            showLoader("请填写密码");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#repeatNewPassword").val()!=$("#newPassword").val()) {
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
            if (me.isChangingPassword) {
                var url = appServerUrl+"/reset_passwd?"+callback+"&phone_number="+phone_number+"&new_passwd="+passwd+"&verify_code="+verify_code;
            } else {
                var url = appServerUrl+"/appregister?"+callback+"&phone_number="+phone_number+"&passwd="+passwd+"&verify_code="+verify_code;
            }

            console.log(url);

            $.getJSON(url, function(data) {
                if (data.ret_code == 0) {
                    if (me.isChangingPassword == false) {
                        showLoader("注册成功");
                    } else {
                        showLoader("密码修改成功");
                    }
                    setTimeout("changePageAndHideLoader(\"#MainPage\")", 2000);
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
                        localStorage.setItem("rmbUser", "true");
                        localStorage.setItem("userName", phone_number);
                        localStorage.setItem("passWord", passwd);
                    } else {
                        localStorage.setItem("rmbUser", "false");
                        localStorage.setItem("userName", '');
                        localStorage.setItem("passWord", '');
                    }

                } else {
                    showLoader(data.ret_msg);
                    setTimeout("hideLoader()", 3000);
                }
            });
        }
    }
}; // end of var me




