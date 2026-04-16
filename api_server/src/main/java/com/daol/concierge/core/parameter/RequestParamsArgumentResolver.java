package com.daol.concierge.core.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Map;

public class RequestParamsArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger logger = LoggerFactory.getLogger(RequestParamsArgumentResolver.class);
    private final ObjectMapper objectMapper;

    public RequestParamsArgumentResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestParams.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        RequestParams requestParams = new RequestParams();

        // URL query parameters
        requestParams.setParameterMap(request.getParameterMap());

        // JSON body (POST/PUT)
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String body = sb.toString().trim();
                if (!body.isEmpty()) {
                    Map<String, Object> bodyMap = objectMapper.readValue(body, Map.class);
                    requestParams.putAll(bodyMap);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse JSON body: {}", e.getMessage());
            }
        }

        // Multipart files
        if (request instanceof MultipartHttpServletRequest mrequest) {
            Iterator<String> iter = mrequest.getFileNames();
            while (iter.hasNext()) {
                String fileName = iter.next();
                requestParams.addFile(fileName, mrequest.getFile(fileName));
            }
        }

        logger.debug("<<PARAMETER[{}]>> {}", request.getRequestURI(), requestParams);
        return requestParams;
    }
}
