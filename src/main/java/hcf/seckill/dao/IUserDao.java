package hcf.seckill.dao;

import hcf.seckill.entity.IUser;
import org.apache.ibatis.annotations.Param;

/**
 * @author hechaofan
 * @date 2022/4/28 19:29
 */
public interface IUserDao {

    IUser queryUserById(@Param("userId") long userId);

    IUser queryUserByPhone(@Param("userPhone") long userPhone);

    int createUser(@Param("user")IUser user);

    String queryPwd(@Param("userPhone")long userPhone);

    int updatePwd(@Param("user")IUser user);
}
