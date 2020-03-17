package com.gotogames.common.services;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotogames.common.tools.JSONTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Map.Entry;

public class JSONService {
	private Logger log = LogManager.getLogger(this.getClass());
	private ObjectMapper jsonMapper = new ObjectMapper();
	
	public JSONService() {
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public JSONService(boolean configMapper) {
		if (configMapper) {
			jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
	}
	
	private String getData(String strURL, String contentParam, Map<String, String> headerParams, int timeout) {
		try {
			// open connection to serviceURL
			URL url = new URL(strURL);
			URLConnection conn = url.openConnection();
			if (timeout > 0) {
				conn.setConnectTimeout(timeout);
				conn.setReadTimeout(timeout);
			}
			if (headerParams != null) {
				for (Entry<String, String> p : headerParams.entrySet()) {
					conn.addRequestProperty(p.getKey(), p.getValue());
				}
			}
			log.debug("openConnection success to "+strURL);
			if (contentParam != null && contentParam.length() > 0) {
				conn.setRequestProperty("CONTENT-TYPE", "application/json; charset=UTF-8");
				conn.setDoOutput(true);
				OutputStreamWriter writer = null;
				writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
				writer.write(contentParam);
				writer.flush();
				writer.close();
			}
			// open stream and read the response
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			StringBuffer buffer = new StringBuffer();
			while ((line = reader.readLine()) != null)  {
				buffer.append(line);
			}
			// close reader
			reader.close();
			return buffer.toString();
		} catch (IOException e) {
			log.error("IOException to get data from strURL="+strURL+" - contentParam="+contentParam, e);
		} catch (Exception e) {
			log.error("Exception to get data from strURL="+strURL+" - contentParam="+contentParam, e);
		}
		return null;
	}
	
	/**
	 * Call a service and retrieve data from service and map it to TypeRefernce. Use this method if the return object is or contains generic type
	 * @param serviceURL
	 * @param param
     * @param headerParams
	 * @param typeRef
     * @param timeout
	 * @return
	 * @throws JSONServiceException
	 */
	public <T> T callService(String serviceURL,
			Object param, 
			Map<String, String> headerParams,
			TypeReference<T> typeRef,
			int timeout) throws JSONServiceException {
		if (serviceURL == null || serviceURL.length() == 0) {
			log.error("Parameter serviceURL is null or empty");
			throw new JSONServiceException("Parameter serviceURL is null or empty");
		}
		if (typeRef == null) {
			log.error("Parameter typeRef is null");
			throw new JSONServiceException("Parameter typeRef is null");
		}
		try {
			// transform param to JSON string
			String paramJson = null;
			if (param != null) {
				if (param != null) {
					paramJson = jsonMapper.writeValueAsString(param);
				}
			}
			
			// get data from service
			String dataStr = getData(serviceURL, paramJson, headerParams, timeout);
			if (dataStr == null) {
				throw new JSONServiceException("Error to get data from serviceURL="+serviceURL+" - paramJson="+paramJson+" - timeout="+timeout);
			}
			// transform string data to valueType object
			T result = JSONTools.mapData(jsonMapper, dataStr, typeRef);
			if (result == null) {
				throw new JSONServiceException("Error to map data from serviceURL="+serviceURL+" - paramJson="+paramJson+" - timeout="+timeout+" - dataStr="+dataStr);
			}
			return result;
		} catch (JsonParseException e) {
			log.error("JsonParseException", e);
			throw new JSONServiceException("JSON Parse exception");
		} catch (JsonGenerationException e) {
			log.error("Exception JsonGenerationException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("JSON Generation exception");
		} catch (JsonMappingException e) {
			log.error("Exception JsonMappingException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("JSON Mapping exception");
		} catch (MalformedURLException e) {
			log.error("Exception MalformedURLException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("MalformedURL exception");
		} catch (IOException e) {
			log.error("Exception IOException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("IOException exception");
		} catch (JSONServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("Exception serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("Exception");
		}
	}
	
	/**
	 * Call a service and retrieve data from service and map it to valueType
	 * @param serviceURL
	 * @param param
     * @param headerParams
	 * @param valueType
     * @param timeout
	 * @return
	 * @throws JSONServiceException
	 */
	public <T> T callService(String serviceURL,
			Object param,
			Map<String, String> headerParams,
			Class<T> valueType,
			int timeout) throws JSONServiceException {
		if (serviceURL == null || serviceURL.length() == 0) {
			log.error("Parameter serviceURL is null or empty");
			throw new JSONServiceException("Parameter serviceURL is null or empty");
		}
		if (valueType == null) {
			log.error("Parameter valueType is null");
			throw new JSONServiceException("Parameter valueType is null");
		}
		try {
			// transform param to JSON string
			String paramJson = null;
			if (param != null) {
				if (param != null) {
					paramJson = jsonMapper.writeValueAsString(param);
				}
			}
			
			// get data from service
			String dataStr = getData(serviceURL, paramJson, headerParams, timeout);
			if (dataStr == null) {
				throw new JSONServiceException("Error to get data from serviceURL="+serviceURL+" - paramJson="+paramJson+" - timeout="+timeout);
			}
			T result = JSONTools.mapData(jsonMapper, dataStr, valueType);
			if (result == null) {
				throw new JSONServiceException("Error to map data from serviceURL="+serviceURL+" - paramJson="+paramJson+" - timeout="+timeout+" - dataStr="+dataStr);
			}
			return result;
		} catch (JsonGenerationException e) {
			log.error("Exception JsonGenerationException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("JSON Generation exception");
		} catch (JsonMappingException e) {
			log.error("Exception JsonMappingException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("JSON Mapping exception");
		} catch (MalformedURLException e) {
			log.error("Exception MalformedURLException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("MalformedURL exception");
		} catch (IOException e) {
			log.error("Exception IOException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("IOException exception");
		} catch (JSONServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("Exception serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
			throw new JSONServiceException("Exception");
		}
	}

    /**
     * Call a service and retrieve string data from service
     * @param serviceURL
     * @param param
     * @param headerParams
     * @param timeout
     * @return
     * @throws JSONServiceException
     */
	public String callService(String serviceURL,
                              Object param,
                              Map<String, String> headerParams,
                              int timeout) throws JSONServiceException {
        if (serviceURL == null || serviceURL.length() == 0) {
            log.error("Parameter serviceURL is null or empty");
            throw new JSONServiceException("Parameter serviceURL is null or empty");
        }
        try {
            // transform param to JSON string
            String paramJson = null;
            if (param != null) {
                if (param != null) {
                    paramJson = jsonMapper.writeValueAsString(param);
                }
            }

            // get data from service
            return getData(serviceURL, paramJson, headerParams, timeout);
        } catch (JsonGenerationException e) {
            log.error("Exception JsonGenerationException serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
            throw new JSONServiceException("JSON Generation exception");
        } catch (Exception e) {
            log.error("Exception serviceURL="+serviceURL+" - param="+param+" - Exception="+e.getMessage(), e);
            throw new JSONServiceException("Exception");
        }
    }
}
