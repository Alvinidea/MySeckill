package hcf.seckill.controller;

import hcf.seckill.dto.Exposer;
import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.dto.SeckillResult;
import hcf.seckill.entity.Seckill;
import hcf.seckill.enums.SeckillStateEnum;
import hcf.seckill.exception.RepeatKillException;
import hcf.seckill.exception.SeckillCloseException;
import hcf.seckill.exception.SeckillException;
import hcf.seckill.service.OptSeckillService;
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
 * @date 2022/3/21 12:28
 * 进行秒杀优化，
 *
 */

@Controller
@RequestMapping(value = "/optSeckill")
public class OptSeckillController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private OptSeckillService optSeckillService;

    @RequestMapping(value="/list", method = RequestMethod.GET)
    public String list(Model model){
        //list.jsp + model = ModelAndView
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        return "jsp/optList";  //等同于 WEB-INF/jsp/list.jsp
    }

    @RequestMapping(value="/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(
            @PathVariable("seckillId") Long seckillId
            , Model model){
        if(seckillId == null){
            return "redirect:/optSeckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if(seckill == null){
            return "forward:/optSeckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "jsp/optDetail";
    }

    @RequestMapping(value="/{seckillId}/sDetail", method = RequestMethod.GET)
    public String sDetail(
            @PathVariable("seckillId") Long seckillId
            , Model model){
        if(seckillId == null){
            return "redirect:/optSeckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if(seckill == null){
            return "forward:/optSeckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "html/sDetail";
    }
    /***
     * 获取秒杀接口
     * @param seckillId
     * @return
     */
    //ajax json
    @RequestMapping(
            value = "/{seckillId}/exposer",
            method = RequestMethod.POST,
            produces = {"application/json;charset=utf-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(Long seckillId){
        SeckillResult<Exposer> seckillResult;
        try{
            Exposer exposer = optSeckillService.exportSeckillUrl(seckillId);
            seckillResult = new SeckillResult<Exposer>(true, exposer);
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            seckillResult = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return seckillResult;
    }

    /***
     * 进行秒杀
     * @param seckillId
     * @return
     */
    @RequestMapping(value = "/{seckillId}/{md5}/execution"
            ,method = RequestMethod.POST
            ,produces = {"application/json;charset=utf-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId
            , @PathVariable("md5")String md5
            , @CookieValue(value = "killPhone", required = false) Long phone){
        if (phone == null) {
            return new SeckillResult<SeckillExecution>(false, "未登录");
        }
        SeckillResult<SeckillExecution> result;
        try{
            SeckillExecution execution = optSeckillService.executeSeckill(seckillId, phone, md5);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(RepeatKillException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(SeckillCloseException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            result = new SeckillResult<SeckillExecution>(true, execution);
        } catch (SeckillException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<SeckillExecution>(true, execution, e.getMessage());
        }
        catch (Exception e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }
        return result;
    }

    /**
     * executeSeckill 方法的Jmeter测试版本，
     *      * 便于测试，取消了重复秒杀的判断，用户秒杀信息在 success_killed的存储
     * @param seckillId
     * @param md5
     * @param phone
     * @return
     */
    @RequestMapping(value = "/{seckillId}/{md5}/{killPhone}/execution"
            ,method = RequestMethod.POST
            ,produces = {"application/json;charset=utf-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> testExecute(
            @PathVariable("seckillId") Long seckillId
            , @PathVariable("md5") String md5
            , @PathVariable("killPhone") Long phone){
        if (phone == null) {
            return new SeckillResult<SeckillExecution>(false, "未登录");
        }
        SeckillResult<SeckillExecution> result;
        try{
            SeckillExecution execution = optSeckillService.executeSeckillForJmeter(seckillId, phone, md5);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(RepeatKillException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }catch(SeckillCloseException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            result = new SeckillResult<SeckillExecution>(true, execution);
        } catch (SeckillException e){
            logger.info(e.getMessage());
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<SeckillExecution>(true, execution, e.getMessage());
        }
        catch (Exception e){
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
