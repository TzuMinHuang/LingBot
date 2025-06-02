package idv.hzm.app.security.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import idv.hzm.app.common.util.URLUtil;//import cn.hutool.core.util.URLUtil;
import jakarta.annotation.PostConstruct;


/**
 * 動態權限數據源，用於獲取動態權限規則.
 */
public class DynamicSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

  private static Map<String, ConfigAttribute> configAttributeMap = null;
  @Autowired
  private DynamicSecurityService dynamicSecurityService;

  @PostConstruct
  public void loadDataSource() {
    configAttributeMap = dynamicSecurityService.loadDataSource();
  }

  public void clearDataSource() {
    configAttributeMap.clear();
    configAttributeMap = null;
  }

  @Override
  public Collection<ConfigAttribute> getAttributes(Object o) throws IllegalArgumentException {
    if (configAttributeMap == null)
      this.loadDataSource();
    List<ConfigAttribute> configAttributes = new ArrayList<>();
    // 獲取當前訪問的路徑
    String url = ((FilterInvocation) o).getRequestUrl();
    String path = URLUtil.getPath(url);
    PathMatcher pathMatcher = new AntPathMatcher();
    Iterator<String> iterator = configAttributeMap.keySet().iterator();
    // 獲取訪問該路徑所需資源
    while (iterator.hasNext()) {
      String pattern = iterator.next();
      if (pathMatcher.match(pattern, path)) {
        configAttributes.add(configAttributeMap.get(pattern));
      }
    }
    return configAttributes;// 未設置操作請求權限，返回空集合
  }

  @Override
  public Collection<ConfigAttribute> getAllConfigAttributes() {
    return null;
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return true;
  }

}
