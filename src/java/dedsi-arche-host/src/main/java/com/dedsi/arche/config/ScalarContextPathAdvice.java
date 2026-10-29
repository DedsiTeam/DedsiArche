package com.dedsi.arche.config;

import org.springdoc.webmvc.scalar.ScalarWebMvcController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 补齐当前 Scalar 集成生成的站点根路径，使文档支持 Servlet context-path。
 */
@ControllerAdvice
public class ScalarContextPathAdvice implements ResponseBodyAdvice<String> {

    /**
     * 仅处理 Scalar 文档页面响应，避免改写其他接口内容。
     */
    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return returnType.getContainingClass() == ScalarWebMvcController.class
                && "getDocs".equals(returnType.getMethod().getName());
    }

    /**
     * 为 Scalar 页面脚本及文档地址补齐当前 Servlet 上下文路径。
     */
    @Override
    public String beforeBodyWrite(
            String body,
            MethodParameter returnType,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body == null || !(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        String contextPath = servletRequest.getServletRequest().getContextPath();
        if (contextPath.isEmpty()) {
            return body;
        }
        return body.replace("src=\"/scalar/", "src=\"" + contextPath + "/scalar/")
                .replace("\"url\":\"/v3/api-docs", "\"url\":\"" + contextPath + "/v3/api-docs");
    }
}
