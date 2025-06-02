package idv.hzm.app.security.component;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Iterator;

import idv.hzm.app.common.util.CollUtil;

/**
 * 動態權限決策管理器，用於判斷用戶是否有訪問權限.
 */
public class DynamicAccessDecisionManager implements AccessDecisionManager {

  @Override
  public void decide(Authentication authentication, Object object,
      Collection<ConfigAttribute> configAttributes)
      throws AccessDeniedException, InsufficientAuthenticationException {
    // 當接口未被配置資源時直接放行
    if (CollUtil.isEmpty(configAttributes)) {
      return;
    }
    Iterator<ConfigAttribute> iterator = configAttributes.iterator();
    while (iterator.hasNext()) {
      ConfigAttribute configAttribute = iterator.next();
      // 將訪問所需資源或用戶擁有資源進行比對
      String needAuthority = configAttribute.getAttribute();
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        if (needAuthority.trim().equals(grantedAuthority.getAuthority())) {
          return;
        }
      }
    }
    throw new AccessDeniedException("抱歉，您沒有訪問權限");
  }

  @Override
  public boolean supports(ConfigAttribute configAttribute) {
    return true;
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return true;
  }

}
