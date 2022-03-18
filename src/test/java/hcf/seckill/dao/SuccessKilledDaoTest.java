package hcf.seckill.dao;

import hcf.seckill.entity.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author hechaofan
 * @date 2022/3/17 14:05
 */

// 使用IOC容器
@RunWith(SpringJUnit4ClassRunner.class)
// 告诉junit spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {

    @Resource
    private SuccessKilledDao successKilledDao;

    @Test
    public void insertSuccessKilled() {
        long id = 2L;
        long phone = 18483672600L;
        int insertCnt = successKilledDao.insertSuccessKilled(id, phone);
        System.out.println("insertCnt = "+ insertCnt);
        /**
         * 首次插入：
         *      insertCnt = 1
         * 二次插入：
         *      insertCnt = 0
         */
    }

    @Test
    public void queryByIdWithSeckill() {
        long id = 1L;
        long phone = 18483672600L;
        SuccessKilled successKilled= successKilledDao.queryByIdWithSeckill(id, phone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
        /**
         * SuccessKilled{
         *  seckillId=1000,
         *  userPhone=18483672600,
         *  state=-1,
         *  createTime=Thu Mar 17 22:23:44 CST 2022}
         *
         * Seckill{
         *  seckillId=1000,
         *  name='1000秒杀iPhone13',
         *  number=999,
         *  startTime=Thu Mar 17 22:02:02 CST 2022,
         *  endTime=Fri Mar 18 08:00:00 CST 2022,
         *  createTime=Thu Mar 17 02:19:39 CST 2022}
         */
    }
}