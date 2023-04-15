package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterData {
	
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\." +
            "[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
            "A-Z]{2,7}$");
	
    public String username;
    public String name;
    public String password;
    public String email;
    public String privacy;
    public String role;
    public String state;

    public RegisterData() {
    	this.role = Role.USER.getValue();
        this.state = State.INACTIVE.getValue();
    }

    public RegisterData(String username, String name, String password, String email, String privacy) {
        this.username = username;
        this.name = name;
        this.password = password;
        this.email = email;
        this.privacy = privacy;
        this.role = Role.USER.getValue();
        this.state = State.INACTIVE.getValue();
    }

    public boolean validRegistration() {
        return (username != null) && (password != null) && (password.length() >= 8) && isValidEmailAddress(email);
    }
    
    private boolean isValidEmailAddress(String email) {
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
    
}
