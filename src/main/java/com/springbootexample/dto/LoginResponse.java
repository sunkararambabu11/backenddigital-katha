package com.springbootexample.dto;

public class LoginResponse {
private String Message;
private Long userId;
private String shopName;
private String token;
public String getToken() {
	return token;
}
public void setToken(String token) {
	this.token = token;
}
public String getMessage() {
	return Message;
}
public void setMessage(String message) {
	Message = message;
}
public Long getUserId() {
	return userId;
}
public void setUserId(Long userId) {
	this.userId = userId;
}
public String getShopName() {
	return shopName;
}
public void setShopName(String shopName) {
	this.shopName = shopName;
}

}
