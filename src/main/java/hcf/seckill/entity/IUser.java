package hcf.seckill.entity;

/**
 * @author hechaofan
 * @date 2022/4/28 19:24
 */
public class IUser {

    private long userId;

    private long userPhone;

    private String userName;

    private String userPwd;

    private int userAuth;

    private String userDescription;



    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(long userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    public int getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(int userAuth) {
        this.userAuth = userAuth;
    }

    public String getUserDescription() {
        return userDescription;
    }

    public void setUserDescription(String userDescription) {
        this.userDescription = userDescription;
    }

    @Override
    public String toString() {
        return "IUser{" +
                "userId=" + userId +
                ", userPhone=" + userPhone +
                ", userName='" + userName + '\'' +
                ", userPwd='" + userPwd + '\'' +
                ", userAuth=" + userAuth +
                ", userDescription='" + userDescription + '\'' +
                '}';
    }
}
