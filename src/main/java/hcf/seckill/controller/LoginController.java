package hcf.seckill.controller;

import hcf.seckill.dto.LoginVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
public class LoginController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/login")
    public Object login(HttpServletResponse response, HttpSession session, LoginVo loginVo, Model model)
    {
        // 获取当前用户
        Subject subject = SecurityUtils.getSubject();
        // 封装用户的登录数据
        UsernamePasswordToken token = new UsernamePasswordToken(
                String.valueOf(loginVo.getPhone()),
                loginVo.getPwd());
        try{
            subject.login(token);
        }catch (UnknownAccountException e){
            logger.info("userErr", e);
            return new Object();
        }catch (IncorrectCredentialsException e){
            logger.info("Pwd Err", e);
            return new Object();
        }
        subject.logout();
/*        *//* ========================================================================================================= *//*
        *//* Shiro 部分 *//*
        // 获取当前用户
        Subject subject = SecurityUtils.getSubject();
        loginService.passTrans(loginVo);
        // 封装用户的登录数据
        UsernamePasswordToken token = new UsernamePasswordToken(loginVo.getMobile(), loginVo.getPassword());

        loginService.login(response, session, loginVo);
        try{
            subject.login(token);
        }catch (UnknownAccountException e) {
            model.addAttribute("msg","UserErr");
            return MyResult.build();
        }catch (IncorrectCredentialsException e){
            model.addAttribute("msg", "Pwd Err");
            return MyResult.build();
        }

        *//* ========================================================================================================= *//*
        MyResult<Boolean> result = MyResult.build();
        session.setAttribute("user", loginVo.getMobile());
        // loginService.login(response, session, loginVo);
        return result;*/
        return null;
    }

/*
    @RequestMapping("/login")
    public MyResult<Boolean> login(HttpServletResponse response, HttpSession session, LoginVo loginVo, Model model)
    {
        MyResult<Boolean> result = MyResult.build();
        loginService.login(response, session, loginVo);
        return result;
    }*/
}
