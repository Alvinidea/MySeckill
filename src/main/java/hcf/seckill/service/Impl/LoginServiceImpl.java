package hcf.seckill.service.Impl;


import hcf.seckill.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Service
public class LoginServiceImpl implements LoginService {


    public boolean login(HttpServletResponse response, HttpSession session, Object loginVo)
    {
        return true;
/*        if(loginVo == null)
            return false; // 应该抛出异常
        String  mobile = loginVo.getMobile();
        String password= loginVo.getPassword();
        User user = userMapper.getByNickname(mobile);
        if( user == null)
            return false; // 应该抛出异常
        String dbPwd = user.getPassword();
        String saltDb= user.getSalt();
        String calcPass = MD5Utils.formPassToDBPass(password,saltDb);
        if(!calcPass.equals(dbPwd) )
            return false;
        *//*
        * 1. 生成cookie 将session返回游览器 分布式session
        * （这儿做了简化，不考虑分布式情况）
        * *//*
        session.setAttribute("user", user);
        return true;*/
    }

    public void passTrans(Object loginVo)
    {
/*        if(loginVo == null)
            return; // 应该抛出异常
        String  mobile = loginVo.getMobile();
        String password= loginVo.getPassword();
        User user = userMapper.getByNickname(mobile);
        if( user == null)
            return; // 应该抛出异常
        String dbPwd = user.getPassword();
        String saltDb= user.getSalt();
        String calcPass = MD5Utils.formPassToDBPass(password,saltDb);
        loginVo.setPassword(calcPass);*/
    }
}
