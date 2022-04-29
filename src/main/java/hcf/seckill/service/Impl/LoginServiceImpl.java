package hcf.seckill.service.Impl;


import hcf.seckill.dao.IUserDao;
import hcf.seckill.dto.Login.LoginResult;
import hcf.seckill.dto.UserVo;
import hcf.seckill.entity.IUser;
import hcf.seckill.service.LoginService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Service
public class LoginServiceImpl implements LoginService {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LoginServiceImpl.class);

    @Autowired
    private IUserDao iUserDao;

    public LoginResult login(UserVo userVo)
    {
        Subject subject = SecurityUtils.getSubject();
        // 封装用户的登录数据
        UsernamePasswordToken token = new UsernamePasswordToken(
                String.valueOf(userVo.getUserPhone()),
                userVo.getUserPwd());
        try{
            subject.login(token);
        }catch (UnknownAccountException e){
            logger.error(e.getMessage());
            return new LoginResult(2, "err");
        }catch (IncorrectCredentialsException e){
            logger.error(e.getMessage());
            return new LoginResult(3, "err");
        }
        // subject.logout();
        return new LoginResult();
    }

    public LoginResult register(UserVo userVo){
        IUser iUser = new IUser();
        iUser.setUserPhone(userVo.getUserPhone());
        iUser.setUserName("default");
        iUser.setUserPwd(userVo.getUserPwd());
        iUser.setUserAuth(1);
        iUser.setUserDescription("fisher");
        int ret = iUserDao.createUser(iUser);
        if(ret != 1){
            new LoginResult(21, "register error");
        }
        return new LoginResult(20, "register OK");
    }
}
