package idv.hzm.app.common.domain;

import java.util.Objects;

/**
 * Log encapsulation class of Controller layer
 */
public class WebLog {
  /**
   * Operation description
   */
  private String description;

  /**
   * Operating user
   */
  private String username;

  /**
   * Operation time
   */
  private Long startTime;

  /**
   * time consuming
   */
  private Integer spendTime;

  /**
   * root path
   */
  private String basePath;

  /**
   * URI
   */
  private String uri;

  /**
   * URL
   */
  private String url;

  /**
   * request type
   */
  private String method;

  /**
   * IP address
   */
  private String ip;

  /**
   * request parameters
   */
  private Object parameter;

  /**
   * return result
   */
  private Object result;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Integer getSpendTime() {
    return spendTime;
  }

  public void setSpendTime(Integer spendTime) {
    this.spendTime = spendTime;
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public Object getParameter() {
    return parameter;
  }

  public void setParameter(Object parameter) {
    this.parameter = parameter;
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(basePath, description, ip, method, parameter, result, spendTime, startTime,
        uri, url, username);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WebLog other = (WebLog) obj;
    return Objects.equals(basePath, other.basePath)
        && Objects.equals(description, other.description) && Objects.equals(ip, other.ip)
        && Objects.equals(method, other.method) && Objects.equals(parameter, other.parameter)
        && Objects.equals(result, other.result) && Objects.equals(spendTime, other.spendTime)
        && Objects.equals(startTime, other.startTime) && Objects.equals(uri, other.uri)
        && Objects.equals(url, other.url) && Objects.equals(username, other.username);
  }

  @Override
  public String toString() {
    return "WebLog [description=" + description + ", username=" + username + ", startTime="
        + startTime + ", spendTime=" + spendTime + ", basePath=" + basePath + ", uri=" + uri
        + ", url=" + url + ", method=" + method + ", ip=" + ip + ", parameter=" + parameter
        + ", result=" + result + "]";
  }
}
