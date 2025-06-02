package idv.hzm.app.security.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Request tool class
 */
public class RequestUtil {
  
  private RequestUtil() {}

  /**
   * 獲取請求真實IP地址
   */
  public static String getRequestIp(HttpServletRequest request) {
    // 通過HTTP代理服務器轉發時添加
    String ipAddress = request.getHeader("x-forwarded-for");
    if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getHeader("Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getRemoteAddr();
      // 從本地訪問時根據網卡取本機配置的IP
      if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
        InetAddress inetAddress = null;
        try {
          inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
          e.printStackTrace();
        }
        ipAddress = inetAddress.getHostAddress();
      }
    }
    // 通過多個代理轉發的情況，第一個IP為客戶端真實IP，多個IP會按照','分割
    if (ipAddress != null && ipAddress.length() > 15) {
      if (ipAddress.indexOf(",") > 0) {
        ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
      }
    }
    return ipAddress;
  }

}
