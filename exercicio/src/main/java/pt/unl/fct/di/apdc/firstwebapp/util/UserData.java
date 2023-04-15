package pt.unl.fct.di.apdc.firstwebapp.util;

public class UserData {

	public String username;
    public String email;
    public String name;
    public String privacy;
    public String role;
    public String state;

    public UserData() {
    	
    }

    public UserData(String username, String name, String email, String privacy, String role, String state) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.privacy = privacy;
        this.role = role;
        this.state = state;
    }
}
