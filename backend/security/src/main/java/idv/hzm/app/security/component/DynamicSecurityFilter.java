package idv.hzm.app.security.component;


import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.access.intercept.AbstractSecurityInterceptor;
import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.web.FilterInvocation;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import idv.hzm.app.security.config.IgnoreUrlsConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 動態權限過濾器，用於實現基於路徑的動態權限過濾.
 */
public class DynamicSecurityFilter extends AbstractSecurityInterceptor implements Filter {

  @Autowired
  private DynamicSecurityMetadataSource dynamicSecurityMetadataSource;
  @Autowired
  private IgnoreUrlsConfig ignoreUrlsConfig;

  @Autowired
  public void setMyAccessDecisionManager(
      DynamicAccessDecisionManager dynamicAccessDecisionManager) {
    super.setAccessDecisionManager(dynamicAccessDecisionManager);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    FilterInvocation fi = new FilterInvocation(servletRequest, servletResponse, filterChain);
    // OPTIONS請求直接放行
    if (request.getMethod().equals(HttpMethod.OPTIONS.toString())) {
      fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
      return;
    }
    // 白名單請求直接放行
    PathMatcher pathMatcher = new AntPathMatcher();
    for (String path : ignoreUrlsConfig.getUrls()) {
      if (pathMatcher.match(path, request.getRequestURI())) {
        fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
        return;
      }
    }
    // 此處會調用AccessDecisionManager中的decide方法進行鑑權操作
    InterceptorStatusToken token = super.beforeInvocation(fi);
    try {
      fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
    } finally {
      super.afterInvocation(token, null);
    }
  }

  @Override
  public void destroy() {}

  @Override
  public Class<?> getSecureObjectClass() {
    return FilterInvocation.class;
  }

  @Override
  public SecurityMetadataSource obtainSecurityMetadataSource() {
    return dynamicSecurityMetadataSource;
  }

}
