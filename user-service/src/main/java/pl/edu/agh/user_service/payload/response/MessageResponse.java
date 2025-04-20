package pl.edu.agh.user_service.payload.response;

public class MessageResponse {
    private int code;
    private String message;

  public MessageResponse(int code, String message) {
      this.code = code;
      this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
