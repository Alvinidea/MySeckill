package hcf.seckill.service;

import hcf.seckill.dao.IUserDao;
import hcf.seckill.entity.IUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * @author hechaofan
 * @date 2022/4/29 11:09
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
        "classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"
        })
public class LoginServiceTest {


    @Autowired
    private IUserDao iUserDao;

    @Test
    public void register() {
        IUser iUser = new IUser();
        iUser.setUserPhone(18483672600L);
        iUser.setUserName("hcf");
        iUser.setUserPwd("11111111");
        iUser.setUserAuth(1);
        iUser.setUserDescription("");
        // System.out.println(iUserDao.createUser(iUser));
        // System.out.println(iUserDao.queryUserById(1));
        iUser.setUserPwd("60e8d86a74a4bd215071b0777d4c8a13");
        System.out.println(iUserDao.updatePwd(iUser));
        System.out.println(iUserDao.queryPwd(iUser.getUserPhone()));
    }
}