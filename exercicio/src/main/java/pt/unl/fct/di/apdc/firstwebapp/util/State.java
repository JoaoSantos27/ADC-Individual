package pt.unl.fct.di.apdc.firstwebapp.util;

public enum State {
	ACTIVE("Active"), INACTIVE("Inactive");
	
	private String value;
    
    private State(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
