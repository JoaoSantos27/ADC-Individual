package pt.unl.fct.di.apdc.firstwebapp.util;

public class PasswordData {

	public AuthToken token;
	public String password;
	public String newPassword;

	public PasswordData() {

	}

	public PasswordData(AuthToken token, String username, String password, String newPassword) {
		this.token = token;
		this.password = password;
		this.newPassword = newPassword;
	}
}
