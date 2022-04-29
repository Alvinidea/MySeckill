package hcf.seckill.service;

import hcf.seckill.dto.Login.LoginResult;
import hcf.seckill.dto.Login.UserVo;

/**
 * @author hechaofan
 * @date 2022/4/3 18:20
 */
public interface LoginService {

    public LoginResult login(UserVo userVo);

    public LoginResult register(UserVo userVo);
}
