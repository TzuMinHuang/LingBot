package idv.hzm.app.security.component;

import org.springframework.security.access.ConfigAttribute;

import java.util.Map;

/**
 * 動態權限相關業務類
 */
public interface DynamicSecurityService {
  /**
   * 加載資源ANT通配符和資源對應MAP
   */
  Map<String, ConfigAttribute> loadDataSource();
}
