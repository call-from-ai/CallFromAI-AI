package com.example.aidatingagentbackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestIdSupport {

    public static final String HEADER = "X-Request-Id";
    private static final String ATTRIBUTE = RequestIdSupport.class.getName() + ".requestId";

    private RequestIdSupport() {
    }

    public static void attach(HttpServletRequest request, String requestId) {
        if (StringUtils.hasText(requestId)) {
            request.setAttribute(ATTRIBUTE, requestId);
        }
    }

    public static String resolve(HttpServletRequest request) {
        Object attribute = request.getAttribute(ATTRIBUTE);
        if (attribute instanceof String requestId && StringUtils.hasText(requestId)) {
            return requestId;
        }
        String header = request.getHeader(HEADER);
        return StringUtils.hasText(header) ? header : null;
    }
}
