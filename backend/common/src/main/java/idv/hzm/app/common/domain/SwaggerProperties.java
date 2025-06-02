package idv.hzm.app.common.domain;

import java.util.Objects;

/**
 * Swagger custom configuration.
 */

public class SwaggerProperties {

  /**
   * API documentation generation base path
   */
  private String apiBasePackage;
  /**
   * Whether to enable login authentication
   */
  private boolean enableSecurity;
  /**
   * document title
   */
  private String title;
  /**
   * Document description
   */
  private String description;
  /**
   * Document version
   */
  private String version;
  /**
   * Document contact name
   */
  private String contactName;
  /**
   * Document contact URL
   */
  private String contactUrl;
  /**
   * Document Contact Email
   */
  private String contactEmail;

  public String getApiBasePackage() {
    return apiBasePackage;
  }

  public void setApiBasePackage(String apiBasePackage) {
    this.apiBasePackage = apiBasePackage;
  }

  public boolean isEnableSecurity() {
    return enableSecurity;
  }

  public void setEnableSecurity(boolean enableSecurity) {
    this.enableSecurity = enableSecurity;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getContactUrl() {
    return contactUrl;
  }

  public void setContactUrl(String contactUrl) {
    this.contactUrl = contactUrl;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiBasePackage, contactEmail, contactName, contactUrl, description,
        enableSecurity, title, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SwaggerProperties other = (SwaggerProperties) obj;
    return Objects.equals(apiBasePackage, other.apiBasePackage)
        && Objects.equals(contactEmail, other.contactEmail)
        && Objects.equals(contactName, other.contactName)
        && Objects.equals(contactUrl, other.contactUrl)
        && Objects.equals(description, other.description) && enableSecurity == other.enableSecurity
        && Objects.equals(title, other.title) && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return "SwaggerProperties [apiBasePackage=" + apiBasePackage + ", enableSecurity="
        + enableSecurity + ", title=" + title + ", description=" + description + ", version="
        + version + ", contactName=" + contactName + ", contactUrl=" + contactUrl
        + ", contactEmail=" + contactEmail + "]";
  }

}
