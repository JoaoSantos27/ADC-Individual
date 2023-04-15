var authToken = localStorage.getItem('authToken'); // retrieve the authentication token from localStorage

function checkAuthToken() {
	if (authToken == null || authToken.valueOf() === "") {
		// If the token is not present, redirect the user to the login page
		window.location.href = '/index.html';
	}
	var decodedToken = JSON.parse(authToken);
	const userRole = decodedToken.role;
	const checkTokenInterval = setInterval(() => {
		authToken = localStorage.getItem('authToken');
		if (authToken == null || authToken.valueOf() === "") {
			// If the token is not present, redirect the user to the login page
			clearInterval(checkTokenInterval);
			alert('Your session is invalid');
			window.location.href = '/index.html';
		}
		decodedToken = JSON.parse(authToken);
		if (Date.now() > decodedToken.expirationDate || decodedToken.expired) {
			clearInterval(checkTokenInterval);
			localStorage.removeItem('authToken');
			alert('Your session is expired');
			logout();
			window.location.href = '/index.html';
		}
	}, 60000);

	const userRoleElement = document.getElementById('userRole');
	userRoleElement.textContent = 'Logged in as ' + userRole + ' ' + decodedToken.username; // display the user's role on the page
	const stripElement = document.getElementById('strip');

    if (userRole === 'User') {
      stripElement.classList.add('green-strip');
    } else if (userRole === 'GBO') {
      stripElement.classList.add('yellow-strip');
    } else if (userRole === 'GA') {
      stripElement.classList.add('blue-strip');
    } else if (userRole === 'GS') {
      stripElement.classList.add('orange-strip');
    } else {
      stripElement.classList.add('red-strip');
    }
}

function checkLogin() {
	if (authToken != null && authToken.valueOf() != "") {
		// If the token is present, redirect the user to the home page
		alert('Already logged in');
		window.location.href = '/pages/home.html';
	}
}

function submitLogin() {
	var username = document.getElementById('username').value;
	var password = document.getElementById('password').value;

	var xhr = new XMLHttpRequest();
	xhr.open('POST', '/rest/login/v1');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
	xhr.onload = function() {
		if (xhr.status === 200) {
			// handle successful login
			localStorage.setItem('authToken', xhr.responseText); // store the authentication token in localStorage
			alert('Logged in successfully!');
			window.location.href = '/pages/home.html'; // redirect to home.html page
		} else {
			// handle login error
			alert('Login failed: ' + xhr.responseText);
		}
	};
	xhr.onerror = function() {
		alert('Request failed.');
	};
	xhr.send(JSON.stringify({ username: username, password: password }));
}

function submitRegister() {
	var xhr = new XMLHttpRequest();
	var username = document.getElementById("username").value;
	var name = document.getElementById("name").value;
	var email = document.getElementById("email").value;
	var privacy = document.getElementById("privacy").value;
	var password = document.getElementById("password").value;
	var confirmPassword = document.getElementById("confirm-password").value;

	if (password !== confirmPassword) {
		alert('Passwords do not match!');
		return;
	}

	xhr.open("POST", "/rest/register/v3", true);
	xhr.setRequestHeader("Content-Type", "application/json");
	xhr.onreadystatechange = function() {
		if (xhr.readyState === XMLHttpRequest.DONE) {
			if (xhr.status === 200) {
				// handle successful registration
				alert('Registered successfully!');
				window.location.href = '/pages/login.html'; // redirect to login.html page
			} else {
				// handle registration error
				alert('Registration failed: ' + xhr.responseText);
			}
		}
	};
	xhr.send(JSON.stringify({
		username: username,
		email: email,
		name: name,
		privacy: privacy,
		password: password
	}));
}


function submitDelete(username) {
	if (confirm("Are you sure you want to delete this user?")) {
		var token = JSON.parse(authToken); // retrieve the authentication token from localStorage
		if (username == null) {
			username = document.getElementById('username').value;
		}
		var xhr = new XMLHttpRequest();
		xhr.open('POST', '/rest/remove/v1');
		xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
		xhr.onload = function() {
			if (xhr.status === 200) {
				// handle successful remove
				alert('Account deleted successfully');
				localStorage.setItem('authToken', xhr.responseText); // store the authentication token in localStorage
				window.location.href = '/pages/home.html'; // redirect to home.html page
			} else {
				// handle login error
				alert('Account deletion failed: ' + xhr.statusText);
			}
		};
		xhr.onerror = function() {
			alert('Request failed.');
		};
		xhr.send(JSON.stringify({ token: token, username: username }));
	}
}

