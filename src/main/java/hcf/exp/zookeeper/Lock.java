package hcf.exp.zookeeper;

/**
 * @author hechaofan
 * @date 2022/4/28 12:08
 */
public interface Lock {

    void getLock();

    void unlock();
}