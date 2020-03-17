package test.gotogames.common.lock;

import java.util.Random;

import com.gotogames.common.lock.LockKeyMgr;


public class LockKeyMgrTest extends Thread{
	private String name;
	private static LockKeyMgr<String> lockMgr = new LockKeyMgr<String>();
	private String key = "toto";
	private LockKeyMgr<String> lMgr;
	
	public static void main(String[] args) {
		LockKeyMgrTest t1 = new LockKeyMgrTest("Thread 1", lockMgr);
		LockKeyMgrTest t2 = new LockKeyMgrTest("Thread 2", lockMgr);
		t1.start();
		t2.start();
	}

	public LockKeyMgrTest(String n, LockKeyMgr mgr) {
		this.name = n;
		this.lMgr = mgr;
	}
	
	@Override
	public void run() {
		try {
			Random random = new Random(System.nanoTime());
			int nbSecondToSleep =random.nextInt(10);
			System.out.println(name+" - Before get lock sleep during "+nbSecondToSleep+" seconds");
			Thread.sleep(nbSecondToSleep*1000);
			System.out.println(name+" - Start get lock");
			lMgr.lockKey(key);
			System.out.println(name+" - I have the lock !!!");
			nbSecondToSleep =random.nextInt(10)+20;
			System.out.println(name+" - Before unlock sleep during "+nbSecondToSleep+" seconds");
			Thread.sleep(nbSecondToSleep*1000);
			System.out.println(name+" - The lock is free !!!");
			lMgr.unlockKey(key);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
}
