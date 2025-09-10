package idv.hzm.app.admin.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiMonitorAspect {

	private static final Logger logger = LoggerFactory.getLogger(ApiMonitorAspect.class);

	@Around("@within(org.springframework.web.bind.annotation.RestController)")
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		Object proceed = joinPoint.proceed();
		long time = System.currentTimeMillis() - start;

		if (time > 2000) { // API 慢請求閾值
			logger.warn("Slow API {} executed in {} ms", joinPoint.getSignature(), time);
		} else {
			logger.info("API {} executed in {} ms", joinPoint.getSignature(), time);
		}
		return proceed;
	}
}
