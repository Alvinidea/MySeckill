package hcf.exp.zookeeper;

/**
 * @author hechaofan
 * @date 2022/4/28 12:08
 */
public abstract class AbstractTemplateLock implements Lock {
    public void getLock(){
        if(tryLock()){
            System.out.println(Thread.currentThread().getName()+ " : Got Lock");
        }else{
            waitLock();
            getLock();
        }
    }

    public void unlock(){
        releaseLock();
    }
    public abstract boolean tryLock() ;
    public abstract void waitLock();
    public abstract void releaseLock();
}
