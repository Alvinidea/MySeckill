package hcf.seckill.controller;

import hcf.seckill.dto.Exposer;
import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.dto.SeckillResult;
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
import org.springframework.web.bind.annotation.*;

/**
 * @author hechaofan
 * @date 2022/3/21 12:28
 * 进行秒杀优化，
 *
 */

@Controller
@RequestMapping(value = "/V2Seckill")
public class OptSeckillController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private OptSeckillService optSeckillService;

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
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
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
    @RequestMapping(value = "/{seckillId}/{md5}/V2execution"
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
            result = new SeckillResult<SeckillExecution>(true, execution);
        }
        catch (Exception e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<SeckillExecution>(true, execution);
        }
        return result;
    }
}
