package com.tesis.mock.tourism;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@Slf4j
public class TouristAttractionsServiceApplication {
    private static final int MAX_PAYLOAD_CHARS = 5000;

    public static void main(String[] args) {
        SpringApplication.run(TouristAttractionsServiceApplication.class, args);
    }

    @Bean
    public OncePerRequestFilter debugPayloadLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (!log.isDebugEnabled()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

                Exception chainException = null;
                try {
                    filterChain.doFilter(requestWrapper, responseWrapper);
                } catch (Exception ex) {
                    chainException = ex;
                    throw ex;
                } finally {
                    String ruta = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
                    String payloadRequest = toPrintablePayload(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
                    String payloadResponse = toPrintablePayload(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());

                    if (chainException == null) {
                        log.debug("HTTP {} {} -> status={} payloadRequest={} payloadResponse={}",
                                request.getMethod(),
                                ruta,
                                responseWrapper.getStatus(),
                                payloadRequest,
                                payloadResponse);
                    } else {
                        log.debug("HTTP {} {} -> status={} payloadRequest={} payloadResponse={} error={}", 
                                request.getMethod(),
                                ruta,
                                responseWrapper.getStatus(),
                                payloadRequest,
                                payloadResponse,
                                chainException.toString());
                    }

                    responseWrapper.copyBodyToResponse();
                }
            }
        };
    }

    private String toPrintablePayload(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "<vacio>";
        }
        Charset charset = encoding == null || encoding.isBlank()
                ? StandardCharsets.UTF_8
                : Charset.forName(encoding);
        String text = new String(content, charset).replaceAll("[\\r\\n]+", " ").trim();
        if (text.isEmpty()) {
            return "<vacio>";
        }
        return text.length() > MAX_PAYLOAD_CHARS
                ? text.substring(0, MAX_PAYLOAD_CHARS) + "...(truncado)"
                : text;
    }
}
