package test.gotogames.common.session;

import com.gotogames.common.session.Session;
import com.gotogames.common.session.SessionMgr;

import junit.framework.TestCase;

public class SessionMgrTest extends TestCase {
	private final String loginTest = "testlogin";
	private final long loginID = 12345;
	private SessionMgr mgr;
	
	@Override
	protected void setUp() throws Exception {
		mgr = SessionMgr.createSessionMgr("test", SessionMgr.SESSION_TYPE_MEMORY);
		assertNotNull(mgr);
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		if (mgr != null) {
			mgr.destroy();
		}
		super.tearDown();
	}
	
	public void testCreateSession(){
		Session s = SessionMgr.createSession(loginTest, loginID, -1);
		assertNotNull(s);
		assertTrue(mgr.putSession(s));
		assertTrue(mgr.isSessionExist(s.getID()));
		assertEquals(s, mgr.getSessionForLogin(loginTest));
		assertTrue(mgr.putSession(s));
		assertTrue(mgr.isSessionExist(s.getID()));
	}
	
	public void testDeleteSession() {
		Session s = SessionMgr.createSession(loginTest, loginID, -1);
		assertNotNull(s);
		assertTrue(mgr.putSession(s));
		assertTrue(mgr.isSessionExist(s.getID()));
		assertTrue(mgr.deleteSession(s.getID()));
		assertTrue(!mgr.isSessionExist(s.getID()));
		assertNull(mgr.getSessionForLogin(loginTest));
	}
	
	public void testScheduler() throws InterruptedException {
		mgr.startCleanScheduler(1);
		Session s = SessionMgr.createSession(loginTest, loginID, 2);
		assertNotNull(s);
		assertTrue(mgr.putSession(s));
		Thread.sleep(1500);
		mgr.touchSession(s.getID());
		Thread.sleep(1500);
		assertTrue(mgr.isSessionExist(s.getID()));
		Thread.sleep(1500);
		assertFalse(mgr.isSessionExist(s.getID()));
		
		s = SessionMgr.createSession(loginTest, loginID, -1);
		assertTrue(mgr.putSession(s));
		Thread.sleep(3000);
		assertTrue(mgr.isSessionExist(s.getID()));
		
	}
	
	public void testGetAllCurrentSession() {
		Session s = SessionMgr.createSession(loginTest, loginID, -1);
		assertNotNull(s);
		assertTrue(mgr.putSession(s));
		assertTrue(mgr.isSessionExist(s.getID()));
		assertTrue(mgr.getAllCurrentSession().contains(s));
		mgr.deleteSession(s.getID());
		assertFalse(mgr.isSessionExist(s.getID()));
		assertFalse(mgr.getAllCurrentSession().contains(s));
	}

	
}
