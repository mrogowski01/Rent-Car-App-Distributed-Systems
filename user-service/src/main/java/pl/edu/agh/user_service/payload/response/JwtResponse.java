package pl.edu.agh.user_service.payload.response;

public class JwtResponse {
  private String text;
  private String token;
  private String type = "Bearer";
  private Long id;
  private String username;
  private String role;

  public JwtResponse(String text, String accessToken, Long id, String username, String role) {
    this.text = text;
    this.token = accessToken;
    this.id = id;
    this.username = username;
    this.role = role;
  }

  public String getAccessToken() {
    return token;
  }

  public void setAccessToken(String accessToken) {
    this.token = accessToken;
  }

  public String getTokenType() {
    return type;
  }

  public void setTokenType(String tokenType) {
    this.type = tokenType;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
