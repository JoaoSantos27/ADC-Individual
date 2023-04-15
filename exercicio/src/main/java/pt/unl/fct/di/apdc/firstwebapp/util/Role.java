package pt.unl.fct.di.apdc.firstwebapp.util;

public enum Role {
	USER("User"), GBO("GBO"), GA("GA"), GS("GS"), SU("SU");
	
	private String value;
    
    private Role(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
