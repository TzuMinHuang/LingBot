package idv.hzm.app.security.util;

// import cn.hutool.core.date.DateUtil;
// import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import idv.hzm.app.common.util.DateUtil;
import idv.hzm.app.common.util.StrUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtToken生成的工具類
 * 
 * JWT token的格式：header.payload.signature header的格式（算法、token的類型）： {"alg":"HS512","typ": "JWT"}
 * payload的格式（用戶名、創建時間、生成時間）：{"sub":"wang","created":1489079981393,"exp":1489684781}
 * signature的生成算法：HMACSHA512(base64UrlEncode(header) + "." +base64UrlEncode(payload),secret)
 * 
 */
public class JwtTokenUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenUtil.class);
  private static final String CLAIM_KEY_USERNAME = "sub";
  private static final String CLAIM_KEY_CREATED = "created";
  @Value("${jwt.secret}")
  private String secret;
  @Value("${jwt.expiration}")
  private Long expiration;
  @Value("${jwt.tokenHead}")
  private String tokenHead;

  /**
   * 根據負責生成JWT的token
   */
  private String generateToken(Map<String, Object> claims) {
    return Jwts.builder().setClaims(claims).setExpiration(generateExpirationDate())
        .signWith(SignatureAlgorithm.HS512, secret).compact();
  }

  /**
   * 從token中獲取JWT中的負載
   */
  private Claims getClaimsFromToken(String token) {
    Claims claims = null;
    try {
      claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    } catch (Exception e) {
      LOGGER.info("JWT格式驗證失敗:{}", token);
    }
    return claims;
  }

  /**
   * 生成token的過期時間
   */
  private Date generateExpirationDate() {
    return new Date(System.currentTimeMillis() + expiration * 1000);
  }

  /**
   * 從token中獲取登錄用戶名
   */
  public String getUserNameFromToken(String token) {
    String username;
    try {
      Claims claims = getClaimsFromToken(token);
      username = claims.getSubject();
    } catch (Exception e) {
      username = null;
    }
    return username;
  }

  /**
   * 驗證token是否還有效
   *
   * @param token 客戶端傳入的token
   * @param userDetails 從數據庫中查詢出來的用戶信息
   */
  public boolean validateToken(String token, UserDetails userDetails) {
    String username = getUserNameFromToken(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  /**
   * 判斷token是否已經失效
   */
  private boolean isTokenExpired(String token) {
    Date expiredDate = getExpiredDateFromToken(token);
    return expiredDate.before(new Date());
  }

  /**
   * 從token中獲取過期時間
   */
  private Date getExpiredDateFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.getExpiration();
  }

  /**
   * 根據用戶信息生成token
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());
    claims.put(CLAIM_KEY_CREATED, new Date());
    return generateToken(claims);
  }

  /**
   * 當原來的token沒過期時是可以刷新的
   *
   * @param oldToken 帶tokenHead的token
   */
  public String refreshHeadToken(String oldToken) {
    if (StrUtil.isEmpty(oldToken)) {
      return null;
    }
    String token = oldToken.substring(tokenHead.length());
    if (StrUtil.isEmpty(token)) {
      return null;
    }
    Claims claims = getClaimsFromToken(token); // token校驗不通過
    if (claims == null) {
      return null;
    }

    if (isTokenExpired(token)) {// 如果token已經過期，不支持刷新
      return null;
    }
    if (tokenRefreshJustBefore(token, 30 * 60)) {// 如果token在30分鐘之內剛刷新過，返回原token
      return token;
    } else {
      claims.put(CLAIM_KEY_CREATED, new Date());
      return generateToken(claims);
    }
  }

  /**
   * 判斷token在指定時間內是否剛剛刷新過
   * 
   * @param token 原token
   * @param time 指定時間（秒）
   */
  private boolean tokenRefreshJustBefore(String token, int time) {
    Claims claims = getClaimsFromToken(token);
    Date created = claims.get(CLAIM_KEY_CREATED, Date.class);
    Date refreshDate = new Date();
    // 刷新時間在創建時間的指定時間內
    if (refreshDate.after(created) && refreshDate.before(DateUtil.offsetSecond(created, time))) {
      return true;
    }
    return false;
  }
}
