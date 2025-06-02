package idv.hzm.app.common.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 操作 Service
 */
public interface RedisService {

  /**
   * 保存屬性
   */
  void set(String key, Object value, long time);

  /**
   * 保存屬性
   */
  void set(String key, Object value);

  /**
   * 獲取屬性
   */
  Object get(String key);

  /**
   * 刪除屬性
   */
  Boolean del(String key);

  /**
   * 批量刪除屬性
   */
  Long del(List<String> keys);

  /**
   * 設置過期時間
   */
  Boolean expire(String key, long time);

  /**
   * 獲取過期時間
   */
  Long getExpire(String key);

  /**
   * 判斷是否有該屬性
   */
  Boolean hasKey(String key);

  /**
   * 按delta遞增
   */
  Long incr(String key, long delta);

  /**
   * 按delta遞減
   */
  Long decr(String key, long delta);

  /**
   * 獲取Hash結構中的屬性
   */
  Object hGet(String key, String hashKey);

  /**
   * 向Hash結構中放入一個屬性
   */
  Boolean hSet(String key, String hashKey, Object value, long time);

  /**
   * 向Hash結構中放入一個屬性
   */
  void hSet(String key, String hashKey, Object value);

  /**
   * 直接獲取整個Hash結構
   */
  Map<Object, Object> hGetAll(String key);

  /**
   * 直接設置整個Hash結構
   */
  Boolean hSetAll(String key, Map<String, Object> map, long time);

  /**
   * 直接設置整個Hash結構
   */
  void hSetAll(String key, Map<String, ?> map);

  /**
   * 刪除Hash結構中的屬性
   */
  void hDel(String key, Object... hashKey);

  /**
   * 判斷Hash結構中是否有該屬性
   */
  Boolean hHasKey(String key, String hashKey);

  /**
   * Hash結構中屬性遞增
   */
  Long hIncr(String key, String hashKey, Long delta);

  /**
   * Hash結構中屬性遞減
   */
  Long hDecr(String key, String hashKey, Long delta);

  /**
   * 獲取Set結構
   */
  Set<Object> sMembers(String key);

  /**
   * 向Set結構中添加屬性
   */
  Long sAdd(String key, Object... values);

  /**
   * 向Set結構中添加屬性
   */
  Long sAdd(String key, long time, Object... values);

  /**
   * 是否為Set中的屬性
   */
  Boolean sIsMember(String key, Object value);

  /**
   * 獲取Set結構的長度
   */
  Long sSize(String key);

  /**
   * 刪除Set結構中的屬性
   */
  Long sRemove(String key, Object... values);

  /**
   * 獲取List結構中的屬性
   */
  List<Object> lRange(String key, long start, long end);

  /**
   * 獲取List結構的長度
   */
  Long lSize(String key);

  /**
   * 根據索引獲取List中的屬性
   */
  Object lIndex(String key, long index);

  /**
   * 向List結構中添加屬性
   */
  Long lPush(String key, Object value);

  /**
   * 向List結構中添加屬性
   */
  Long lPush(String key, Object value, long time);

  /**
   * 向List結構中批量添加屬性
   */
  Long lPushAll(String key, Object... values);

  /**
   * 向List結構中批量添加屬性
   */
  Long lPushAll(String key, Long time, Object... values);

  /**
   * 從List結構中移除屬性
   */
  Long lRemove(String key, long count, Object value);
}
