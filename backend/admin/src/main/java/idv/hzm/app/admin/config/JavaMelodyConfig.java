package idv.hzm.app.admin.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.bull.javamelody.MonitoringFilter;

@Configuration
public class JavaMelodyConfig {

    @Bean
    public FilterRegistrationBean<MonitoringFilter> monitoringFilter() {
        FilterRegistrationBean<MonitoringFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MonitoringFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}

