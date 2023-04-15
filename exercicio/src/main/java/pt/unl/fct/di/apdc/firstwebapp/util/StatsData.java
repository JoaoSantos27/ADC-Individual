package pt.unl.fct.di.apdc.firstwebapp.util;

public class StatsData {

	public long logins;
	public long failedLogins;
	public String firstLogin;
	public String lastLogin;
	public String lastAttempt;

	public StatsData() {

	}

	public StatsData(long logins, long failedLogins, String firstLogin, String lastLogin, String lastAttempt) {
		this.logins = logins;
		this.failedLogins = failedLogins;
		this.firstLogin = firstLogin;
		this.lastLogin = lastLogin;
		this.lastAttempt = lastAttempt;
	}

}
