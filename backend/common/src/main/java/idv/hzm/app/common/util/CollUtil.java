package idv.hzm.app.common.util;

import java.util.Collection;

public class CollUtil {

  private CollUtil() {}

  public static boolean isEmpty(Collection<?> collection) {
    return cn.hutool.core.collection.CollUtil.isEmpty(collection);
  }
}
