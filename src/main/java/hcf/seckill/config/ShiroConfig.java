package hcf.seckill.config;

import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.util.LinkedHashMap;
import java.util.Map;


public class ShiroConfig {


    // 3. ShiroFilterFactoryBean
    @Bean
    public ShiroFilterFactoryBean getShiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager defaultWebSecurityManager)
    {
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        /*
            设置安全管理器
                ShiroFilterFactoryBean -> DefaultWebSecurityManager -> UserRealm
        */
        bean.setSecurityManager(defaultWebSecurityManager);
        // 添加shiro的内置管理器

        /* =========================================================================== */
        /* 其他信息都是模板，一般情况下真正需要修改的就是这儿
        *       配置拦截和授权                                                            */
        /* --------------------------------------------------------------------------- */
        /*
         *   anon：无需认证就可以访问
         *   authc：必须认证了才可以访问
         *   user：必须拥有 记住我 功能才能狗使用
         *   perms：拥有对某个资源的访问权限才能访问
         *   role：拥有某个角色才能访问
         * */
        Map<String, String> filterMap = new LinkedHashMap<>();
        // 拦截
        filterMap.put("/view/goods_list","authc");
        filterMap.put("/view/goods_detail","authc");

        // 权限授权
        // 访问  "/view/shiroAuth" 页面，需要具有 "user:all" 权限
        filterMap.put("/view/shiroAuth","perms[user:all]");

        bean.setFilterChainDefinitionMap(filterMap);
        // 设置登录请求（被拦截之后会 转到 此页面）：这种就是基于业务的设计
        bean.setLoginUrl("/view/login");
        // 设置未授权页面
        bean.setUnauthorizedUrl("/view/unAuthorize");
        /* --------------------------------------------------------------------------- */
        return bean;
    }


    // 2. DefaultWebSecurityManager (1会用到)
    @Bean(name="securityManager")
    public DefaultWebSecurityManager getDefaultWebSecurityManager(@Qualifier("userRealm") UserRealm userRealm)
    {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 关联 UserRealm
        securityManager.setRealm(userRealm);

        return securityManager;
    }

    // 3. 创建 realm 对象，需要自定义 (2 会用到)
    @Bean(name="userRealm")
    public UserRealm userRealm(){
        return new UserRealm();
    }

    // 整合 ShiroDialect ：用于整合shiro 和 thymeleaf
/*    @Bean
    public ShiroDialect getShiroDialect()
    {
        return new ShiroDialect();
    }*/
}
