package idv.hzm.app.common.util;

import java.util.Date;

public class DateUtil {

  private DateUtil() {}

  public static Date offsetSecond(Date created, int time) {
    return cn.hutool.core.date.DateUtil.offsetSecond(created, time);
  }

}
