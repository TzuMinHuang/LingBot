package idv.hzm.app.common.log;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import idv.hzm.app.common.domain.WebLog;
import idv.hzm.app.common.util.JSONUtil;
import idv.hzm.app.common.util.StrUtil;
import idv.hzm.app.common.util.URLUtil;
//import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import net.logstash.logback.marker.Markers;

/**
 * Unified log processing aspect.
 */
@Aspect
@Component
@Order(1)
public class WebLogAspect {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);

  @Pointcut("execution(public * idv.hzm.app.*.controller.*.*(..))")
  public void webLog() {}

  @Before("webLog()")
  public void doBefore(JoinPoint joinPoint) throws Throwable {}

  @AfterReturning(value = "webLog()", returning = "ret")
  public void doAfterReturning(Object ret) throws Throwable {}

//  @Around("webLog()")
//  public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
//    long startTime = System.currentTimeMillis();
//
//    // Get the current request object
//    ServletRequestAttributes attributes =
//        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//    HttpServletRequest request = attributes.getRequest();
//
//    // Record request information (passed into Elasticsearch through Logstash)
//    WebLog webLog = new WebLog();
//    Object result = joinPoint.proceed();
//    Signature signature = joinPoint.getSignature();
//    MethodSignature methodSignature = (MethodSignature) signature;
//    Method method = methodSignature.getMethod();
//    if (method.isAnnotationPresent(Operation.class)) {
//      Operation log = method.getAnnotation(Operation.class);
//      webLog.setDescription(log.description());
//    }
//
//    long endTime = System.currentTimeMillis();
//    String urlStr = request.getRequestURL().toString();
//    webLog.setBasePath(StrUtil.removeSuffix(urlStr, URLUtil.url(urlStr).getPath()));
//    webLog.setUsername(request.getRemoteUser());
//    webLog.setIp(request.getRemoteAddr());
//    webLog.setMethod(request.getMethod());
//    webLog.setParameter(getParameter(method, joinPoint.getArgs()));
//    webLog.setResult(result);
//    webLog.setSpendTime((int) (endTime - startTime));
//    webLog.setStartTime(startTime);
//    webLog.setUri(request.getRequestURI());
//    webLog.setUrl(request.getRequestURL().toString());
//
//    Map<String, Object> logMap = new HashMap<>();
//    logMap.put("url", webLog.getUrl());
//    logMap.put("method", webLog.getMethod());
//    logMap.put("parameter", webLog.getParameter());
//    logMap.put("spendTime", webLog.getSpendTime());
//    logMap.put("description", webLog.getDescription());
//
//    LOGGER.info("{}", JSONUtil.parse(webLog));
//    LOGGER.info(Markers.appendEntries(logMap), JSONUtil.parse(webLog).toString());
//    return result;
//  }

  /**
   * Get request parameters based on method and incoming parameters
   */
  private Object getParameter(Method method, Object[] args) {
    List<Object> argList = new ArrayList<>();
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      // The parameter modified by the RequestBody annotation is used as the request parameter
      RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
      if (requestBody != null) {
        argList.add(args[i]);
      }
      // The parameter modified by the RequestParam annotation is used as the request parameter
      RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
      if (requestParam != null) {
        Map<String, Object> map = new HashMap<>();
        String key = parameters[i].getName();
        if (!StrUtil.isEmpty(requestParam.value())) {
          key = requestParam.value();
        }
        map.put(key, args[i]);
        argList.add(map);
      }
    }
    if (argList.size() == 0) {
      return null;
    } else if (argList.size() == 1) {
      return argList.get(0);
    } else {
      return argList;
    }
  }
}
