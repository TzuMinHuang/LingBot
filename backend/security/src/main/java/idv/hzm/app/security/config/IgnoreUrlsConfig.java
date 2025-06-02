package idv.hzm.app.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * SpringSecurity白名單資源路徑配置
 */
@ConfigurationProperties(prefix = "secure.ignored")
public class IgnoreUrlsConfig {

    private List<String> urls = new ArrayList<>();

    public List<String> getUrls() {
      return urls;
    }

    public void setUrls(List<String> urls) {
      this.urls = urls;
    }
}
