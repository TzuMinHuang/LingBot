package idv.hzm.app.common.util;

public class StrUtil {

  private StrUtil() {}

  public static boolean isEmpty(CharSequence path) {
    return cn.hutool.core.util.StrUtil.isEmpty(path);
  }

  public static String str(CharSequence path) {
    return cn.hutool.core.util.StrUtil.str(path);
  }

  public static String trim(String location) {
    return cn.hutool.core.util.StrUtil.trim(location);
  }

  public static String removeSuffix(String urlStr, String path) {
    return cn.hutool.core.util.StrUtil.removeSuffix(urlStr, path);
  }

}
