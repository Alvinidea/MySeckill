package hcf.exp.zookeeper;


import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author hechaofan
 * @date 2022/4/27 21:24
 *
 * https://blog.csdn.net/yuyu1067/article/details/117265653
 */
public class ZkSeqLock extends AbstractTemplateLock{
    private static final String Lock_Path = "/HCF_ZK_SeqLOCK";

    private ZkClient zkClient;

    // 当前锁节点路径
    private String currentPath;

    // 前一个锁节点路径
    private String prevPath;

    // 锁类型
    private LockType lockType;

    public ZkSeqLock(){
        // 默认生成公平锁对象
        this(LockType.FAIR);
    }

    public ZkSeqLock(LockType type){
        lockType = type;
        // 生成 Zookeeper 客户端对象
        zkClient = new ZkClient("10.16.65.76:2181", 10000);
        if(lockType == LockType.FAIR){
            try{
                // 根节点不存在则创建( 并发问题 )
                // A B都发现Lock_Path不存在，都进入了创建的方法，但是A先创建了Lock_Path
                // B 创建Lock_Path时候就会报异常
                // ---------------------------------------
                // 有Blog使用synchronized关键字。。。。，这可是分布式锁的实现耶，
                // 用synchronized干毛哦
                //  ---------------------------------------
                if(!zkClient.exists(Lock_Path)){
                    System.out.println(Thread.currentThread().getName() + " !zkClient.exists(Lock_Path");
                    zkClient.createPersistent(Lock_Path);
                }
            }catch (Exception e){
                // 捕获异常并打印（不打印也行啦，重点是捕获异常）
                e.printStackTrace();
            }
        }
    }

    /**
     * 公平锁、非公平锁
     * */
    public boolean tryLock(){
        if (lockType == LockType.FAIR) {
            return tryLock_Fair();
        }else{
            return tryLock_UnFair();
        }
    }

    /**
     * 非公平锁（临时节点，）
     * 这种实现的功能类似JVM锁中的非公平锁，即没有先后顺序所言
     * 监听 根节点
     * */
    private boolean tryLock_UnFair() {
        try{
            // 创建临时节点，也可以创建持久化节点，到时候释放节点的时候删除就好了
            zkClient.createEphemeral(Lock_Path, "");
            return true;
        }catch (Exception e){
            //如果创建失败，则获取节点锁失败，则进入等待
            return false;
        }
    }

    /**
     * 公平锁（临时顺序节点，）
     * 监听 前一节点
     * */
    public boolean tryLock_Fair(){
        if(currentPath == null){
            currentPath = zkClient.createEphemeralSequential(Lock_Path+"/","");
            System.out.println("Create EphemeralSequential Node : " + currentPath);
        }

        List<String> childrenList = zkClient.getChildren(Lock_Path);
        Collections.sort(childrenList);

        if(currentPath.equals(Lock_Path+"/"+ childrenList.get(0)) ){
            return true;
        }else{
            int currentIndex = childrenList.indexOf(currentPath.substring(Lock_Path.length() + 1));
            prevPath = Lock_Path + "/" + childrenList.get(currentIndex - 1);
            return false;
        }
    }

    /**
     * 监听（ 根 |前一）节点的数据变化情况，并做出相应反应
     * */
    public void waitLock(){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        //创建监听事件
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {}

            @Override
            public void handleDataDeleted(String s) throws Exception {
                System.out.println("node - " + s + " just deleted!");
                countDownLatch.countDown();
            }
        };
        //注册监听器
        String Path = null;
        if(lockType == LockType.FAIR){
            Path = prevPath;
        }else{
            Path = Lock_Path;                  // 非公平锁的监听
        }
        //如果ZooKeeper上存在锁节点，那么进入等待
        zkClient.subscribeDataChanges(Path, listener);
        if(zkClient.exists(Path)){
            try {
                //采用CountDownLatch等待        // 进入等待
                countDownLatch.await();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        // 删除监听器
        zkClient.unsubscribeDataChanges(Path, listener);
    }

    public void releaseLock(){
        //注册监听器
        String Path = null;
        if(lockType == LockType.FAIR){
            Path = currentPath;
        }else{
            Path = Lock_Path;   // 非公平锁的监听
        }
        zkClient.delete(Path);
        zkClient.close();
    }
}

enum LockType{
    FAIR(0),
    UNFAIR(1);

    private final int label;
    private LockType(int val){
        label = val;
    }
}