## 第一部分 ： DB + DAO（Data Access Object）

1. SpringMVC
2. MySQL
3. MyBatis
 - xml配置
 
4. Spring 整合 MyBatis
- 少编码：只写接口，不写实现
- 少配置：别名、mapper配置文件的扫描问题
- 灵活性：定制SQL、自由传参、结果赋值


> 错误记录
> 
> [1] java.sql.SQLException: Access denied for user 'as'@'localhost' (using password: YES)
> 之所以出现这个错误是因为在db.properties中写了username=xxx
> 而username这个变量好像被jvm环境占用了，所以在applicationContext.xml中${username}取值得到的是jvm中username的值，

> [2] The server time zone value '�й���׼ʱ��' is unrecogni....
      解决方案
> 在 URL 后面加上 ?serverTimezone=UTC 如下：
>
> url=jdbc:mysql://localhost:3306/DBname?serverTimezone=UTC

---
**总结：**
DB设计
Mapper接口
SQL编写
---


## 第二部分 ： Service

dto：业务层的数据传输问题
exception：业务层会出现的一场处理
service：具体业务层代码


业务对象依赖：
seckillService
    SeckillDao
    SuccessKilledDao
        SqlSessionFactory
            DataSource
            
### IOC
    对象创建统一管理
    规范的生命周期管理
    灵活的依赖注入
    一致的获取对象

---  
IOC注入方式和场景

- XML：第三方类库（DataSource等，需要命名空间配置等）
- 注解：项目中Spring框架中的类（Service、Controller等）
- Java配置类：需要通过代码控制对象创建逻辑的场景
----
本项目使用：
XML配置 + 注解（package-scan + Annotation）


---
### Spring 声明式事务
声明式事务使用方法：
- 早期使用方式： ProxyFactoryBean + XML 
- 一次配置永久有效： tx:advice + aop 命名空间
- （推荐）注解方法：注解@Transactional

事务嵌套 + 传播行为

什么时候回滚事务：
- 运行期异常（RuntimeException），小心不当的try-catch

声明式事务的配置=> spring-service.xml

Q2: IDEA新建xml文件显示为普通的text文本
> https://blog.csdn.net/qq_40585396/article/details/85250182

Q3：org.apache.ibatis.binding.BindingException: Invalid bound statement (not found)错误
> 1. xml配置
> 2. mapper.xml文件与接口名一致
> 3. 对应方法名一致
> 4. 返回类型一致


## web层

- 前端交互
- Restful
- SpringMVC
- JQuery+Bootstrap

**前端页面流程**
列表页 -> 详情页面 -> login
                        -No--> 登录 -> 写入 Cookie   ->  展示逻辑
                        -Yes          ->                展示逻辑
                        
详情页面
当前标准系统时间
时间判断（秒杀未开始，秒杀中，秒杀结束）
秒杀未开始-倒计时
秒杀中- 

Q4：解决问题--idea启动SpringMVC项目中文打印乱码
> https://www.jianshu.com/p/f58cde986da9

Q5：Caused by: java.lang.IllegalArgumentException: 找到多个名为spring_web的片段。这是不合法的相对排序。
> https://www.jianshu.com/p/9cc900a18c9e
> https://www.icode9.com/content-4-628789.html
> https://blog.csdn.net/Linwang2020/article/details/117480977?spm=1001.2101.3001.6650.2&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7ERate-2.pc_relevant_default&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7ECTRLIST%7ERate-2.pc_relevant_default&utm_relevant_index=5

Q6：静态资源js，css等加载不了
> 将js，css对应文件放置在WEB-INF的同一级文件夹中，
> 也就是如下结构：
> webapp
>     css
>     js
>     WEB-INF
>
Q7：测试前端页面时候，注意刷新浏览器缓存

Q8：异常被你的 catch“吃了”导致 @Transactional 失效
> “吃了” : 在catch中没有重新将异常抛出
>  https://www.cnblogs.com/frankyou/p/12691463.html

Q9：MySQL 如何查看表的存储引擎
> mysql> show create table test;                 
  +-------+----------------------------------------------+
  | Table | Create Table                                 |
  +-------+----------------------------------------------+
  | test  | CREATE TABLE `test` (
    `id` int(11) DEFAULT NULL,
    `name` varchar(12) DEFAULT NULL
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8 |
  +-------+----------------------------------------------+
  1 row in set (0.00 sec)

Q10：Tomcat10的坑，可能导致404等问题
>  Tomcat9正常运行，但是Tomcat10出现404
>  https://www.cnblogs.com/djma/p/15684017.html
>  https://blog.csdn.net/qq_34325222/article/details/105922739?utm_medium=distribute.pc_aggpage_search_result.none-task-blog-2~aggregatepage~first_rank_ecpm_v1~rank_v31_ecpm-8-105922739.pc_agg_new_rank&utm_term=9+tomcat10+%E5%8C%BA%E5%88%AB&spm=1000.2123.3001.4430
>> 原因：servlet包名竟然不是叫javax!!是jaraka

Q11：静态资源放在放在webapp和放在WEB-INF下的区别
> 1、当静态资源放在webapp下面的时候，可直接通过浏览器访问，不需要配置映射，安全性略低
> 
> 2、WEB-INF是Java的WEB应用的安全目录。所谓安全就是客户端无法访问，只有服务端可以访问的目录。
> 当静态资源放在WEB-INF下面的时候，外部是不能直接访问的，一般是在springmvc的配置文件中配置资源映射
> 如：
> <!-- 资源映射 -->
```
    <mvc:resources location="/WEB-INF/css/" mapping="/css/**"/>
    <mvc:resources location="/WEB-INF/js/" mapping="/js/**"/>
```
> webapp与WEB—INF的区别:https://blog.csdn.net/weixin_40159122/article/details/104889928

  