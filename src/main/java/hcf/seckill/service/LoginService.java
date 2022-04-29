package hcf.seckill.service;

import hcf.seckill.dto.Login.LoginResult;
import hcf.seckill.dto.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author hechaofan
 * @date 2022/4/3 18:20
 */
public interface LoginService {

    public LoginResult login(UserVo userVo);

    public LoginResult register(UserVo userVo);
}
