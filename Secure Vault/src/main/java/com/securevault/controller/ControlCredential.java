package com.securevault.controller;

public class ControlCredential {
    public String validateCredential(String website, String username, String password ){
        if(website == null || website.isBlank() ||
                username == null || username.isBlank() ||
                password == null || password.isBlank()){
            return "Please fill in all credentials";

        }
        return "Credentials are valid";
    }
    public String passwordStrength(String password){
        if(password == null || password.isBlank()){

            return "";
        }
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        if(password.length() >= 12 && hasDigit && hasUppercase && hasSpecialChar && hasLowercase){
            return "Strong";
        }
        if(password.length() >= 8 && hasDigit && hasLowercase ){
            return "Medium";
        }
        return "Weak";
    }
}
