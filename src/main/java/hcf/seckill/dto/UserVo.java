package hcf.seckill.dto;

/**
 * @author hechaofan
 * @date 2022/4/28 20:37
 */
public class UserVo {
    private long userPhone;

    private String userPwd;

    public long getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(long userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    @Override
    public String toString() {
        return "UserVo{" +
                "userPhone=" + userPhone +
                ", userPwd='" + userPwd + '\'' +
                '}';
    }
}
