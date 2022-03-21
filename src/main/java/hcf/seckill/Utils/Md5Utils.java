package hcf.seckill.Utils;

import org.springframework.util.DigestUtils;

/**
 * @author hechaofan
 * @date 2022/3/21 16:22
 */
public class Md5Utils {

    // 盐，MD5混淆因子
    private static final String slat = "lsdfjolghoaes&**(sdgfklsfaf&&*(seorfihdsnj";

    public static String getMD5(long seckillId){
        String base = seckillId  + "/"+ slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }
}