function modifyAccount() {
	// get the selected attribute and new value from the form
	const token = JSON.parse(authToken);
	var username = document.getElementById('username').value;
	var attribute = document.getElementById('attribute').value;
	var newValue = document.getElementById('newValue').value;
	if (username === "" && token.role === "User") {
		username = token.username;
	}
	if (attribute === "" && newValue === "") {
		attribute = document.getElementById('attributeG').value;
		newValue = document.getElementById('newValueG').value;
	}
	// make a POST request to the server to modify the account attribute
	const xhr = new XMLHttpRequest();
	xhr.open('POST', '/rest/modify/v1');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
	xhr.onload = function() {
		if (xhr.status === 200) {
			// handle successful modify
			localStorage.setItem('authToken', xhr.responseText); // store the authentication token in localStorage
			alert('Account modified successfully');
			window.location.reload(); // refresh the page
		} else {
			// handle modify error
			alert('Account modification failed: ' + xhr.statusText);
		}
	};
	xhr.onerror = function() {
		alert('Request failed.');
	};
	xhr.send(JSON.stringify({ token: token, username: username, attribute: attribute, newValue: newValue }));
}

function submitPasswordChange() {
	const token = JSON.parse(authToken);
	var currentPassword = document.getElementById('currentPassword').value;
	var newPassword = document.getElementById('newPassword').value;
	var confirmPassword = document.getElementById('confirmPassword').value;

	if (newPassword !== confirmPassword) {
		alert('New passwords do not match. Please try again.');
		return;
	}

	var xhr = new XMLHttpRequest();
	xhr.open('POST', '/rest/modify/pwd');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
	xhr.onload = function() {
		if (xhr.status === 200) {
			// handle successful password change
			alert('Password changed successfully');
			window.location.href = '/pages/home.html'; // redirect to home page
		} else {
			// handle password change error
			alert('Password change failed: ' + xhr.statusText);
		}
	};
	xhr.onerror = function() {
		alert('Request failed.');
	};
	xhr.send(JSON.stringify({ token: token, password: currentPassword, newPassword: newPassword }));
}

function showNewOP(){
    const op9 = document.getElementById('OP9');
    const userRole = JSON.parse(authToken).role;
    if(userRole === "User" || userRole === "SU") {
        op9.style.display = 'block';
        showStats();
    }
}

function showContentRoleBased() {
	const userContent = document.getElementById('userContent');
	const gContent = document.getElementById('gContent');
	const userRole = JSON.parse(authToken).role;
	switch (userRole) {
		case 'User':
			userContent.style.display = 'block'; // show the content for regular users
			break;
		case 'GBO':
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		case 'GS':
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		case 'SU':
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		default:
			// handle unknown user roles here
			break;
	}
}

function showContentRoleDelete() {
	const userContent = document.getElementById('userContent');
	const gContent = document.getElementById('gContent');
	const userRole = JSON.parse(authToken).role;
	switch (userRole) {
		case 'User':
			userContent.style.display = 'block'; // show the content for regular users
			break;
		case 'GBO':
			userContent.style.display = 'block'; // show the content for regular users
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		case 'GS':
			userContent.style.display = 'block'; // show the content for regular users
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		case 'SU':
			userContent.style.display = 'block'; // show the content for regular users
			gContent.style.display = 'block'; // show the content for higher permission users
			break;
		default:
			// handle unknown user roles here
			break;
	}
}

