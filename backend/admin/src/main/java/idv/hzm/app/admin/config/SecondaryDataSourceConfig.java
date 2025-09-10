package idv.hzm.app.admin.config;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(basePackages = "idv.hzm.app.temp.mapper", sqlSessionTemplateRef = "SecondarySessionTemplate")
public class SecondaryDataSourceConfig {

	@Value("${spring.datasource.secondary.url}")
	private String url;
	@Value("${spring.datasource.secondary.username}")
	private String username;
	@Value("${spring.datasource.secondary.password}")
	private String password;
	@Value("${spring.datasource.secondary.driverClassName}")
	private String driverClassName;

	@Bean("SecondaryDataSource")
	public DataSource secondaryDataSource() {
		return DataSourceBuilder.create().url(this.url).username(this.username).password(this.password)
				.driverClassName(this.driverClassName).build();
	}

	@Bean(name = "SecondarySessionFactory")
	public SqlSessionFactory secondarySessionFactory(@Qualifier("SecondaryDataSource") DataSource dataSource)
			throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dataSource);
		bean.setPlugins(new Interceptor[]{new SqlPerformanceInterceptor()});
		return bean.getObject();
	}

	@Bean(name = "SecondaryTransactionManager")
	public DataSourceTransactionManager secondaryTransactionManager(
			@Qualifier("SecondaryDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean(name = "SecondarySessionTemplate")
	public SqlSessionTemplate secondarySessionTemplate(
			@Qualifier("SecondarySessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}