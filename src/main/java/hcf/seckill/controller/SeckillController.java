package hcf.seckill.controller;

import hcf.seckill.dto.Exposer;
import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.dto.SeckillResult;
import hcf.seckill.entity.Seckill;
import hcf.seckill.enums.SeckillStateEnum;
import hcf.seckill.exception.RepeatKillException;
import hcf.seckill.exception.SeckillCloseException;
import hcf.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/3/18 10:51
 */

/**
 * 基础阶段：
 *      实现DB design + DAO + Service + Controller
 * /hcf.seckill/list
 * /hcf.seckill/{seckillId}/detail
 * /hcf.seckill/{seckillId}/exposer
 * /hcf.seckill/{seckillId}/{md5}/execution
 * /hcf.seckill/time/now
 */
@Controller
@RequestMapping("/seckill")
//url:/模块/资源/{id}/细分
public class SeckillController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value="/list", method = RequestMethod.GET)
    public String list(Model model){
        //list.jsp + model = ModelAndView
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        return "list";  //等同于 WEB-INF/jsp/list.jsp
    }

    @RequestMapping(value="/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(
            @PathVariable("seckillId") Long seckillId
            , Model model){
        if(seckillId == null){
            return "redirect:/hcf.seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if(seckill == null){
            return "forward:/hcf.seckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "detail";
    }

    //ajax json
    @RequestMapping(
            value = "/{seckillId}/exposer",
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(Long seckillId){
        SeckillResult<Exposer> seckillResult;
        try{
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            seckillResult = new SeckillResult<Exposer>(true, exposer);
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            seckillResult = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return seckillResult;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution"
            ,method = RequestMethod.POST
            ,produces = {"application/json;charset=utf-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(
                @PathVariable("seckillId") Long seckillId
                ,@PathVariable("md5") String md5
                ,@CookieValue(value = "killPhone", required = false) Long phone){ // 从Cookie中获取数据
        // 验证phone或者spring MVC验证（此处没有）
        if(phone == null){
            return new SeckillResult<SeckillExecution>(false, "未注册");
        }
        SeckillResult<SeckillExecution> result;
        try{
            // SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
            // 存储过程调用
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId, phone, md5);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(RepeatKillException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(SeckillCloseException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch (Exception e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }
       return result;
    }

    @RequestMapping(value = "/time/now", method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time(){
        Date now = new Date();
        return new SeckillResult<Long>(true, now.getTime());
    }
}
