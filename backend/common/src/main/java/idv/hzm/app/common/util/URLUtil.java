package idv.hzm.app.common.util;

import java.net.URL;
import idv.hzm.app.common.exception.UtilException;

public class URLUtil {

  private URLUtil() {}

  /**
   * 獲得path部分<br>
   *
   * @param uriStr URI路徑
   * @return path
   * @throws UtilException 包裝URISyntaxException
   */
  public static String getPath(String uriStr) {
    return cn.hutool.core.util.URLUtil.getPath(uriStr);
  }

  public static URL url(String urlStr) {
    return cn.hutool.core.util.URLUtil.url(urlStr);
  }

}
