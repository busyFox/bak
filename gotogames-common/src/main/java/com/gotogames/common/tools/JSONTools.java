package com.gotogames.common.tools;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONTools {
	private ObjectMapper jsonMapper = new ObjectMapper();
	
	public JSONTools() {
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public ObjectMapper getMapper() {
		return jsonMapper;
	}
	
	/**
	 * Transform string data to type of valueData
	 * @param mapper
	 * @param data
	 * @param valueData
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	public static <T> T mapData(ObjectMapper mapper, String data, Class<T> valueData) throws IOException, JsonParseException, JsonMappingException {
		if (mapper != null && data != null && data.length() > 0) {
			return mapper.readValue(data, valueData);
		}
		return null;
	}
	
	/**
	 * Transform string data to type of valueData
	 * @param data
	 * @param valueData
	 * @return
	 * @throws IOException, JsonParseException, JsonMappingException
	 */
	public <T> T mapData(String data, Class<T> valueData) throws IOException, JsonParseException, JsonMappingException{
		return mapData(jsonMapper, data, valueData);
	}
	
	/**
	 * Transform string data to type of TypeReference. Use this method if the return object is or contains generic type
	 * @param mapper
	 * @param data
	 * @param valTypeRef
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T mapData(ObjectMapper mapper, String data, TypeReference<T> valTypeRef) throws IOException, JsonParseException, JsonMappingException {
		if (mapper != null && data != null && data.length() > 0) {
			return (T)mapper.readValue(data, valTypeRef);
		}
		return null;
	}
	
	/**
	 * Transform string data to type of TypeReference. Use this method if the return object is or contains generic type
	 * @param data
	 * @param valTypeRef
	 * @return
	 * @throws IOException, JsonParseException, JsonMappingException
	 */
	public <T> T mapData(String data, TypeReference<T> valTypeRef) throws IOException, JsonParseException, JsonMappingException {
		return mapData(jsonMapper, data, valTypeRef);
	}
	
	public String transform2String(Object data, boolean prettyPrint) throws JsonGenerationException, JsonMappingException, IOException {
	    return transform2String(jsonMapper, data, prettyPrint);
	}
	
	public static String transform2String(ObjectMapper mapper, Object data, boolean prettyPrint) throws JsonGenerationException, JsonMappingException, IOException {
		if (mapper != null && data != null) {
		    if (prettyPrint) {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            }
			return mapper.writeValueAsString(data);
		}
		return null;
	}
	
	public void transform2File(String filePath, Object data, boolean prettyPrint) throws JsonGenerationException, JsonMappingException, IOException {
		transform2File(jsonMapper, filePath, data, prettyPrint);
	}
	
	public static void transform2File(ObjectMapper mapper, String filePath, Object data, boolean prettyPrint) throws JsonGenerationException, JsonMappingException, IOException {
		if (mapper != null && data != null) {
		    if (prettyPrint) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
            } else {
                mapper.writeValue(new File(filePath), data);
            }
		}
	}
	
	/**
	 * Transform data from file to type of TypeReference
	 * @param filePath
	 * @param valTypeRef
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T mapDataFromFile(String filePath, TypeReference<T> valTypeRef) throws JsonParseException, JsonMappingException, IOException {
		return mapDataFromFile(jsonMapper, filePath, valTypeRef);
	}
	
	/**
	 * Transform data from file to type of TypeReference
	 * @param mapper
	 * @param filePath
	 * @param valTypeRef
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T mapDataFromFile(ObjectMapper mapper, String filePath, TypeReference<T> valTypeRef) throws JsonParseException, JsonMappingException, IOException {
		if (mapper != null && filePath != null && filePath.length() > 0) {
			return (T)mapper.readValue(new File(filePath), valTypeRef);
		}
		return null;
	}
	
	/**
	 * Transform data from file to type of valueData
	 * @param mapper
	 * @param filePath
	 * @param valTypeRef
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static <T> T mapDataFromFile(ObjectMapper mapper, String filePath, Class<T> valueData) throws JsonParseException, JsonMappingException, IOException {
		if (mapper != null && filePath != null && filePath.length() > 0) {
			return (T)mapper.readValue(new File(filePath), valueData);
		}
		return null;
	}
	
	/**
	 * Transform data from file to type of valueData
	 * @param filePath
	 * @param valTypeRef
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public <T> T mapDataFromFile(String filePath, Class<T> valueData) throws JsonParseException, JsonMappingException, IOException {
		return mapDataFromFile(jsonMapper, filePath, valueData);
	}
	
	/**
	 * Update the json string from the original with the node value updated and newed from the new json. If a node is present in the original and not in the new, the retrun string will contained this node.
	 * This is a jointure between the nodes from the original and the new. ONLY NODE AT LEVEL 1 ARE PROCEEDED.
	 * @param jsonOri
	 * @param jsonNew
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static String updateJSONString(String jsonOri, String jsonNew) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		// read the original json string and extract all key at level 1
		JsonNode nodeOri = mapper.readTree(jsonOri);
		// read the new json string
		JsonNode nodeNew = mapper.readTree(jsonNew);
		
		// loop on each key from the original
		Iterator<Map.Entry<String, JsonNode>> itOri = nodeOri.fields();
		while (itOri.hasNext()){
			Map.Entry<String, JsonNode> e = itOri.next();
			// check if node key is present in the new
			if (!nodeNew.has(e.getKey())) {
				// add the node in the new
				((ObjectNode)nodeNew).put(e.getKey(), e.getValue());
			}
		}
		return mapper.writeValueAsString(nodeNew);
	}
	
	/**
	 * Check a key is present in node at level 1
	 * @param json
	 * @param key
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static boolean checkKeyPresent(String json, String key) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		// read the original json string and extract all key at level 1
		JsonNode node = mapper.readTree(json);
		return node.has(key);
	}
	
	/**
	 * Return the string value for a key
	 * @param json
	 * @param key
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static String getKeyValueString(String json, String key) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		// read the original json string and extract all key at level 1
		JsonNode node = mapper.readTree(json);
		JsonNode n = node.get(key);
		if (n != null) {
			return n.toString();
		}
		return null;
	}

    /**
     * Extract JSON data to map by converting JSON fields . Map FieldsConversion contains keys (path to extract) and associated value (key to use in return map).
     * Sample : In mapFielsConversion, key="/node1/node2/field1" associated value="mynode", the return contains a key "mynode" with the value find in the nodeRoot.
     * If the nodeRoot doesn't contains the searched field (path return missing node), the field is added to the fielsNotFound list.
     * If an exception occurs, the searched field is added to the fielsFailed list.
     * @param nodeRoot
     * @param mapFieldsConversion
     * @param fieldsNotFound
     * @param fielsFailed
     * @return
     */
    public static Map<String, Object> extractJSONData(JsonNode nodeRoot, Map<String, String> mapFieldsConversion, List<String> fieldsNotFound, List<String> fielsFailed) {
        Map<String, Object> values = new HashMap<>();
        for (String k : mapFieldsConversion.keySet()) {
            try {
                String keyConv = mapFieldsConversion.get(k);
                JsonNode nodeVal = nodeRoot.at(k);
                if (nodeVal != null && keyConv != null && !nodeVal.isMissingNode()) {
                    if (nodeVal.isTextual()) {
                        values.put(keyConv, nodeVal.textValue());
                    } else if (nodeVal.isBoolean()) {
                        values.put(keyConv, nodeVal.booleanValue());
                    } else if (nodeVal.isDouble()) {
                        values.put(keyConv, nodeVal.doubleValue());
                    } else if (nodeVal.isInt()) {
                        values.put(keyConv, nodeVal.intValue());
                    } else if (nodeVal.isLong()) {
                        values.put(keyConv, nodeVal.longValue());
                    } else if (nodeVal.isFloat()) {
                        values.put(keyConv, nodeVal.floatValue());
                    } else {
                        values.put(keyConv, nodeVal.toString());
                    }
                } else {
                    if (fieldsNotFound != null) {
                        fieldsNotFound.add(k);
                    }
                }
            } catch (Exception e) {
                if (fielsFailed != null) {
                    fielsFailed.add(k);
                }
            }
        }
        return values;
    }
}
