package idv.hzm.app.security.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import idv.hzm.app.security.annotation.CacheException;

/**
 * Redis緩存切面，防止Redis宕機影響正常業務邏輯.
 */
@Aspect
@Component
@Order(2)
public class RedisCacheAspect {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheAspect.class);

  @Pointcut("execution(public * idv.hzm.app.portal.service.*CacheService.*(..)) || execution(public * idv.hzm.app.service.*CacheService.*(..))")
  public void cacheAspect() {}

  @Around("cacheAspect()")
  public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    Signature signature = joinPoint.getSignature();
    MethodSignature methodSignature = (MethodSignature) signature;
    Method method = methodSignature.getMethod();
    Object result = null;
    try {
      result = joinPoint.proceed();
    } catch (Throwable throwable) {
      // 有CacheException註解的方法需要拋出異常
      if (method.isAnnotationPresent(CacheException.class)) {
        throw throwable;
      } else {
        LOGGER.error(throwable.getMessage());
      }
    }
    return result;
  }

}
