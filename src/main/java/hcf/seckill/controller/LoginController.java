package hcf.seckill.controller;

import hcf.seckill.dto.Login.LoginResult;
import hcf.seckill.dto.Login.UserVo;
import hcf.seckill.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private LoginService loginService;

    @RequestMapping("register")
    public String registerView(Model model){
        model.addAttribute("count", 0);
        return "html/register";
    }

    @RequestMapping("/toRegister")
    @ResponseBody
    public LoginResult toRegister(UserVo userVo)
    {        // 获取当前用户
        LoginResult result = loginService.register(userVo);
        return result;
    }


    @RequestMapping("login")
    public String loginView(Model model){
        model.addAttribute("count", 0);
        return "html/login";
    }

    @RequestMapping("/toLogin")
    @ResponseBody
    public LoginResult toLogin(UserVo userVo)
    {        // 获取当前用户
        LoginResult result = loginService.login(userVo);
        return result;
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
