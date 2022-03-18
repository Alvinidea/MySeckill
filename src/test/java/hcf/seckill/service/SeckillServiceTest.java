package hcf.seckill.service;

import hcf.seckill.dto.Exposer;
import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.entity.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author hechaofan
 * @date 2022/3/17 16:36
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
        "classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"})

public class SeckillServiceTest {
    // slf4j基础是logback
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SeckillService seckillService;
    @Test
    public void getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        logger.info("list={}",list);
    }

    @Test
    public void getById() {
        long id = 1L;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckill={}",seckill);
    }

    @Test
    public void exportSeckillUrl() {
        long id = 3L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        logger.info("exposer={}",exposer);
        /*
        * Exposer{
        *   exposed=true,
        *   md5="63b2b37d08af3af84b929c67038e2a93",
        *   seckillId=3,
        *   now=0,
        *   start=0,
        *   end=0
        * }
        * */
    }

    @Test
    public void executeSeckill() {
        long seckillId = 3L;
        long phone = 18483672600L;
        String md5 = "63b2b37d08af3af84b929c67038e2a93";
        SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
        logger.info(execution.getStateInfo());

/*        long seckillId = 1000L;
        long phone = 18483672600L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if (exposer.isExposed()){
            String md5 = exposer.getMd5();
            SeckillExecution excution = seckillService.executeSeckill(seckillId,phone,md5);
            logger.info(excution.getStateInfo());
        }*/
    }
}