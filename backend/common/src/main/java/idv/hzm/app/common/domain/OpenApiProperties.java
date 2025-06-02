package idv.hzm.app.common.domain;

public class OpenApiProperties {

  private String title = "";
  private String description = "";
  private String version = "";
  private String licenseName = "";
  private String licenseUrl = "";
  private String externalDocumentationDescription = "";
  private String externalDocumentationUrl = "";

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

  public String getLicenseName() {
    return licenseName;
  }

  public void setLicenseName(String licenseName) {
    this.licenseName = licenseName;
  }

  public String getLicenseUrl() {
    return licenseUrl;
  }

  public void setLicenseUrl(String licenseUrl) {
    this.licenseUrl = licenseUrl;
  }

  public String getExternalDocumentationDescription() {
    return externalDocumentationDescription;
  }

  public void setExternalDocumentationDescription(String externalDocumentationDescription) {
    this.externalDocumentationDescription = externalDocumentationDescription;
  }

  public String getExternalDocumentationUrl() {
    return externalDocumentationUrl;
  }

  public void setExternalDocumentationUrl(String externalDocumentationUrl) {
    this.externalDocumentationUrl = externalDocumentationUrl;
  }

  @Override
  public String toString() {
    return "OpenApiProperties [title=" + title + ", description=" + description + ", version="
        + version + ", licenseName=" + licenseName + ", licenseUrl=" + licenseUrl
        + ", externalDocumentationDescription=" + externalDocumentationDescription
        + ", externalDocumentationUrl=" + externalDocumentationUrl + "]";
  }

}
