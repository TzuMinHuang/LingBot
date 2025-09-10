package idv.hzm.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CityMapper {

  @Select("SELECT sid, sname, sage, ssex FROM TEST_STUDENT WHERE sid = #{sid}")
  public City findBySid(@Param("sid") String sid);
  
  
  @Lang(MybatisExtendedLanguageDriver.class)
  @Select("SELECT sid, sname, sage, ssex FROM TEST_STUDENT WHERE sid NOT IN (#{sids})")
  List<City> findByNotSids(@Param("sids") List<String> sids);
  
  @Lang(MybatisExtendedLanguageDriver.class)
  @Select("SELECT sid, sname, sage, ssex FROM TEST_STUDENT WHERE sid NOT IN (#{sids}) AND sname NOT IN (#{snames})")
  List<City> findByNotSidsAndNotSnames(@Param("sids") List<String> sids, @Param("snames") List<String> snames);
  


}
