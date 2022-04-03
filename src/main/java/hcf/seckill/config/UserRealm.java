package hcf.seckill.config;


import hcf.seckill.dto.LoginVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRealm extends AuthorizingRealm {

    /**
        1、subject.hasRole(“admin”) 或 subject.isPermitted(“admin”)：
                自己去调用这个是否有什么角色或者是否有什么权限的时候；
        2、@RequiresRoles(“admin”) ：
                在方法上加注解的时候；
        3、[@shiro.hasPermission name = “admin”][/@shiro.hasPermission]：
                在页面上加shiro标签的时候，即进这个页面的时候扫描到有这个标签的时候。
    */

    /**
     * 授权操作
     * */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        System.out.println("执行了 =》 授权 doGetAuthorizationInfo");
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addStringPermission("user:all");


        Subject subject = SecurityUtils.getSubject();       // 获取当前登录的这个对象，LoginController中登录的subject
        LoginVo currentUser = (LoginVo) subject.getPrincipal();   // 获取到User对象

        // 设置当前用户的权限
        // info.addStringPermission(currentUser.getPerms());
        // 本应该使用用户具有的权限，但是我的设计中最开始未考虑 权限，所以这儿直接写一个字符串 "user:all" 作为权限
        info.addStringPermission("user:all");

        return info;
        // return null;
    }

    /**
     * 认证操作
     * */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        System.out.println("执行了 =》 认证 doGetAuthenticationInfo");
        /* 可以链接数据库查找 */
        UsernamePasswordToken userToken = (UsernamePasswordToken) token;

        // 从数据库中获取数据
        LoginVo login = new LoginVo();
        login.setPhone(18483670000L);
        login.setPwd("Pwd");

        if(login == null)
            return null;
        String pwd = login.getPwd();
        // 密码加密问题： MD5 和MD5 + salt 加密
        // 密码认证
        // 在这儿设置第一个参数
        return new SimpleAuthenticationInfo(login,pwd,"");
    }
}
