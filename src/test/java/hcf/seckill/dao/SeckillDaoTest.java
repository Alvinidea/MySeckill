package hcf.seckill.dao;

import hcf.seckill.entity.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.util.Date;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/3/17 12:45
 */

// 使用IOC容器
@RunWith(SpringJUnit4ClassRunner.class)
// 告诉junit spring配置文件
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {

    @Resource
    // @Autowired
    private SeckillDao seckillDao;

    @Test
    public void queryById() {
        long id = 1;
        Seckill seckill = seckillDao.queryById(id);
        System.out.println(seckill.getName());
        System.out.println(seckill);
    }

    /**
     * Caused by: org.apache.ibatis.binding.BindingException:
     * Parameter 'offset' not found. Available parameters are [0, 1, param1, param2]
     *    java没有保存形参的表述，所以当超过两个参数时候就需要进行取别名
     *    List<Seckill> queryAll(int offset , int limit);
     *    可以看作是
     *    List<Seckill> queryAll(int arg0 , int arg1);
     *    所以，需要强制的为形参命名：
     *    List<Seckill> queryAll(@Param("offset") int offset
     *                           , @Param("limit") int limit);
     *                           gaos
     */
    @Test
    public void queryAll() {
        List<Seckill> seckills = seckillDao.queryAll(0,100);
        for(Seckill seckill : seckills){
            System.out.println(seckill);
        }
    }

    @Test
    public void reduceNumber() {
        int updateTime = seckillDao.reduceNumber(1L, new Date());
        System.out.println("updateCount = "+updateTime);
    }


}