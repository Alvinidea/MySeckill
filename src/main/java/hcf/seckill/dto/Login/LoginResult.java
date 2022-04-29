package hcf.seckill.dto.Login;

import hcf.seckill.enums.LoginStateEnum;

/**
 * @author hechaofan
 * @date 2022/4/29 12:51
 */
public class LoginResult {
    private int code;

    private String description;


    public LoginResult() {
        this(LoginStateEnum.LOGIN_SUCCESS);
    }

    public LoginResult(LoginStateEnum loginStateEnum) {
        this.code = loginStateEnum.getCode();
        this.description = loginStateEnum.getDescription();
    }

    public LoginResult(int code, String description) {
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

    @Override
    public String toString() {
        return "LoginResult{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
