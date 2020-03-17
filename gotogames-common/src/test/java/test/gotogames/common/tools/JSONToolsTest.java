package test.gotogames.common.tools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import junit.framework.TestCase;


import com.gotogames.common.tools.JSONTools;

public class JSONToolsTest extends TestCase {
	public void testMapData() {
		JSONTools jsonTools = new JSONTools();
		String test1Str = "{\"valueInt\":1,\"valueString\":\"toto\"}"; 
		try {
			JSONTest1 t1 = jsonTools.mapData(test1Str, JSONTest1.class);
			assertNotNull(t1);
			assertEquals(1, t1.valueInt);
			assertEquals("toto", t1.valueString);
		} catch (JsonParseException e) {
			assertTrue("JsonParseException - "+e.getMessage(), false);
		} catch (JsonMappingException e) {
			assertTrue("JsonMappingException - "+e.getMessage(), false);
		} catch (IOException e) {
			assertTrue("IOException - "+e.getMessage(), false);
		}
	}
	
	public void testTransform2Str() {
		JSONTools jsonTools = new JSONTools();
		JSONTest1 t1 = new JSONTest1();
		t1.valueInt = 1;
		t1.valueString = "toto";
		try {
			String str = jsonTools.transform2String(t1, false);
			assertNotNull(str);
			assertEquals(str, "{\"valueInt\":1,\"valueString\":\"toto\"}");
		} catch (JsonGenerationException e) {
			assertTrue("JsonGenerationException - "+e.getMessage(), false);
		} catch (JsonMappingException e) {
			assertTrue("JsonMappingException - "+e.getMessage(), false);
		} catch (IOException e) {
			assertTrue("IOException - "+e.getMessage(), false);
		}
	}
	
	public void testArnaud() {
		JSONTools jsonTools = new JSONTools();
		String data = "{\"error\":null, \"data\":[{\"id\":\"1050\", \"pseudo\":\"Pierre Andry\", \"photo\":\"empty\", \"serie\":\"1 <img src='/forum/img/new_smileys/20.png' style='vertical-align:middle;'>\", \"pays\":\"<img src='/img/drap_fr.gif' style='vertical-align:middle;'>\"}]}";
		try {
			JSONTestArnaud1 test = jsonTools.mapData(data, JSONTestArnaud1.class);
			System.out.println(test);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testMapDataEngine() {
		JSONTools jsonTools = new JSONTools();
		String data = "{\"exception\":{\"message\":\"THE SESSION ID IS NOT VALID\",\"type\":\"SESSION_INVALID_SESSION_ID\"},\"data\":null}";
		try {
			EngineRestResponse<EngineRestGetResultResponse> test = jsonTools.mapData(data, new TypeReference<EngineRestResponse<EngineRestGetResultResponse>>(){});
			System.out.println(test);
			assertNotNull(test);
			assertTrue(test.isException());
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testMapDataEngine2() {
		JSONTools jsonTools = new JSONTools();
		String data = "{\"exception\":null,\"data\":{\"result\":\"PA\"}}";
		try {
			EngineRestResponse<EngineRestGetResultResponse> test = jsonTools.mapData(data, new TypeReference<EngineRestResponse<EngineRestGetResultResponse>>(){});
			System.out.println(test);
			assertNotNull(test);
			assertFalse(test.isException());
			assertNotNull(test.data);
			assertEquals("PA", test.data.result);
		} catch (JsonParseException e) {
			assertFalse(true);
			e.printStackTrace();
		} catch (JsonMappingException e) {
			assertFalse(true);
			e.printStackTrace();
		} catch (IOException e) {
			assertFalse(true);
			e.printStackTrace();
		}
	}
	
	public void testUpdateJSONString() {
		String jsonOri = "{\"exception\":null,\"data\":{\"result\":\"PA\"}, \"champs1\":123456}";
		String jsonModif = "{\"exception\":null, \"champs1\":0, \"champs2\":89765}";
		try {
			String jsonNew = JSONTools.updateJSONString(jsonOri, jsonModif);
			assertNotNull(jsonNew);
			assertTrue(JSONTools.checkKeyPresent(jsonNew, "exception"));
			assertTrue(JSONTools.checkKeyPresent(jsonNew, "data"));
			assertTrue(JSONTools.checkKeyPresent(jsonNew, "champs1"));
			assertTrue(JSONTools.checkKeyPresent(jsonNew, "champs2"));
			assertEquals("0", JSONTools.getKeyValueString(jsonNew, "champs1"));
			assertEquals("89765", JSONTools.getKeyValueString(jsonNew, "champs2"));
			assertEquals("null", JSONTools.getKeyValueString(jsonNew, "exception"));
			assertEquals("{\"result\":\"PA\"}", JSONTools.getKeyValueString(jsonNew, "data"));
		} catch (JsonProcessingException e) {
			assertFalse(true);
			e.printStackTrace();
		} catch (IOException e) {
			assertFalse(true);
			e.printStackTrace();
		}
	}
	
	public void testCheckKeyPresent() {
		try {
			String jsonOri = "{\"exception\":null,\"data\":{\"result\":\"PA\"}, \"champs1\":123456}";
			assertTrue(JSONTools.checkKeyPresent(jsonOri, "exception"));
			assertTrue(JSONTools.checkKeyPresent(jsonOri, "champs1"));
			assertFalse(JSONTools.checkKeyPresent(jsonOri, "champs"));
			assertFalse(JSONTools.checkKeyPresent(jsonOri, "result"));
		} catch (JsonProcessingException e) {
			assertFalse(true);
			e.printStackTrace();
		} catch (IOException e) {
			assertFalse(true);
			e.printStackTrace();
		}
	}
	
	public void testGetKeyValueString() {
		try {
			String jsonOri = "{\"exception\":null,\"data\":{\"result\":\"PA\"}, \"champs1\":123456}";
			assertEquals("123456", JSONTools.getKeyValueString(jsonOri, "champs1"));
			assertEquals("null", JSONTools.getKeyValueString(jsonOri, "exception"));
			assertEquals("{\"result\":\"PA\"}", JSONTools.getKeyValueString(jsonOri, "data"));
		} catch (JsonProcessingException e) {
			assertFalse(true);
			e.printStackTrace();
		} catch (IOException e) {
			assertFalse(true);
			e.printStackTrace();
		}
	}
}
