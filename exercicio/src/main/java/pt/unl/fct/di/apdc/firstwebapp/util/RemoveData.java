package pt.unl.fct.di.apdc.firstwebapp.util;

public class RemoveData {
	
	public AuthToken token;
	public String username;
	
	public RemoveData() {
		
	}
	
	public RemoveData(AuthToken token, String username) {
		this.token = token;
		this.username = username;
	}

}