function listUsers() {
	var userTableBody = document.getElementById("userTableBody");
	const userRole = JSON.parse(authToken).role;
	const xhr = new XMLHttpRequest();
	xhr.open("POST", '/rest/users/v1');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');

	xhr.onreadystatechange = function() {
		if (xhr.readyState === XMLHttpRequest.DONE) {
			if (xhr.status === 200) {
				const users = JSON.parse(xhr.responseText);
				for (const user of users) {
					const tr = document.createElement("tr");
					// Check the user's role and display the appropriate attributes
					if (userRole === "User") {
						const usernameTd = document.createElement("td");
						usernameTd.textContent = user.username;
						tr.appendChild(usernameTd);

						const emailTd = document.createElement("td");
						emailTd.textContent = user.email;
						tr.appendChild(emailTd);

						const nameTd = document.createElement("td");
						nameTd.textContent = user.name;
						tr.appendChild(nameTd);
					} else {
						const usernameTd = document.createElement("td");
						usernameTd.textContent = user.username;
						tr.appendChild(usernameTd);

						const emailTd = document.createElement("td");
						emailTd.textContent = user.email;
						tr.appendChild(emailTd);

						const nameTd = document.createElement("td");
						nameTd.textContent = user.name;
						tr.appendChild(nameTd);

						const privacyTd = document.createElement("td");
						privacyTd.textContent = user.privacy;
						tr.appendChild(privacyTd);

						const roleTd = document.createElement("td");
						roleTd.textContent = user.role;
						tr.appendChild(roleTd);

						const stateTd = document.createElement("td");
						stateTd.textContent = user.state;
						tr.appendChild(stateTd);

						userTableBody = document.getElementById("userTableBody2");
					}
					userTableBody.appendChild(tr);
				}
			} else {
				console.log("Error fetching user data");
			}
		}
	};
	xhr.send(authToken);
}

function showStats() {
	const statsTableBody = document.getElementById("statsTableBody");
	const xhr = new XMLHttpRequest();
	xhr.open("POST", '/rest/users/stats');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');

	xhr.onreadystatechange = function() {
		if (xhr.readyState === XMLHttpRequest.DONE) {
			if (xhr.status === 200) {
				const stats = JSON.parse(xhr.responseText);
				const tr = document.createElement("tr");

				const loginsTd = document.createElement("td");
				loginsTd.textContent = stats.logins;
				tr.appendChild(loginsTd);

				const failedLoginsTd = document.createElement("td");
				failedLoginsTd.textContent = stats.failedLogins;
				tr.appendChild(failedLoginsTd);

				const firstLoginTd = document.createElement("td");
				firstLoginTd.textContent = stats.firstLogin;
				tr.appendChild(firstLoginTd);

				const lastLoginTd = document.createElement("td");
				lastLoginTd.textContent = stats.lastLogin;
				tr.appendChild(lastLoginTd);

				const lastAttemptTd = document.createElement("td");
				lastAttemptTd.textContent = stats.lastAttempt;
				tr.appendChild(lastAttemptTd);
				statsTableBody.appendChild(tr);
			} else {
				console.log("Error fetching user data");
			}
		}
	};
	xhr.send(authToken);
}

function showInfo() {
	const userTable = document.getElementById("userTable");
	const xhr = new XMLHttpRequest();
	xhr.open("POST", '/rest/users/info');
	xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');

	xhr.onreadystatechange = function() {
		if (xhr.readyState === XMLHttpRequest.DONE) {
			if (xhr.status === 200) {
				const info = JSON.parse(xhr.responseText);
				const tr = document.createElement("tr");

				const usernameTd = document.createElement("td");
				usernameTd.textContent = info.username;
				tr.appendChild(usernameTd);

				const emailTd = document.createElement("td");
				emailTd.textContent = info.email;
				tr.appendChild(emailTd);

				const nameTd = document.createElement("td");
				nameTd.textContent = info.name;
				tr.appendChild(nameTd);

				const privacyTd = document.createElement("td");
				privacyTd.textContent = info.privacy;
				tr.appendChild(privacyTd);

				const roleTd = document.createElement("td");
				roleTd.textContent = info.role;
				tr.appendChild(roleTd);

				const stateTd = document.createElement("td");
                stateTd.textContent = info.state;
                tr.appendChild(stateTd);

				userTable.appendChild(tr);
			} else {
				console.log("Error fetching user data");
			}
		}
	};
	xhr.send(authToken);
}

function displayToken() {
	if (authToken) {
		const tokenDisplay = document.getElementById('tokenDisplay');
		tokenDisplay.textContent = `Auth Token: ${authToken}`;
	} else {
		alert('Your session is invalid');
		window.location.href = '/index.html';
	}
}

function logout() {
	var xhr = new XMLHttpRequest();
	xhr.open('POST', '/rest/logout/v1');
	xhr.setRequestHeader('Content-Type', 'application/json');
	xhr.onload = function() {
		if (xhr.status === 200) {
			// handle successful login
			localStorage.removeItem('authToken');
			alert('Logged out successfully!');
			window.location.href = '/index.html'; // redirect to index.html page
		} else {
			// handle login error
			alert('Logout failed: ' + xhr.statusText);
		}
	};
	xhr.onerror = function() {
		alert('Request failed.');
	};
	xhr.send(authToken);
}