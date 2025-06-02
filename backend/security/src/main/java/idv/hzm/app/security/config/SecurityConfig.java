package idv.hzm.app.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import idv.hzm.app.security.component.DynamicSecurityFilter;
import idv.hzm.app.security.component.DynamicSecurityService;
import idv.hzm.app.security.component.JwtAuthenticationTokenFilter;
import idv.hzm.app.security.component.RestAuthenticationEntryPoint;
import idv.hzm.app.security.component.RestfulAccessDeniedHandler;


/**
 * SpringSecurity 5.4.x以上新用法配置，僅用於配置HttpSecurity.
 */
@Configuration
public class SecurityConfig {

  @Autowired
  private IgnoreUrlsConfig ignoreUrlsConfig;
  @Autowired
  private RestfulAccessDeniedHandler restfulAccessDeniedHandler;
  @Autowired
  private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
  @Autowired
  private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
  @Autowired(required = false)
  private DynamicSecurityService dynamicSecurityService;
  @Autowired(required = false)
  private DynamicSecurityFilter dynamicSecurityFilter;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry =
        httpSecurity.authorizeRequests();
    // 不需要保護的資源路徑允許訪問
    for (String url : ignoreUrlsConfig.getUrls()) {
//      registry.antMatchers(url).permitAll();
    }
    // 允許跨域請求的OPTIONS請求
//    registry.antMatchers(HttpMethod.OPTIONS).permitAll();
    // 任何請求需要身份認證
    registry.and().authorizeRequests().anyRequest().authenticated()
        // 關閉跨站請求防護及不使用session
        .and().csrf().disable().sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        // 自定義權限拒絕處理類
        .and().exceptionHandling().accessDeniedHandler(restfulAccessDeniedHandler)
        .authenticationEntryPoint(restAuthenticationEntryPoint)
        // 自定義權限攔截器JWT過濾器
        .and()
        .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
    // 有動態權限配置時添加動態權限校驗過濾器
    if (dynamicSecurityService != null) {
      registry.and().addFilterBefore(dynamicSecurityFilter, FilterSecurityInterceptor.class);
    }
    return httpSecurity.build();
  }

}
