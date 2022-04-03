package hcf.seckill.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author hechaofan
 * @date 2022/4/3 18:20
 */
public interface LoginService {

        public boolean login(HttpServletResponse response, HttpSession session, Object loginVo);

        public void passTrans(Object loginVo);
    }
