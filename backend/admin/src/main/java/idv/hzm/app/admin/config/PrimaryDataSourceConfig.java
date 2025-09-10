package idv.hzm.app.admin.config;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(basePackages = "idv.hzm.app.admin.mapper", sqlSessionTemplateRef = "PrimarySessionTemplate")
public class PrimaryDataSourceConfig {

	@Value("${spring.datasource.primary.url}")
	private String url;
	@Value("${spring.datasource.primary.username}")
	private String username;
	@Value("${spring.datasource.primary.password}")
	private String password;
	@Value("${spring.datasource.primary.driverClassName}")
	private String driverClassName;

	@Bean("PrimaryDataSource")
	@Primary
	public DataSource primaryDataSource() {
		return DataSourceBuilder.create().url(this.url).username(this.username).password(this.password)
				.driverClassName(this.driverClassName).build();
	}

	@Bean(name = "PrimarySessionFactory")
	@Primary
	public SqlSessionFactory primarySessionFactory(@Qualifier("PrimaryDataSource") DataSource dataSource)
			throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dataSource);
		bean.setPlugins(new Interceptor[]{new SqlPerformanceInterceptor()});
		return bean.getObject();
	}

	@Bean(name = "PrimaryTransactionManager")
	@Primary
	public DataSourceTransactionManager primaryTransactionManager(
			@Qualifier("PrimaryDataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean(name = "PrimarySessionTemplate")
	@Primary
	public SqlSessionTemplate primarySessionTemplate(
			@Qualifier("PrimarySessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory);
	}

}
