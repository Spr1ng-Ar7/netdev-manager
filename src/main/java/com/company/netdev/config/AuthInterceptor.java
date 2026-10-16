package com.company.netdev.config;

import com.company.netdev.common.BizException;
import com.company.netdev.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 会话鉴权拦截器：校验 session 中的 userId；
 * 未登录统一返回 code=401，前端据此跳转登录页。
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_NAME = "name";
    public static final String SESSION_ROLE = "role";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或会话已过期")));
            return false;
        }
        return true;
    }

    public static Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(SESSION_USER_ID);
        if (v == null) {
            throw new BizException(401, "未登录或会话已过期");
        }
        return (Long) v;
    }

    public static String currentUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(SESSION_USERNAME);
    }

    public static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && "ADMIN".equals(session.getAttribute(SESSION_ROLE));
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BizException(403, "无权限操作，需要管理员角色");
        }
    }
}
