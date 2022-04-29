package hcf.seckill.dto.Login;

/**
 * @author hechaofan
 * @date 2022/4/3 18:30
 */
public class LoginVo {

    private long phone;
    private String pwd;

    public long getPhone() {
        return phone;
    }

    public void setPhone(long phone) {
        this.phone = phone;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return "LoginVo{" +
                "phone=" + phone +
                ", pwd='" + pwd + '\'' +
                '}';
    }
}
