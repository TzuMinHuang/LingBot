package idv.hzm.app.common.util;

import cn.hutool.json.JSON;

public class JSONUtil {
  
  private JSONUtil() {}

  public static JSON parse(Object obj) {
    return cn.hutool.json.JSONUtil.parse(obj);
  }
}
