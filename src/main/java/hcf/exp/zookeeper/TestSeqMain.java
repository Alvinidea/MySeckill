package hcf.exp.zookeeper;

import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/4/27 21:38
 */
public class TestSeqMain {

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threadList.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    ZkSeqLock zkLock = new ZkSeqLock(LockType.FAIR);
                    // ZkSeqLock zkLock = new ZkSeqLock(LockType.UNFAIR);
                    zkLock.getLock();
                    try {
                        System.out.println("    " + Thread.currentThread().getName() + " : deal mission");
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    zkLock.unlock();
                }
            }));
        }

        threadList.forEach(thread -> thread.start());

        /*
        * ZkClient zkClient = new ZkClient(
                "10.16.65.76:2181"
                , 10000);
        * zkClient.createPersistent("/HCF_ZK_SeqLOCK");
        * 此位置加上这句话之后会报以下异常：节点已经存在
        * Caused by: org.apache.zookeeper.KeeperException$NodeExistsException:
        *       KeeperErrorCode = NodeExists for /HCF_ZK_SeqLOCK
        * */
        Thread.sleep(10000);
        ZkClient zkClient = new ZkClient(
                "10.16.65.76:2181"
                , 10000);
        zkClient.delete("/HCF_ZK_SeqLOCK");
        System.out.println(zkClient.exists("/HCF_ZK_SeqLOCK"));
        zkClient.close();
    }

}
