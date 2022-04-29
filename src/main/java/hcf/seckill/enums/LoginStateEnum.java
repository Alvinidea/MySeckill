package hcf.seckill.enums;

/**
 *
 * 登录、注册的状态
 * @author hechaofan
 * @date 2022/4/29 15:55
 */
public enum LoginStateEnum {
    LOGIN_SUCCESS(1, "login success"),
    LOGIN_NO_USER(2, "user phone didn't exist"),
    LOGIN_ERR_PWD(3, "password didn't exist"),
    LOGIN_ERR_UNKNOWM(4, "login err with unknown reason"),
    REGISTER_SUCCESS(21, "register success"),
    REGISTER_ERR_UNKNOWM(21, "register err with unknown reason");

    private int code;
    private String description;


    LoginStateEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
