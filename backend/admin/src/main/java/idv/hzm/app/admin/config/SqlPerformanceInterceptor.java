package idv.hzm.app.admin.config;

import java.sql.Connection;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Intercepts({
		@Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class SqlPerformanceInterceptor implements Interceptor {

	private static final Logger logger = LoggerFactory.getLogger(SqlPerformanceInterceptor.class);
	private static final long SLOW_SQL_THRESHOLD = 500; // ms

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

	    // 透過 MyBatis 提供的 MetaObject 拿到真實的 MappedStatement
	    MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
	    MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

	    String sql = statementHandler.getBoundSql().getSql();

	    long start = System.currentTimeMillis();
	    Object result = invocation.proceed();
	    long duration = System.currentTimeMillis() - start;

	    if (duration > SLOW_SQL_THRESHOLD) {
	    	logger.warn("[SLOW SQL] " + duration + " ms | "  + mappedStatement.getId() + " | SQL: " + sql);
	    } else {
	    	logger.info("[SQL] " + duration + " ms | "  + mappedStatement.getId() + " | SQL: " + sql);
	    }

	    return result;
	}

}
