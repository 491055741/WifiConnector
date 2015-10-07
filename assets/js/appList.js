var appServerUrl = "http://livew.mobdsp.com/cb";
var callback = "callback=?";
var localServerUrl = "http://127.0.0.1:5000";
var milkPapaServerUrl = "http://app.milkpapa.com:5000";
var checkNetworkInterval = 1500; // ms
var checkNetworkUrl = "http://115.159.3.16/cb/app_test";
var countDownTimer = null;
var checkNetworkTimer = null;
var autoLoginTimer = null;
var requestKulianWifiTimer = null;
var connectedSSID = null;
var version = null;
var count = 0;
var WifiStatus = {"disconnected" : 0, "connected" : 1, "kulian" : 2, "kulianAuthed" : 3};
(function($){
    $.ajaxSetup({
        timeout: 10000,
        cache: false,
        error: function (x, e) {
            showLoader("T_T 网络出问题了，请稍后再试");
            setTimeout("hideLoader()", 3000);
        }
    });
})(jQuery);
// js-Android interface
var updateDownloadProgress = function (appId, progress) {
    // console.log('app['+appId+'] download progress: '+progress);
    //已安装的应用  包含app列表和app管理里的
    var installApps = $(".installBtn[data-appid="+appId+"]");
    $.each(installApps, function (index,el) {
        if ($(el).hasClass('bigLogo-instBtn')) {
            //如果遮罩层存在就在遮罩层上获取对应的raobj对象
            var cMask = $(el).siblings('.app-img').children('.canvas-mask');
            var raObj = cMask.data('radialIndicator');
            // console.log('cmask-raobj');
        } else {
            // console.log("i'm installbtn");
            var raObj = $(el).data('radialIndicator');
            // console.log(raObj);
        }
        if (raObj) {
            raObj.animate(progress);
        }

    });
};
// js-Android interface
var finishDownloadProgress = function (appId) {
    console.log('app['+appId+'] download finished.');

    // 已安装的应用  包含app列表和app管理里的
    var installApps = $(".installBtn[data-appid="+appId+"]");
    $.each(installApps, function (index,el) {

        $(el).removeClass("downloading");
        if ($(el).hasClass('bigLogo-instBtn')) { // 推荐中的
            //如果遮罩层存在就在遮罩层上获取对应的raobj对象
            $(el).siblings('.app-img').children('.canvas-mask').hide();
            $(el).children('canvas').hide();
            $(el).text('已下载');
            $(el).attr('data-downloaded', 'YES');
            $(el).addClass("hasIns inactive");
            $(el).after("<i class='down-symbol--t1'></i>");
        } else if ($(el).hasClass('manageTab')) { // 下载管理列表中的
            $(el).attr('data-downloaded', 'YES');
            me.removeFromAppManageTab(el);
            me.addToAppManageTab(el);
        } else { // 软件列表中的
            $(el).children('canvas').remove();
            $(el).siblings('.app_down').hide();
            $(el).siblings('.app_coins').hide();
            $(el).attr('data-downloaded', 'YES');
            $(el).addClass('inactive');
            $(el).append('<span>已下载</span>');
        }
    });
};
// js-Android interface
var appInstallFinished = function (appId) {
    var phone_number = $(".acount_list #account").text();
    var url = appServerUrl+"/download_report?"+callback+"&appid="+appId+"&phone_number="+phone_number;
    console.log("Report app install:"+url);

    var installApps = $(".installBtn[data-appid="+appId+"]");
    $.each(installApps, function (index,el) {

        if ($(el).hasClass('bigLogo-instBtn')) { // 推荐中的
            //如果遮罩层存在就在遮罩层上获取对应的raobj对象
            $(el).siblings('.app-img').children('.canvas-mask').hide();
            $(el).children('canvas').hide();
            $(el).text('打  开');
            $(el).attr('data-installed', 'YES');
            $(el).addClass("hasIns inactive");
            // $(el).after("<i class='down-symbol--t1'></i>");
        } else if ($(el).hasClass('manageTab')) { // 下载管理列表中的
            $(el).attr('data-installed', 'YES');
            me.removeFromAppManageTab(el);
            me.addToAppManageTab(el);
        } else { // 软件列表中的
            $(el).children('canvas').remove();
            $(el).siblings('.app_down').hide();
            $(el).attr('data-installed', 'YES');
            $(el).addClass("hasIns inactive");
            $(el).children('span').text('打  开');
        }
    });

    $.ajax({
        type: "GET",
        url: url,
        data: '',
        dataType: "jsonp",
        xhrFields: {
            withCredentials: true
        },
        crossDomain: true,
        success: function (data, textStatus) {
            if (data.ret_code == 0) {
                showLoader('您现在有 '+data.coin_num+' 个金币了');
                setTimeout("hideLoader()", 3000);
                $("#coin").text(data.coin_num);
            } else {
                showLoader(data.ret_msg);
            }
            setTimeout("hideLoader()", 3000);
        },
    });
}
// js-Android interface
var wifiStatusChanged = function (ssid) {
    if ($(".acount_list #account").text() == '') {
        console.log('wifiStatusChanged: not login yet.');
        return;
    }
    console.log("wifiStatusChanged, ssid:"+ssid);
    if (ssid != undefined) { // wifi连接上了
        ssid = ssid.replace(/\"/g, ""); // 去掉双引号
        connectedSSID = ssid;
        if (me.isKuLianWifi(ssid)) {
            console.log("wifiStatusChanged: is kulian wifi.");
            $("#connectWifiBtn").attr("data-wifiStatus", WifiStatus.kulian);
        } else {
            console.log("wifiStatusChanged: not kulian wifi.");
            $("#connectWifiBtn").attr("data-wifiStatus", WifiStatus.connected);
        }
        $("#statusDesc").data("wifissid", ssid);
        me.updateWifiStatusUI($("#connectWifiBtn").attr("data-wifiStatus"));
        me.checkNetwork();
        me.removeFromWifiList(ssid);
    } else { // 断开连接了
        connectedSSID = null;
        $("#connectWifiBtn").attr("data-wifiStatus", WifiStatus.disconnected);
        me.updateWifiStatusUI(WifiStatus.disconnected);
        // me.requestWifiList();
    }
}
// js-android interface
var receivedVerifyCode = function(verifyCode) {
    console.log("receivedVerifyCode:"+verifyCode);
    $("#registVerifyCode").val(verifyCode);
}
// js-android interface
var checkLogin = function() {
    console.log("checkLogin");
    if (!me.isLogin) {
        me.autoLogin();
    }
}

$("#WelcomPage").on("pageshow", function () {
    console.log("welcome page show");

    var userName = getItem("userName");
    var password = getItem("passWord");
    if (userName && userName.length > 0 && password && password.length > 0) {
        setTimeout("changePage('#MainPage')",1000);
        return;
    } else {
        console.log("WelcomPage show : no saved account info");
        setTimeout("changePage('#RegisterPage')",1000);
    }
});

$("#RegisterPage").on("pagebeforeshow", function () {
    console.log("register page show");

    if (me.isChangingPassword) {
        me.showBackBtn(true);
        setTitle("修改密码");
        $("#passwordFields").show();
        $("#inviteCodeFields").hide();
    } else {
        me.showBackBtn(false);
        setTitle("验证");
        $("#passwordFields").hide();
        $("#inviteCodeFields").show();
    }

    $("#registPassword").val('');
    $("#registVerifyCode").val('');
    $("#repeatPassword").val('');
});

$("#RegisterPage").on("pageshow", function () {

});

$("#MainPage").on("pageinit", function() {
    console.log("main page init");
    // use fastClick will cause pop to home page when tap the tab on PC.
    $("#connectionBtn").click(function(e) {me.showTab(0);});
    $("#excellentBtn").click(function(e) {me.showTab(1);});
    $("#mineBtn").click(function(e) {me.showTab(2);});
    $("#connectWifiBtn").attr("data-wifiStatus", WifiStatus.disconnected);
    me.requestAppList();
    me.requestMessage();
    // me.requestAppAds();
    me.fillVersion();
});

$("#MainPage").on("pagebeforeshow", function () {
    console.log("main page before show");
    me.showBackBtn(false);
    me.showTab(me.currentTabIdx);
});

$("#MainPage").on("pageshow", function () {

    console.log("main page show");
    me.requestKulianWifi();
    me.requestWifiList();
    me.checkNetwork();
    if (window.android != undefined) {
        window.android.requestCheckConnection();
    }
    // for debug on browser
    if (window.android == undefined) {
        setTimeout("wifiStatusChanged('SuperMary')", 1000);
        setTimeout("me.autoLogin()", 10000);
    }
});

$('#dialog').jqm({
    modal: true
});

$("#AppDetailPage").on("pagebeforeshow", function () {
    me.showBackBtn(true);
});

$("#AppDetailPage").on("pageshow", function () {
    gallery = new Swiper('.swiper-container',{
        initialSlide: 1,
        pagination: '.swiper-pagination',
        spaceBetween: 30,
        slidesPerView: 2,
        centeredSlides: true
    });
    //gallery.slideTo(1,1000,false);
});

$("#ExchangePage").on("pagebeforeshow", function () {
    me.showBackBtn(true);
    $(".exchangeHeader .coin_num").text($("#coin").text());
});

$("#logoutBtn").fastClick(function() {
    me.isLogin = false;
    me.isChangingPassword = false;
    changePage("#RegisterPage");
});

$("#registBtn").fastClick(function() {
    me.register();
});

$("#coin").fastClick(function() {
    changePage("#ExchangePage");
});

$("input").bind("focus", function() {
    if ($(this).attr("value")=='请填写您的手机号' || $(this).attr("value")=='选填') {
        $(this).attr("value","");
    }
});

$("#shenmaBtn").fastClick(function () {
    if (window.android != undefined) {
        window.android.shenZhouShuMaAuth();
    }
    showLoader("发送认证请求");
    setTimeout("hideLoader()", 2000);
});

$(".user_signin").fastClick(function() {
    me.signIn();
});

$(".verifyCodeBtn").fastClick(function() {
    me.requestVerifyCode();
});

$(".changePwdBtn").fastClick(function() {
    if (!me.isLogin) {
        showLoader("还未登录");
        setTimeout("hideLoader()", 2000);
        return;
    }
    me.isChangingPassword = true;
    changePage("#RegisterPage");
});

$(".qqBtn").fastClick(function() {
    console.log("QQ");
    if (window.android != undefined) {
        window.android.openQQ('123456789');
    }
});

$(".feedbackBtn").fastClick(function() {
    console.log("feedback");
    if (window.android != undefined) {
        window.android.feedback();
    }
});

$(".socialShareBtn").fastClick(function() {
    console.log("social share");
    if (window.android != undefined) {
        window.android.socialShare();
    }
});

$("#toRegistBtn").fastClick(function() {
    me.isChangingPassword = false;
    changePage("#RegisterPage");
});

$("#connectWifiBtn").fastClick(function() {
    var status = parseInt($(this).attr("data-wifiStatus"));
    console.log("connectWifiBtn clicked, status code:"+status);
    switch (status) {
        case 0: // wifi not connected
        case 1: // connected to other wifi, switch to xiaohong wifi
            me.connectWifi(this);
            me.checkNetwork();
            break;
        case 2: // xiaohong not authed
            me.authentication();
            break;
        case 3: // xiaohong authed
            console.log("alreay authed");
            break;
        default:
            break;
    }
});

$(".exchange_item").fastClick(function() {
    me.requestExchange(this);
});

$(".refresh-app-list").fastClick(function() {
    me.requestAppTypePage(me.curAppTabIdx, 1);
});

var me = {
    countDownSeconds : 0, 
    isChangingPassword : false,
    currentTabIdx : 0, // bottom tab
    curAppTabIdx : 1,  // app top tab
    curAppPageIdx : [1,1,1], // current page of each app tab
    myScroll : null,
    kuLianWifi : null,
    appList : null,
    isLogin : false,
    autoLoginRetryCount : 0,
    showBackBtn : function (isShowBackBtn) {
        console.log("showBackBtn:"+isShowBackBtn);
        if (window.android != undefined) {
            window.android.showBackBtn(isShowBackBtn);
        }
        if (isShowBackBtn) {
            console.log("ShowBackBtn: history.length:"+window.history.length);
        }
    },

    checkNetwork : function() {
        clearTimeout(checkNetworkTimer);
        var url = checkNetworkUrl;
        if (me.isLogin) {
            url = url + "?mobile="+$("#account").text();
        }
        console.log("checkNetwork: "+checkNetworkUrl);
        // $("#statusDesc").text("检查网络...");
        $.ajax({
            type: "GET",
            url: url,
            dataType : "jsonp",
            jsonp: "callback",//"callname",//服务端用于接收callback调用的function名的参数
            // jsonpCallback:"success",//callback的function名称
            success : function(data) {
                        console.log("checkNetwork success.");
                        // $("#statusDesc").text("网络连接成功");
                        // $(".wifiStatus .statusOn").show();
                        // $("#connectWifiBtn").hide();
                        // $(".wifiStatus .statusOff").hide();
                        if (parseInt($("#connectWifiBtn").attr("data-wifiStatus")) == WifiStatus.disconnected) {
                            me.updateWifiStatusUI(WifiStatus.disconnected);
                        } else if (parseInt($("#connectWifiBtn").attr("data-wifiStatus")) == WifiStatus.kulian) {
                            $("#connectWifiBtn").attr("data-wifiStatus", WifiStatus.kulianAuthed);
                            me.updateWifiStatusUI(WifiStatus.kulianAuthed);
                            if (me.isLogin) {
                                me.reportAuthenSuccess();
                            }
                        }
                        console.log("connectWifiBtn wifiStatus:"+$("#connectWifiBtn").attr("data-wifiStatus"));
                        if (!me.isLogin) {
                            me.autoLogin();
                        }
                    },
            error : function() {
                        console.log("checkNetwork fail.");
                        // $("#statusDesc").text("网络连接失败");
                        $(".wifiStatus .statusOn").hide();
                        $(".wifiStatus .statusOff").show();
                        if (parseInt($("#connectWifiBtn").attr("data-wifiStatus")) == WifiStatus.kulian) {
                            checkNetworkTimer = setTimeout(me.authentication(), checkNetworkInterval);
                        }
                    }
        });
    },

    authentication : function() {
        if (!me.isLogin) {
            me.autoLogin();
            return;
        }
        console.log("authentication.");
        var phone_number = $(".acount_list #account").text();
        var url = appServerUrl+"/query_coin?phone_number="+phone_number;
        $.getJSON(url, function(data) {

            if (data.ret_code == 0 || data.ret_code == 3001) {
                me.sendAuthenticationRequest();
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 2000);
            }
        });
    },

    sendAuthenticationRequest : function() {
        console.log("sendAuthenticationRequest.");
        clearTimeout(checkNetworkTimer);
        // $("#statusDesc").text("认证中...");
        if (checkNetworkInterval > 10000) {
            checkNetworkInterval = 1500;
            console.log("authentication timeout.");
            // $("#statusDesc").text("认证超时");
            return;
        }

        var authUrl = "http://182.254.140.228/portaltt/Logon.html";
        $.ajax({
            type: "GET",
            crossDomain: true,
            url: authUrl,
            data: '',
            dataType : "jsonp",
            // jsonp: "callback",//服务端用于接收callback调用的function名的参数
            // jsonpCallback:"success_jsonpCallback",//callback的function名称
            // 由于返回的是html网页，不是json数据，所以下面会认为请求失败，但实际AC已经认证通过
            success : function(data, textStatus) {
                        // $("#statusDesc").text("认证成功");
                      },
            error : function(XMLHttpRequest, textStatus, errorThrown) {
            }
        });

        checkNetworkTimer = setTimeout(me.checkNetwork(), checkNetworkInterval);
        checkNetworkInterval = checkNetworkInterval + 1000;
    },

    reportAuthenSuccess : function() {
        console.log("reportAuthenSuccess.");
        if (!me.isLogin) {
            console.log("error! not login yet!");
            return;
        }
        var phone_number = $(".acount_list #account").text();
        var url = appServerUrl+"/dec_coin?phone_number="+phone_number;
        $.getJSON(url, function(data) {
            if (data.ret_code == 0) {
                $("#dialog_message").empty();
                var html = "<div class='modalViewTitle'>恭喜您已连接到免费小鸿WiFi</div>";
                html += "<div class='modalViewText'>今日消耗金币</div>";
                html += "<div class='modalViewBigText'>"+data.dec_coin_num+"</div>"
                html += "<div class='modalViewText'>您还有"+data.coin_num+"金币</div>";
                $("#dialog_message").append(html);
                me.showTab(0);
                $("#dialog").jqmShow();
                $("#coin").text(data.coin_num);
            } else if (data.ret_code != 3001) { // 3001 means already  coin
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 2000);
            }
        });
    },

    signIn : function() {
        console.log("sign in.");
        if (!me.isLogin) {
            return;
        }
        var phone_number = $(".acount_list #account").text();
        var url = appServerUrl+"/user_sign?phone_number="+phone_number;
        $.getJSON(url, function(data) {
            if (data.ret_code == 0) {
                $("#dialog_message").empty();
                var html = "<div class='modalViewTitle'>恭喜您已签到成功</div>";
                html += "<div class='modalViewText'>今日赢取金币</div>";
                html += "<div class='modalViewBigText'>"+data.add_coin_num+"</div>"
                $("#dialog_message").append(html);
                me.showTab(0);
                $("#dialog").jqmShow();
                $("#coin").text(data.coin_num);
            } else if (data.ret_code == 3002) {
                showLoader("今天已经领取过了，明天再来哟！");
                setTimeout("hideLoader()", 3000);
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 3000);
            }
        });
    },

    updateWifiStatusUI : function(status) {
        // WifiStatus = {"disconnected" : 0, "connected" : 1, "kulian" : 2, "kulianAuthed" : 3};
        console.log("updateWifiStatusUI:"+status);
        switch (parseInt(status))
        {
            case 0:
                $("#statusDesc").text("未连接到Wifi");
                $(".wifiStatus .statusOff").show();
                $(".wifiStatus .statusOn").hide();
                $("#connectWifiBtn").show().text("连接小鸿免费Wifi");
                $(".portalframe").hide();
                break;
            case 1:
                $("#statusDesc").text("已连接到Wifi: "+$("#statusDesc").data("wifissid"));
                $(".wifiStatus .statusOff").hide();
                $(".wifiStatus .statusOn").show();
                $("#connectWifiBtn").show().text("切换到小鸿免费Wifi");
                $(".portalframe").hide();
                break;
            case 2:
                $("#statusDesc").text("已连接到小鸿免费Wifi: "+$("#statusDesc").data("wifissid"));
                $(".wifiStatus .statusOff").hide();
                $(".wifiStatus .statusOn").show();
                $("#connectWifiBtn").show().text("认证上网");
                $(".portalframe").hide();
                break;
            case 3:
                $("#statusDesc").text("已连接到小鸿免费Wifi: "+$("#statusDesc").data("wifissid"));
                $(".wifiStatus .statusOff").hide();
                $(".wifiStatus .statusOn").show();
                $("#connectWifiBtn").hide();
                $(".portalframe").hide();
                break;
            default:
                break;
        }
    },

    showTab : function(idx) {
        var tabs = new Array("connectionView", "choiceView", "mineView");
        for (var i = 0; i < tabs.length; i++) {
            if (i == idx) {
                $("#" + tabs[i]).show();
                $("#" + tabs[i] + "Btn").addClass("ui-btn-active");
            } else {
                $("#" + tabs[i]).hide();
                $("#" + tabs[i] + "Btn").removeClass("ui-btn-active");
            }
        }
        me.currentTabIdx = idx;
        if (idx == 1 && slide.isInited == true) { // app tab
            slide.show();
        } else {
            slide.hide();
        }
        if (idx == 1) {
            me.refreshScroll();
        }

        var titles = new Array("连接", "精选", "我的");
        setTitle(titles[idx]);
    },

    requestKulianWifi : function()
    {
        var url = appServerUrl+"/get_ssidlist?"+callback;
        console.log("requestKulianWifi:"+url);
        me.kuLianWifi = {"ssidlist": [ {"ssid":"@小鸿科技","ssid_passwd":""},{"ssid":"test","ssid_passwd":""}]};

        $.ajax({
            type: "GET",
            url: url,
            dataType : "jsonp",
            jsonp: "callback",
            success : function(data) {
                        console.log("requestKulianWifi success.");
                        if (data.ret_code == 0 && data.ssidlist.length > 0) {
                            me.kuLianWifi = data.ssidlist;
                        }
                        clearTimeout(requestKulianWifiTimer);
                    },
            error : function() {
                        console.log("requestKulianWifi fail.");
                        requestKulianWifiTimer = setTimeout(me.requestKulianWifi(), 2000);
                    }
        });
    },

    requestMessage : function()
    {
        var url = appServerUrl+"/app_broadcast?"+callback;
        console.log("requestAppMessage:"+url);
        $.getJSON(url, function(data) {
            if (data.broadcastlist != undefined && data.broadcastlist.length > 0) {
                me.parseMessages(data);
                me.showMessage();
            }
        });        
    },

    requestAppAds : function()
    {
        var url = appServerUrl+"/appad?"+callback;
        console.log("requestAppAds:"+url);
        $.getJSON(url, function(data) {
            if (data.adlist != undefined && data.adlist.length > 0) {
                me.parseAppAds(data);
                slide.init();
                $("#olSlideNum").hide();
                // $("#tab-1 .wrapper").css("top", 200);
                // if (me.currentTabIdx == 0) {
                    $(".fouce").show();
                // }
                if (me.myScroll[1] != null) {
                    setTimeout(me.myScroll[1].refresh(), 1000);
                }
            }
        });
    },

    parseAppAds : function(data)
    {
        var html = me.appAdsTemplate(data);
        $("#adlist").empty();
        $("#adlist").append(html);
    },

    appAdsTemplate : function(data)
    {
        var ads = data.adlist;
        var arrHtml = new Array();

        for (var i = 0; i < ads.length; i++) {
            arrHtml.push("<li>");
            var url = ads[i].click_url;
            if (url.length == 0) {
                url = "#";
            }
            arrHtml.push("<a href=\"" + url + "\">");
            arrHtml.push("<img src=\"" + ads[i].image_url + "\" />");
            arrHtml.push("</a>");
            arrHtml.push("</li>");
        }
        return arrHtml.join("");
    },

    requestWifiList : function()
    {
        console.log("requestWifiList");
        if (window.android == undefined) {
            var url = localServerUrl + "/wifilist?"+callback;
            // console.log("requestWifiList:" + url);
            $.getJSON(url, function(data) {
                me.parseWifiList(data);
            });
        } else {
            var jsonStr= window.android.wifiListJsonString();
            var obj = eval("(" + jsonStr +")");
            me.parseWifiList(obj);
        }
        setTimeout("me.requestWifiList()", 15000);
    },

    parseWifiList : function(data)
    {
        var html = me.freeWifiListTemplate(data);
        $("#connectionView .freeWifi_box").empty();
        $("#connectionView .freeWifi_box").append(html);

        var html = me.encryptWifiListTemplate(data);
        $("#connectionView .annexWifi_box").empty();
        $("#connectionView .annexWifi_box").append(html);

        $(".wifiList img.lazy").lazyload({threshold:300, placeholder:null });
        $(window).trigger("scroll");

        $("#connectionView .wifiList li").fastClick(function() {
            me.connectWifi(this);
        });

        var arrWifiList = data.wifilist;

        for (var i = 0; i < arrWifiList.length; i++) {
            if (me.isKuLianWifi(arrWifiList[i].SSID)) {
                $("#connectWifiBtn").data("wifissid", arrWifiList[i].SSID);
                $("#connectWifiBtn").data("wifipasswd", '');
                $("#connectWifiBtn").data("wifiencrypt", arrWifiList[i].encrypt);
                break;
            }
        }
    },

    isKuLianWifi : function(ssid)
    {
        if (ssid.startWith("Hongwifi") || ssid.indexOf("小鸿")!=-1) { //  || ssid.startWith("SuperMary")
            console.log("isKuLianWifi match pattern: "+ssid);
            return true;
        }
        var ssidMD5 = CryptoJS.MD5(ssid, { asString: true });
        console.log("my wifi ssid:"+ssid+"  MD5:"+ssidMD5);
        var isKuLian = false;
        // var passwd = "";
        var arrKuLianWifi = me.kuLianWifi;
        for (var j = 0; j < arrKuLianWifi.length; j++) {
            if (arrKuLianWifi[j].ssid == ssidMD5) {
                isKuLian = true;
                console.log("Found a xiaohong wifi:"+ssid);
                break;
            }
        }
        return isKuLian;
    },

    freeWifiListTemplate : function(res)
    {
        var data = res.wifilist;
        var arrHtml = new Array();
        var wifiCount = 0;
        for (var i = 0; i < data.length; i++) {

            if (data[i].encrypt != "" || data[i].SSID == connectedSSID) {
                continue;
            }

            var level = Math.abs(data[i].level);
            if (level > 90) { level = 1;}
            else if (level > 70) { level = 2; }
            else if (level > 50) { level = 3; }
            else { level = 4; }
            
            var li = "<li data-wifissid='"+data[i].SSID+"' data-wifiencrypt='"+data[i].encrypt+"'>";
            li += "<img class='wifi-icon lazy' data-original='images/wifi_signal_"+ level +".png' />";
            li += "<a>"+subString.autoAddEllipsis(data[i].SSID, 22, true)+"</a>";
            if (me.isKuLianWifi(data[i].SSID)) {
                li += "<span class='recomWifi_txtspan'>推荐</span>";
            }
            li += "</li>";
            arrHtml.push(li);
            wifiCount++;
        }
        $(".freeWifi_title").text(wifiCount+"个免费WiFi");
        if (wifiCount == 0) {
            $(".freeWifi").hide();
        } else {
            $(".freeWifi").show();
        }
        return arrHtml.join("");
    },

    encryptWifiListTemplate : function(res)
    {
        var data = res.wifilist;
        var arrHtml = new Array();
        var wifiCount = 0;
        for (var i = 0; i < data.length; i++) {
            if (data[i].encrypt == "" || data[i].SSID == connectedSSID) {
                continue;
            }

            var level = Math.abs(data[i].level);
            if (level > 90) { level = 1;}
            else if (level > 70) { level = 2; }
            else if (level > 50) { level = 3; }
            else { level = 4; }

            var li = "<li data-wifissid='"+data[i].SSID+"' data-wifiencrypt='"+data[i].encrypt+"' >";
            li += "<img class='wifi-icon lazy' data-original=\"images/wifi_signal_"+ level +".png\"><a>"+subString.autoAddEllipsis(data[i].SSID, 22, true)+"</a>";
            li += "<img class='lock-icon' src='images/lock.png' />";
            li += "</li>";
            arrHtml.push(li);
            wifiCount++;
        }
        if (wifiCount == 0) {
            $(".annexWifi").hide();
        } else {
            $(".annexWifi").show();
        }
        return arrHtml.join("");
    },

    connectWifi : function (obj)
    {
        var ssid    = $(obj).data("wifissid");
        var encrypt = $(obj).data("wifiencrypt");
        var pwd     = "";
        if (ssid == undefined) {
            showLoader("没有搜索到小鸿Wifi");
            setTimeout("hideLoader()", 2000);
            // me.requestWifiList();
            return;
        }
        if (encrypt != "") {
            pwd = prompt("请输入"+ssid+"的密码:", '');
            if (pwd == null) {
                return;
            }
        }
        console.log("connectWifi: "+ssid+"  pwd: "+pwd+"  encrypt: "+encrypt);
        showLoader("正在连接Wifi，请稍候");
        setTimeout("hideLoader()", 8000);

        if (window.android != undefined) {
            window.android.connectWifi(ssid, pwd, encrypt);
        } else {
            console.log("try to connect wifi but window.android is undefined");
        }
    },

    requestAppList : function()
    {
        showLoader();
        for (var type = 1; type <= 3; type++) {
            $("#tab-"+type+" .app-list").empty();
            me.requestAppTypePage(type, 1);
        }
    },

    // type : 1 ~ 3
    requestAppTypePage : function(type, page)
    {
        var url = appServerUrl+"/applist_page?apptype="+type+"&page="+page+"&"+callback;
        console.log("requestAppList:" + url);

        $("#tab-"+type+" .refresh-app-list").hide();

        $.ajax({
            type: "GET",
            url: url,
            dataType : "jsonp",
            jsonp: "callback",//"callname",//服务端用于接收callback调用的function名的参数
            // jsonpCallback:"success",//callback的function名称
            success : function(data) {
                        hideLoader();
                        if (data.applist_count == 0) {  // last page
                            $("#tab-"+type+" .scroller-pullUp").hide();
                            return;
                        }
                        $("#tab-"+type+" .wrapper").show();

                        me.curAppPageIdx[type] = page + 1;
                        me.appList = data;
                        var html;
                        if (type == 1) {
                            html = me.appBigLogoListTemplate(data);
                        } else {
                            html = me.appListTemplate(data);
                        }

                        $("#tab-"+type+" .app-list").append(html);
                        if (type == 1) {
                            $("img.lazy").lazyload({threshold:300, effect:"fadeIn", placeholder:null });
                            $(window).trigger("scroll");
                        }

                        var btns = $("#tab-"+type+" .app-list .installBtn[data-installed='YES']");
                        $.each(btns, function(index, el) {
                            me.addToAppManageTab(el);
                        });

                        $("#tab-"+type+" .app-list li").click(function() {  // don't use fastclick, it will eat 'touchbegin' event
                            //[2015-9-24]
                             me.clickOnApp(this);
                            // me.downloadApp(todo);
                        });

                        $("#tab-"+type+" .app-list .installBtn").click(function(e) {
                            e.stopPropagation();
                            console.log('click on installBtn');
                            if ($(this).hasClass('downloading')) {
                                console.log('downloading, ignore download request...');
                                return;
                            }
                            if ($(this).attr("data-installed") == "YES") {
                                if (window.android) {
                                    showLoader("请稍候...");
                                    setTimeout("hideLoader()", 2000);
                                    console.log('start app '+$(this).data("pkgname"));
                                    window.android.startAPP($(this).data("pkgname"));
                                } else {
                                    showLoader("只能在手机中打开");
                                    setTimeout("hideLoader()", 2000);
                                }
                                return;
                            }
                            if ($(this).attr("data-downloaded") == "YES") {
                                console.log('downloaded, ignore download request...');
                                return;
                            }

                            me.downloadApp(this);
                            $(this).addClass("inactive");
                            //创建圆形进度条
                            //如果为tab1(精选)中的安装按钮则在div.canvas-mask中创建进度条
                            if ($(this).hasClass('bigLogo-instBtn')){
                                var width = parseInt($(this).parent().width()/8);
                                console.log(width);
                                $(this).siblings('.app-img').children('.canvas-mask').show().radialIndicator({
                                    radius: width,
                                    displayNumber: false,
                                    barColor: '#fff',
                                    barBgColor: 'rgba(255,255,255,0.4)',
                                    barWidth: 6,
                                    initValue: 0,
                                    roundCorner : false,
                                    percentage: true
                                });
                                $(this).text('下载中');
                            } else {
                                $(this).siblings('.app_coins').hide();
                                $(this).addClass('app-downloading--t3').radialIndicator({
                                    radius: 15,
                                    displayNumber: false,
                                    barColor: '#48D1CC',
                                    barBgColor: '#eee',
                                    barWidth: 2,
                                    initValue: 0,
                                    roundCorner : false,
                                    percentage: false
                                })
                            }
                        });
                        setTimeout(me.initIScroll(type), 2000);
                        if (me.myScroll[4] == null) {
                            me.initIScroll(4);
                        }
                    },
            error : function() {
                        $("#tab-"+type+" .refresh-app-list").show();
                        $("#tab-"+type+" .wrapper").hide();
                    }
        });
    },

    appListTemplate : function(res)
    {
        var data = res.applist;
        if (data == null || data == undefined) {
            return;
        }

        var arrHtml = new Array();

        for (var i = 0; i < data.length; i++) {

            if (data[i].PackageName == undefined) {
                break;
            }

            var isAppInstalled = false;
            if (window.android != undefined && window.android.isAppInstalled(data[i].PackageName, 1)) {
                isAppInstalled = true;
            }
            // arrHtml.push("<li style='height:50px;'>aaa");

            arrHtml.push("<li data-appid='" + data[i].AppId + "' id=\"myId" + data[i].AppId +"\" class=\"index-item list-index\" >");
            arrHtml.push("<div class=\"index-item-main\">");
            arrHtml.push("<dl class=\"clearfix\">");
            arrHtml.push("<dt class=\"item-icon\"><span class=\"app-tags hide\"></span>");
            arrHtml.push("<img src=\"" + data[i].AppLogo + "\" />");
            arrHtml.push("</dt>");
            arrHtml.push("<dd class=\"item-title\">");
            arrHtml.push("<div class=\"item-title-sname\">");
            arrHtml.push("<div class=\"baiying-name\">");
            arrHtml.push(subString.autoAddEllipsis(data[i].AppName, 30, true) + "</div></div></dd>");
            arrHtml.push("<dd class=\"item-star\">");
            // arrHtml.push("<span class=\"score-star\"><span style=\"width:" + data[i].AppScore + "%;\"></span></span>");

            if (data[i].AppSize != "") {
                // var size = parseFloat(data[i].AppSize/1000000).toFixed(1) + "MB";
                arrHtml.push("<span class=\"new-item-size\">" + data[i].AppSize + "</span>");
            }

            arrHtml.push("</dd>");
            arrHtml.push("<dd>");
            arrHtml.push("<div class=\"xiaobian-comment\">");
            arrHtml.push(data[i].BriefSummary == "" ? "暂无介绍" : subString.autoAddEllipsis(data[i].BriefSummary, 25, true));
            arrHtml.push("</div></dd></dl></div>");

            arrHtml.push("<div class='app_down'>");
            // isAppInstalled = true;
            if (isAppInstalled) {
                arrHtml.push("<div class='ui-btn installBtn inactive hasIns' data-installed='YES' data-applogo=\""+data[i].AppLogo+"\"  data-appname=\""+data[i].AppName+"\" data-appurl=\""+data[i].AppSource+"\" data-appid="+data[i].AppId+" data-pkgname=\""+data[i].PackageName+"\"><span>打  开</span></div>");
            } else {
                arrHtml.push("<div class='app_coins'>");
                arrHtml.push("<div class='coin_num'><span>"+data[i].GiveCoin+"</span> 金币</div>");
                arrHtml.push("</div>");

                arrHtml.push("<div class='ui-btn installBtn' data-installed='NO' data-applogo=\""+data[i].AppLogo+"\"  data-appname=\""+data[i].AppName+"\" data-appurl=\""+data[i].AppSource+"\" data-appid="+data[i].AppId+" data-pkgname=\""+data[i].PackageName+"\"></div>");
            }
            arrHtml.push("</div>");
            arrHtml.push("</div>");
            arrHtml.push("</li>");
        }

        return arrHtml.join("");
    },

    appBigLogoListTemplate : function (res) {

        var data = res.applist;
        if (data == null || data == undefined) {
            return;
        }

        var arrHtml = new Array();

        for (var i = 0; i < data.length; i++) {

            if (data[i].PackageName == undefined) {
                break;
            }

            var isAppInstalled = false;
            if (window.android != undefined && window.android.isAppInstalled(data[i].PackageName, 1)) {
                isAppInstalled = true;
            }
            arrHtml.push("<li data-appid='" + data[i].AppId + "' id=\"myId" + data[i].AppId +"\" class=\"index-item list-index h-list-item\" >");
            arrHtml.push("<div class=\"index-item-w\">");
            arrHtml.push("<div class='app-img'>");
            arrHtml.push("<img class='lazy' data-original='"+data[i].AppLargeLogo+"' />");
            //遮罩层
            arrHtml.push("<div class='canvas-mask'></div>");
            arrHtml.push("<div class='dummy'></div>");
            arrHtml.push("</div>");

            arrHtml.push("<div class=\"h baiying-name\">");
            arrHtml.push(subString.autoAddEllipsis(data[i].AppName, 30, true));
            if (data[i].AppSize != "") {
                arrHtml.push("<span class=\"new-item-size\"> " + data[i].AppSize + " </span>");
            }
            arrHtml.push("</div>");
            // isAppInstalled = true;
            if (isAppInstalled) {
                arrHtml.push("<div class='ui-btn installBtn bigLogo-instBtn hasIns inactive' data-installed='YES' data-applogo=\""+data[i].AppLogo+"\"  data-appname=\""+data[i].AppName+"\" data-appurl=\""+data[i].AppSource+"\" data-appid="+data[i].AppId+" data-pkgname=\""+data[i].PackageName+"\">打 开</div>");
                arrHtml.push("<i class='down-symbol--t1'></i>")
            } else {
                arrHtml.push("<div class='ui-btn installBtn bigLogo-instBtn' data-installed='NO' data-applogo=\""+data[i].AppLogo+"\"  data-appname=\""+data[i].AppName+"\" data-appurl=\""+data[i].AppSource+"\" data-appid="+data[i].AppId+" data-pkgname=\""+data[i].PackageName+"\">下 载</div>");
            }
            arrHtml.push("<div class='app-down-des'>安装<span class='reward'>+"+data[i].GiveCoin+"</span></div>");
            arrHtml.push("</div>");
            arrHtml.push("</li>");
        }

        return arrHtml.join("");
    },

    getAppInfoById : function (appId)
    {
        var appList = me.appList.applist;
        for (var i = 0; i < appList.length; i++) {
            if (appList[i].AppId == appId) {
                return appList[i];
            }
        }
        return null;
    },

    clickOnApp : function (obj)
    {
        var appId = $(obj).data("appid");
        me.requestAppDetail(appId);
    },

    requestAppDetail : function (appId)
    {
        var url = appServerUrl+"/appdetail?"+callback+"&apptype="+me.curAppTabIdx+"&appid="+appId;
        console.log(url);
        showLoader();
        $.getJSON(url, function(data) {
            hideLoader();
            me.parseAppDetail(data);
        });
    },

    parseAppDetail : function (data)
    {
    	$(".appDetail").empty();
        // var obj = eval("("+data+")");
        var html = me.appDetailTemplate(data.detail_info);
        $(".appDetail").append(html);

        $(".DownloadBtn").fastClick(function() {

            if ($(this).data("installed") == "YES") {
                if (window.android) {
                    showLoader("请稍候...");
                    setTimeout("hideLoader()", 2000);
                    console.log('start app '+$(this).data("pkgname"));
                    window.android.startAPP($(this).data("pkgname"));
                } else {
                    showLoader("只能在手机中打开");
                    setTimeout("hideLoader()", 2000);
                }
                return;
            }
            if ($(this).data("downloaded") == "YES") {
                console.log('downloaded, ignore download request...');
                return;
            }

            //去掉安装'文案'，并创建圆形进度条
            $(this)
                .addClass('inactive installBtn')
                .text('')
                .radialIndicator({
                    radius: 12,
                    displayNumber: false,
                    barColor: '#48D1CC',
                    barBgColor: '#eee',
                    barWidth: 2,
                    initValue: 0,
                    roundCorner : false,
                    percentage: false
                });
            me.downloadApp(this);
        });

        //轮播图点击放大
        $('.swiper-container').on('click','.swiper-slide',function(){
            var $parent = $('.swiper-container'),
                index = $(this).index();
            //alert(index);
            if (!$parent.hasClass('larger')){
                $parent.addClass('larger');
                gallery.params.slidesPerView = 1;
                gallery.update({initialSlide:index});
            }else {
                $parent.removeClass('larger');
                gallery.params.slidesPerView = 2;
                gallery.update({initialSlide:index});
            }
        });

        changePage("#AppDetailPage");
    },

    downloadApp : function (installBtn)
    {
        console.log("downloadApp");
        if ($(installBtn).data("installed") == 'YES') {
            showLoader("软件已经安装了");
            setTimeout("hideLoader()", 2000);
            return;
        }
        $(installBtn).addClass("downloading");
        me.addToAppManageTab(installBtn);

        if (window.android != undefined) {
            var appId = $(installBtn).data("appid");
            var appInfo = me.getAppInfoById(appId);
            if (appInfo != null) {
                var mac = window.android.getMacAddress();
                var url = appInfo.Clickurl.replace("[M_MAC]", mac);
                var imei = window.android.getIMEI();
                url = url.replace("[M_IMEI]", imei);
                $.getJSON(url, function(data) {
                    console.log("report click:"+url);
                });
            }

            window.android.downloadApp(appId, $(installBtn).data("appname"), $(installBtn).data("pkgname"), $(installBtn).data("appurl"));
            showLoader("开始下载，保持WIFI酷连打开，完成安装后才会赠送金币哦");
            setTimeout("hideLoader()", 3000);
        } else { // for test on browser
            console.log("window.android undefined. url:" + $(installBtn).data("appurl"));
            setTimeout("updateDownloadProgress("+$(installBtn).data("appid")+",50)", 1000);
            setTimeout("finishDownloadProgress("+$(installBtn).data("appid")+")", 3000);
            setTimeout("appInstallFinished("+$(installBtn).data("appid")+")", 8000);
        }
    },

    removeFromWifiList : function(SSID)
    {
        var li = $(".wifiList li[data-wifissid='"+SSID+"']");
        li.remove();
        var wifiCount = $(".freeWifi_box li").length;
        $(".freeWifi_title").text(wifiCount+"个免费WiFi");
        if (wifiCount == 0) {
            $(".freeWifi").hide();
        }
        wifiCount = $(".annexWifi_box li").length;
        if (wifiCount == 0) {
            $(".annexWifi").hide();
        } else {
            $(".annexWifi").show();
        }
    },

    removeFromAppManageTab : function(installBtn)
    {
        var li = $("#tab-4 .app-list li[data-appid='" + $(installBtn).data('appid')+"']");
        li.remove();
        if ($("#tab-4 .app-list .downloading").children("li").length == 0) {
            $("#tab-4 .app-list .downloading").hide();
        }
        if ($("#tab-4 .app-list .hasDownloaded").children("li").length == 0) {
            $("#tab-4 .app-list .hasDownloaded").hide();
        }
        if ($("#tab-4 .app-list .hasInstalled").children("li").length == 0) {
            $("#tab-4 .app-list .hasInstalled").hide();
        }
    },

    addToAppManageTab : function(installBtn)
    {
        var isAppInstalled = ($(installBtn).data("installed") == 'YES');
        var isAppDownloaded = ($(installBtn).data("downloaded") == 'YES');

        var arrHtml = new Array();
        var thisInstallBtn;
        arrHtml.push("<li data-appid='" + $(installBtn).data("appid") + "' \" class=\"index-item list-index\" >");
        arrHtml.push("<div class=\"index-item-main\">");
        arrHtml.push("<dl class=\"clearfix\">");
        arrHtml.push("<dt class=\"item-icon\"><span class=\"app-tags hide\"></span>");
        arrHtml.push("<img src=\"" + $(installBtn).data("applogo") + "\" />");
        arrHtml.push("</dt>");
        arrHtml.push("<dd class='item-title item-title--t4'>");
        arrHtml.push("<div class=\"item-title-sname\">");
        arrHtml.push("<div class=\"baiying-name\">");
        arrHtml.push(subString.autoAddEllipsis($(installBtn).data("appname"), 30, true) + "</div></div></dd>");
        arrHtml.push("</dl></div>");

        arrHtml.push("<div class='app_down'>");
        // console.log($(installBtn).data());
        if (isAppInstalled) {
            arrHtml.push("<div class='ui-btn installBtn manageTab inactive hasIns' data-installed='YES' data-applogo=\""+$(installBtn).data('applogo')+"\"  data-appname=\""+$(installBtn).data('appname')+"\" data-appurl=\""+$(installBtn).data('appurl')+"\" data-appid="+$(installBtn).data('appid')+" data-pkgname=\""+$(installBtn).data('pkgname')+"\"><span>打  开</span></div>");
        } else if (isAppDownloaded) {
            arrHtml.push("<div class='ui-btn installBtn manageTab inactive' data-downloaded='YES' data-applogo=\""+$(installBtn).data('applogo')+"\"  data-appname=\""+$(installBtn).data('appname')+"\" data-appurl=\""+$(installBtn).data('appurl')+"\" data-appid="+$(installBtn).data('appid')+" data-pkgname=\""+$(installBtn).data('pkgname')+"\"><span>已下载</span></div>");
        } else {
            arrHtml.push("<div class='ui-btn installBtn manageTab' data-installed='NO' data-applogo=\""+$(installBtn).data('applogo')+"\"  data-appname=\""+$(installBtn).data('appname')+"\" data-appurl=\""+$(installBtn).data('appurl')+"\" data-appid="+$(installBtn).data('appid')+" data-pkgname=\""+$(installBtn).data('pkgname')+"\"></div>");
        }

        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</li>");
        var html = arrHtml.join("");

        if (isAppInstalled) {
            $("#tab-4 .app-list .hasInstalled").show().append(html);
            thisInstallBtn = $("#tab-4 .installBtn[data-appid='" + $(installBtn).data('appid') + "']");
        } else if (isAppDownloaded) {
            $("#tab-4 .app-list .hasDownloaded").show().append(html);
            thisInstallBtn = $("#tab-4 .installBtn[data-appid='" + $(installBtn).data('appid') + "']");
        } else {
            //count = 1;
            $("#tab-4 .app-list .downloading").show().append(html);
            thisInstallBtn = $("#tab-4 .installBtn[data-appid='" + $(installBtn).data('appid') + "']");
            //创建圆形进度条
            thisInstallBtn.radialIndicator({
                radius: 15,
                displayNumber: false,
                barColor: '#48D1CC',
                barBgColor: '#eee',
                barWidth: 2,
                initValue: 0,
                roundCorner : false,
                percentage: false
            });
        }

        thisInstallBtn.addClass("inactive");

        thisInstallBtn.click(function(e) {
            e.stopPropagation();
            console.log('click on installBtn');
            if ($(this).hasClass('downloading')) {
                console.log('downloading, ignore download request...');
                return;
            }
            if ($(this).attr("data-installed") == "YES") {
                if (window.android) {
                    showLoader("请稍候...");
                    setTimeout("hideLoader()", 2000);
                    console.log('start app '+$(this).data("pkgname"));
                    window.android.startAPP($(this).data("pkgname"));
                } else {
                    showLoader("只能在手机中打开");
                    setTimeout("hideLoader()", 2000);
                }
                return;
            }
            if ($(this).attr("data-downloaded") == "YES") {
                console.log('downloaded, ignore download request...');
                return;
            }
        });
    },

    appDetailTemplate : function(data)
    {
        var arrHtml  = new Array();
        arrHtml.push(me.appIntroTemplate(data));

        arrHtml.push("<div class='swiper-container'><div class='swiper-wrapper'>");
        for (var i = 0; i < data.ImageSrcList.length; i++) {
          arrHtml.push("<div class='swiper-slide'><img src='" + data.ImageSrcList[i] + "' alt=''/></div>");
        }
        arrHtml.push("</div><div class='swiper-pagination'></div></div>");
        arrHtml.push(me.descriptionTemplate(data));
        return arrHtml.join("");
    },

    appIntroTemplate : function (data)
    {
        var isAppInstalled = false;
        if (window.android != undefined && window.android.isAppInstalled(data.PackageName, 1)) {
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
        // arrHtml.push("<br>");
        // arrHtml.push("<span class=\"score-star\">");
        // arrHtml.push("<span style=\"width: " + data.AppScore + "%;\">");
        // arrHtml.push("</span>");
        // arrHtml.push("</span>");
        // arrHtml.push("<br>");
        arrHtml.push("<div class=\"download_size\">");
        arrHtml.push("<span>");
        // var size = parseFloat(data.AppSize/1000000).toFixed(1) + "MB";
        arrHtml.push("v" + subString.autoAddEllipsis(data.AppVersion, 10, false) + "&nbsp;|&nbsp;" + data.AppSize);
        arrHtml.push("</span>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");

        arrHtml.push("</div>");
        // var gaAppName = data.AppName.replace(/\"/g, "”").replace(/'/g, "’");

        arrHtml.push("<div id=\"divdownarea\" class=\"down-area\">");
        arrHtml.push("<div class=\"content-btn-con\">");
        arrHtml.push("<a class=\"DownloadBtn\" data-appurl=\""+data.AppSource+"\" data-appname=\""+data.AppName+"\" data-appid=\""+data.AppId+"\" data-pkgname=\""+data.PackageName+"\" data-applogo=\""+data.AppLogo+"\" ");
        if (isAppInstalled) {
            arrHtml.push("data-installed='YES' >已安装</a>");
        } else {
            arrHtml.push("data-installed='NO' >安装</a>");
        }
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</section>");

        return arrHtml.join("");
    },

    descriptionTemplate : function (data)
    {
        var arrHtml = new Array();
        arrHtml.push("<section class=\"description\">");
        arrHtml.push("<div class=\"content-navdes-wrapper\">");
        arrHtml.push("<div class=\"des-main\">");
        // arrHtml.push("<div class=\"des-indent des-short\">");

        arrHtml.push("<div class=\"des-long-content\">");
        // arrHtml.push("<p>" + data.BriefSummary + "</p>");
        arrHtml.push("<p>" + data.AppSummary.replace(/\r\n/g,"<br/>").replace(/\\n/g,"<br/>").replace(/\n/g,"<br/>") + "</p>");
        arrHtml.push("</div>");
        // arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</div>");
        arrHtml.push("</section>");
        return arrHtml.join("");
    },

    //获取查询参数
    parseQueryString : function ()
    {
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

    parseMessages : function (res)
    {
        var data = res.broadcastlist;
        if (data == null || data == undefined) {
            return;
        }

        var arrHtml = new Array();

        if (data.length > 0) {
            $(".message_grid").show();
        }

        for (var i = 0; i < data.length; i++) {
            arrHtml.push("<li><p>"+data[i].broadcast_item+"</p></li>");
        }
        var html = arrHtml.join('');
        $("#twitter").append(html);
    },

    showMessage : function ()
    {
        
        if ($("#twitter li").length > 1) {
            $("#twitter li:not(:first)").css("display","none");
            var B = $("#twitter li:last");
            var C = $("#twitter li:first");
            setInterval(function() {
                if (B.is(":visible")) {
                    C.fadeIn(500).addClass("in");
                    B.hide()
                } else {
                    $("#twitter li:visible").addClass("in");
                    $("#twitter li.in").next().fadeIn(500);
                    $("li.in").hide().removeClass("in");
                }
            },5000); // 切换间隔
        }
    },

    requestVerifyCode : function()
    {
        if ($(".verifyCodeBtn").hasClass("text_disabled")) {
            console.log("Please wait...");
            return;
        }

        var phone_number = $("#registPhoneNumber").val();
        if (phone_number == '' || phone_number == '请填写您的手机号' || !isPhoneNumber(phone_number)) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return;
        }

        // workaround the sms failure
        // setTimeout("receivedVerifyCode('0904')", 1000);
        // return;
        // end

        var url = appServerUrl+"/appverifycode?"+callback+"&phone_number="+phone_number;
        console.log(url);
        $.getJSON(url, function(data) {
            if (data.ret_code == 0) {
                showLoader("验证码已通过短信发送");
                setTimeout("hideLoader()", 2000);
                $(".verifyCodeBtn").addClass("text_disabled");
                me.countDownSeconds = 120;
                setTimeout("me.countDown()", 1000);
                $(".verifyCodeBtn").attr("disabled","disabled");
                if (window.android) {
                    window.android.startVerifyCodeObserver();
                }
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 3000);
            }
        }
    )},

    countDown : function()
    {
        $(".verifyCodeBtn").text(me.countDownSeconds + "秒");
        me.countDownSeconds = me.countDownSeconds - 1;
        if (me.countDownSeconds <= 0) {
            me.resetCountDown();
        } else {
            countDownTimer = setTimeout("me.countDown()", 1000);
        }
    },

    resetCountDown : function ()
    {
        if (countDownTimer != null) {
            clearTimeout(countDownTimer);
            countDownTimer = null;
        }
        $(".verifyCodeBtn").removeClass("text_disabled").text("获取验证码");
        $(".verifyCodeBtn").attr("disabled","");
    },

    validLogin : function()
    {
        if ($("#loginUsername").val()=='' || $("#loginUsername").val()=='请填写您的手机号' || !isPhoneNumber($("#loginUsername").val())) {
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

    validRegist : function()
    {
        if ($("#registPhoneNumber").val()=='' || $("#registPhoneNumber").val()=='请填写您的手机号' || !isPhoneNumber($("#registPhoneNumber").val())) {
            showLoader("请填写手机号");
            setTimeout("hideLoader()", 2000);
            return false;
        }
        if ($("#registVerifyCode").val()=='') {
            showLoader("请填写验证码");
            setTimeout("hideLoader()", 2000);
            return false;
        }

        if (me.isChangingPassword) {
            if ($("#registPassword").val()=='') {
                showLoader("请填写密码");
                setTimeout("hideLoader()", 2000);
                return false;
            }
            if ($("#registPassword").val().length>16) {
                showLoader("密码长度不能超过16位");
                setTimeout("hideLoader()", 2000);
                return false;
            }
            var filter=/[`~!@#$^&*()\-\+=|\\\[\]\{\}:;'\,.<>/?]/;
            if (filter.test($("#registPassword").val())) {
                showLoader("密码只能包含字母、数字和下划线");
                setTimeout("hideLoader()", 2000);
                return false;
            }
            if ($("#repeatPassword").val()!=$("#registPassword").val()) {
                showLoader("两次输入的密码不一致");
                setTimeout("hideLoader()", 2000);
                return false;
            }
        } else {
            if ($("#inviteCode").val().length > 6) {
                showLoader("邀请码无效");
                setTimeout("hideLoader()", 2000);
                return false;
            }            
        }
        return true;
    },

    register : function ()
    {
        if (me.validRegist()) {
            var phone_number = $("#registPhoneNumber").val();
            var verify_code  = $("#registVerifyCode").val();
            var inviteCode   = $("#inviteCode").val();
            if (me.isChangingPassword) {
                var passwd    = $("#registPassword").val();
                var passwdMD5 = CryptoJS.MD5(passwd, { asString: true });
                var url = appServerUrl+"/reset_passwd?"+callback+"&phone_number="+phone_number+"&new_passwd="+passwdMD5+"&verify_code="+verify_code;
            } else {
                var passwd    = phone_number.substr(5, 6); // get phone number last 6 digit
                var passwdMD5 = CryptoJS.MD5(passwd, { asString: true });
                var url = appServerUrl+"/appregister?"+callback+"&phone_number="+phone_number+"&passwd="+passwdMD5+"&verify_code="+verify_code;
                if (inviteCode && inviteCode.length > 0 && inviteCode != "选填") {
                    url = url + "&invite_code=" + inviteCode;
                }
                saveItem("userName", phone_number);
                saveItem("passWord", passwd);
            }
            console.log(url);

            $.getJSON(url, function(data) {
                me.resetCountDown();  // 重置验证码倒计时
                if (data.ret_code == 0) {
                    if (me.isChangingPassword == false) {
                        me.saveToken(data.token);
                        showLoader("验证成功");
                    } else {
                        showLoader("密码修改成功");
                        saveItem("userName", phone_number);
                        saveItem("passWord", passwd);
                    }

                    if (data.coin_num == undefined) {
                        data.coin_num = 0;
                    }
                    if (data.invite_code == undefined) {
                        $("#myInviteCode").hide();
                    }

                    $("#coin").text(data.coin_num);

                    setTimeout("changePageAndHideLoader(\"#MainPage\")", 2000);
                    $("#account").text(phone_number);
                    $("#myInviteCode").text(data.invite_code);

                } else {
                    showLoader(data.ret_msg);
                    setTimeout("hideLoader()", 3000);
                }
            });
        }
    },

    autoLogin : function()
    {
        console.log("autoLogin");
        clearTimeout(autoLoginTimer);

        if (me.isLogin) {
            return;
        }
        if (me.autoLoginRetryCount > 4) {
            console.log("reach max auto login retry count, abort.");
            me.autoLoginRetryCount = 0;
            return;
        }
        console.log("autoLogin retry count:"+me.autoLoginRetryCount);
        var phone_number = getItem("userName");
        var passwd       = getItem("passWord");
        if (phone_number == undefined || phone_number == null || phone_number.length == 0) {
            changePage("#RegisterPage");
            return;
        }
        var passwdMD5    = CryptoJS.MD5(passwd, { asString: true });
        var url = appServerUrl+"/applogin?"+callback+"&phone_number="+phone_number+"&passwd="+passwdMD5;
        console.log(url);

        $.ajax({
            type: "GET",
            url: url,
            dataType : "jsonp",
            jsonp: "callback",//"callname",//服务端用于接收callback调用的function名的参数
            success : function(data) {
                        me.autoLoginRetryCount = 0;
                        if (data.ret_code == 0) {
                            me.isLogin = true;
                            me.saveToken(data.token);
                            // changePage("#MainPage");
                            console.log("login success, coin num:" + data.coin_num);
                            if (data.coin_num == undefined) {
                                data.coin_num = 0;
                            }

                            $("#account").text(phone_number);
                            $("#myInviteCode").text(data.invite_code);
                            $("#coin").text(data.coin_num);

                            if (parseInt($("#connectWifiBtn").attr("data-wifiStatus")) == WifiStatus.kulianAuthed) {
                                me.reportAuthenSuccess();
                            }

                            me.reportConnection(phone_number);
                        } else {
                            console.log("auto login:"+data.ret_msg);
                            changePage("#RegisterPage");
                        }
                    },
            error : function() {
                        console.log("autoLogin network fail.");
                        autoLoginTimer = setTimeout(me.autoLogin(), 2000);
                        me.autoLoginRetryCount++;
                    }
        });
    },

    reportConnection : function(phone_number)
    {
        if (window.android == undefined || phone_number == undefined) {
            return;
        }
        // data内容，utf8，json编码， 
        // mm，手机mac 
        // wm，wifimac 
        // mn，手机号码 
        // tm，时间戳
        var macAddr = window.android.getMacAddress();
        var BSSID = window.android.getBSSID();
        if (macAddr == undefined || BSSID == undefined) {
            console.log("reportConnection error : mac["+macAddr+"] BSSID["+BSSID+"]");
            return;
        }
        var time = parseInt(new Date().getTime()/1000);
        var data = {"mm":macAddr, "mn":phone_number, "tm":time, "wm":BSSID};
        var url = "http://115.159.89.152:1220/?name=appmm&opt=put&data="+jsonToString(data)+"&auth=hongkulian&"+callback;
        console.log(url);
        $.ajax({
            type: "GET",
            url: url,
            dataType : "jsonp",
            jsonp: "callback",
            success : function(data) {},
            error : function() {}
        });
    },

    requestExchange : function(obj)
    {
        var myCoin = parseInt($("#coin").text());
        var needCoin = $(obj).data("coin");
        if (myCoin < needCoin) {
            showLoader("您的金币数不足");
            setTimeout("hideLoader()", 3000);
            return;
        }
        var type = $(obj).data("exchangetype");
        var phone_number = $(".acount_list #account").text();
        var url = appServerUrl+"/exchange?"+callback+"&phone_number="+phone_number+"&coin="+needCoin+"&exchange_type="+type;
        console.log(url);
        showLoader();

        $.getJSON(url, function(data) {
            if (data.ret_code == 0) {
                showLoader("话费兑换申请已提交，将在两个工作日内充值到您手机号码内");
                $("#coin").text(data.coin_num);// update coin num
                setTimeout("changePageAndHideLoader(\"#MainPage\")", 3000);
            } else {
                showLoader(data.ret_msg);
                setTimeout("hideLoader()", 3000);
            }
        });
    },

    saveToken : function(token)
    {
        $.cookie("token", token, {expires:10});
    },

    getVersion : function()
    {
        if (version == null) {
            if (window.android != undefined) {
                version = window.android.getVersion();
            } else {
                version = '1.0build0';
            }
        }
        return version;
    },

    fillVersion : function()
    {
        $("#version").text(me.getVersion());
    },

    showAppTab : function (tabIdx) {
        me.curAppTabIdx = tabIdx;
    },

    refreshScroll : function (idx) {
        if (idx == undefined) {
            idx = me.curAppTabIdx;
        }
        if (me.myScroll && me.myScroll[idx]) {
            me.myScroll[idx].refresh();
        }
    },
    
    initIScroll : function (idx) {
        console.log("initIScroll");
        if (me.myScroll == null) {
            me.myScroll = new Array(5);
        }

        var upIcon = $("#tab-"+idx+" .up-icon");
            // var downIcon = $("#tab-"+me.curAppTabIdx+" .down-icon");

        if (me.myScroll[idx] != null) {
            me.myScroll[idx].destroy();
        }
        me.myScroll[idx] = new IScroll("#tab-"+idx+" .wrapper", 
                                {click:true, probeType: 3, mouseWheel: true, fadeScrollbars: true }
                                );

        me.myScroll[idx].on("scroll",function() {
            var y = this.y,
                maxY = this.maxScrollY - y,
                // downHasClass = downIcon.hasClass("reverse_icon"),
                upHasClass = upIcon.hasClass("reverse_icon");
            
            // if(y >= 40){
            //     !downHasClass && downIcon.addClass("reverse_icon");
            //     return "";
            // }else if(y < 40 && y > 0){
            //     downHasClass && downIcon.removeClass("reverse_icon");
            //     return "";
            // }
            
            if (maxY >= 40) {
                !upHasClass && upIcon.addClass("reverse_icon");
                return "";
            } else if (maxY < 40 && maxY >=0) {
                upHasClass && upIcon.removeClass("reverse_icon");
                return "";
            }
        });
        
        me.myScroll[idx].on("slideDown",function(){
            if (this.y > 40) {
                // alert("slideDown");
                upIcon.removeClass("reverse_icon")
            }
        });
        
        me.myScroll[idx].on("slideUp",function(){
            if (this.maxScrollY - this.y > 40) {
                // me.requestAppTypePage(me.curAppTabIdx, me.curAppPageIdx[me.curAppTabIdx]);
                upIcon.removeClass("reverse_icon");
            }
        });
        setTimeout(me.myScroll[idx].refresh(), 300);
    }
}; // end of var me
