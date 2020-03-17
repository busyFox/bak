package com.gotogames.bridge.engineserver.ws;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Provider
@Component(value="mapperProvider")
@Produces({MediaType.APPLICATION_JSON})
public class JacksonMapperProvider implements ContextResolver<ObjectMapper>{
	private ObjectMapper mapper;
	
	public JacksonMapperProvider() {
        mapper = new ObjectMapper();
        // configure Jackson to not fail when a field is missing or adding in JSON
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        // usr annotation in Bean
        mapper.configure(MapperFeature.USE_ANNOTATIONS, true);
//        mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, true);
//        mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
	}
	
	@Override
	public ObjectMapper getContext(Class<?> type) {
		return mapper;
	}

	
}
