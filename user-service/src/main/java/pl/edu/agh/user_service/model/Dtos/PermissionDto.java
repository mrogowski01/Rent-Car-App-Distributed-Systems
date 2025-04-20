package pl.edu.agh.user_service.model.Dtos;

public class PermissionDto {
    String userMail;

    public String getUserMail(){
        return userMail;
    }

    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }
}
