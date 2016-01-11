/**
 * @author HC
 * @param  {jQuery} $      [description]
 * @param  {window} window [description]
 * @return {object}        [description]
 * var dialog = H.Dialog;
 * var a = dialog({
 * 		xx:xx
 * })
 */
;(function($,window){
	//定义基础方法
	Function.prototype.method = function(name,fn){
		this.prototype[name] = fn;
		return this;
	};

	var dialog = function(option){
		//定义dialog对象
		var D = function(option){
			this.init(option);
		};
		//定义dialog的方法
		D.method('init',function(option){
			var _this = this,
				defaults = {
					width: '260',
					height: '200',
					title: '提示信息',
					titlePostion: 'center',
					content: '',
					closeBtn: false,
					closeCallback: null,
					ok: true,
					okCallback: null
				};
			var settings = $.extend(true, defaults, option || {});
			this.render(settings);
		});
		D.method('render',function(settings){
			var _this = this;
			//创建dom
			this.createDom(settings);
			//绑定事件
			$('#dialog-ok').on('click',function(event) {
				settings.okCallback && settings.okCallback();
				_this.destroy();
			});
		});
		D.method('createDom',function(settings){
			var mask = '<div class="dialog-mask" style="position: fixed;width: 100%;height: 100%;top: 0; left: 0;z-index: 1000;background: rgba(0,0,0,0.4);"></div>',
				dialogDom = '<div id="dialog-body" class="dialog-body" style="position: fixed;width: 0;height: 0;top: 50%;left: 50%;overflow: hidden;background: #fff;z-index: 1001;border-radius: 8px;transition: all 0.4s;">'+
						'<h2 class="dialog-title" style="text-align: '+settings.titlePostion+';padding: 5px 0;background: #ccc;color:#666;">'+settings.title+'</h2>'+
						'<div class="dialog-content" style="width: 96%; height: '+(settings.height-75)+'px;;padding: 2%;color: #666;overflow: auto;word-break: break-all;">'+settings.content+'</div>'+
						'<div class="dialog-btn-group" style="text-align: center;padding: 5px;">'+
							'<button id="dialog-ok" class="dialog-ok" style="display: '+(settings.ok?'inline-block':'none')+';border: 0; padding: 3px 10px;color:#666;">确定</button>'+
						'</div>'+
					'</div>';
			//append到页面
			$('body')
				.append(mask)
				.append(dialogDom);
			setTimeout(function(){
				$('#dialog-body').css({
					'width': settings.width,
					'height': settings.height,
					'marginTop': '-' + settings.height/2 +'px',
					'marginLeft': '-' + settings.width/2 +'px'
				});
			},100);
		});
		D.method('destroy',function(){
			$('.dialog-mask').remove();
			$('#dialog-body').remove();
		});

		return new D(option);
	};

	var guideMask = function(option){
		//创建guidemask对象
		var GM = function(option){
			this.init(option);
		};
		//声明guidemask对象的方法
		GM.method('init',function(option){
			var settings = option;
			this.render(settings);
		});
		GM.method('render',function(settings){
			var _this = this;
				_this.createDom(settings);

			$('#guideMask').on('click', function(event) {
				_this.destroy();
			});
		});
		GM.method('createDom',function(settings){
			var dom = '<div id="guideMask" class="mask-c" style="position: fixed;width: 100%;height: 100%; background-color: rgba(0,0,0,0.75);z-index: 2000;">' +
						'<div class="tl-h">' +
							'<img src="' + ( settings.signImgSrc || '' ) + '">' +
						'</div>' +
						'<div class="dc-h">' +
							'<img src="' + ( settings.downloadImgSrc || '' ) + '">' +
						'</div>' + 
					'</div>';
			$('body').prepend(dom);
		});
		GM.method('destroy',function(){
			$('#guideMask').length && $('#guideMask').remove();
		});

		return new GM(option);
	};

	window.H = {
		Dialog : dialog,
		GuideMask : guideMask 
	};
})(jQuery,window);