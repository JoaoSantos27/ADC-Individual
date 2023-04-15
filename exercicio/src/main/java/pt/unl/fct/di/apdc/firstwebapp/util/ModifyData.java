package pt.unl.fct.di.apdc.firstwebapp.util;

public class ModifyData {

	public AuthToken token;
	public String username;
	public String attribute;
	public String newValue;

	public ModifyData() {

	}

	public ModifyData(AuthToken token, String username, String attribute, String newValue) {
		this.token = token;
		this.username = username;
		this.attribute = attribute;
		this.newValue = newValue;
	}

}
