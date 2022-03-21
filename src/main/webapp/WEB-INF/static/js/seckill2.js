//主要用于存放交互逻辑的js代码
//javascript模块化

var seckill= {
    //封装秒杀相关ajax的url
    URL :{
        now:function () {
            return '/seckill/time/now';
        },
        exposer:function(seckillId){
            return '/seckill/'+seckillId+"/exposer";
        },
        execution :function (seckillId, md5) {
            return '/seckill/'+seckillId+'/'+md5+'/execution';
        }
    },

    // 验证手机号信息
    validatePhone: function (phone) {
        if (phone && phone.length == 11 && !isNaN(phone)) {
            return true;
        } else {
            return false;
        }
    },
    handleSeckill : function(seckillId, node) {
        // 获取秒杀地址，控制现实逻辑，执行秒杀
        node.hide().html(
            '<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');//按钮
        var params ={};
        params.seckillId = seckillId;
        $.post(seckill.URL.exposer(seckillId), params, function (result) {
            //在回调函数中，执行交互流程
            if (result && result['success']){
                var exposer = result['data'];
                if (exposer['exposed']){ //开启秒杀
                    //获取秒杀地址
                    var md5 = exposer['md5'];
                    var killUrl = seckill.URL.execution(seckillId,md5);
                    console.log("killURL:"+killUrl);//TODO
                    $('#killBtn').one('click',function(){
                        //绑定执行秒杀请求操作
                        //1、先禁用按钮
                        $(this).addClass('disabled');
                        //2、发送秒杀请求
                        var params = {};
                        params.seckillId =seckillId;
                        params.md5 = md5;
                        $.post(killUrl,params,function(result){
                            if (result && result['success']){
                                var killResult = result['data'];
                                var state = killResult['state'];
                                var stateInfo = killResult['stateInfo'];
                                //显示秒杀结果
                                node.html('<span class="label label-success">'+stateInfo+'</span>')
                            }
                        });
                    });
                    node.show();
                }
                else{ //未开启秒杀
                    var now = exposer['now'];
                    var start = exposer['start'];
                    var end = exposer['end'];
                    //重新进入即使逻辑
                    seckill.count(seckillId,now,start,end);
                }
            }
            else {
                console.log('result:'+result);//TODO
            }

        });
    },
    countdown : function(seckillId,nowTime,startTime,endTime){
        //时间判断
        var seckkillbox = $('#seckill-box');
        if (nowTime > endTime){ // 秒杀结束的情况
            //秒杀结束
            seckkillbox.html('秒杀结束');
        }
        else if (nowTime<startTime){ // 秒杀未开始的情况
            //秒杀未开始，
            var killTime = new Date(startTime + 1000);//加1秒，防止时间偏移
            // 计时事件绑定
            seckkillbox.countdown(killTime,function(event){ // 每次时间变化就会回调这个函数（进行时间的显示）
                //时间格式
                var format = event.strftime('秒杀倒计时：%D天 %H时 %M分 %S秒');
                seckkillbox.html(format);
                //时间完成后回调事件
            }).on('finish.countdown',function(){
                //获取秒杀地址，控制现实逻辑，执行秒杀
                seckill.handleSeckill(seckillId,seckkillbox);
            });
        }else{
            //秒杀开始
            seckill.handleSeckill(seckillId,seckkillbox);
        }

    },
    //秒杀详情页逻辑
    detail:{
        init : function (params){
            //用户手机验证和登陆，计时交互
            //规划我们的交互流程
            //在cookie中查找手机号
            var killPhone = $.cookie('killPhone');

            if(!seckill.validatePhone(killPhone)){
                //绑定手机号  //控制输出
                var killPhoneModal = $('#killPhoneModal');
                // 弹出层显示
                killPhoneModal.modal({ // bootstrap的 class=modal时候有的方法
                    show: true, //显示弹出层
                    backdrop: 'static', //禁止位置关闭
                    keyboard: false, //关闭键盘事件
                });
                $('#killPhoneBtn').click(function () {
                    var inputPhone = $('#killPhoneKey').val();
                    console.log(inputPhone)
                    if (seckill.validatePhone(inputPhone)) {
                        //电话写入cookie
                        $.cookie('killPhone', inputPhone, {expires: 7, path: '/seckill'});
                        //刷新界面
                        window.location.reload();
                    }else { // 提示信息错误
                        $('#killPhoneMessage').hide().html('<label class="label label-danger">手机号错误!</label>')
                            .show(300);
                    }
                });
            }
            // 计时交互
            var startTime = params['startTime'];
            var endTime = params['endTime'];
            var seckillId = params['seckillId'];
            $.get(seckill.URL.now(), {}, function (result) {
                if(result && result['success']){
                    var nowTime = result['data'];
                    console.log("nowTime = "+ nowTime);
                    //  =======================================================
                    //  时间判断(秒杀前、秒杀中、秒杀后)
                    seckill.countdown(seckillId,nowTime,startTime,endTime)
                    //  =======================================================
                }else{ // 未拿到数据则进行打印（方便调试）
                    console.log("result = " + result );
                }
            })
        }
    }
}