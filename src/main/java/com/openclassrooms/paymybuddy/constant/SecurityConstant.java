package com.openclassrooms.paymybuddy.constant;

public class SecurityConstant {
    public static final String FORBIDDEN_MESSAGE = "Vous devez vous connecter pour accéder à cette page";
    public static final String ACCESS_DENIED_MESSAGE = "Vous n'avez pas la permission d'accéder à cette page";
    public static final String OPTIONS_HTTP_METHOD = "OPTIONS";
    public static final String[] PUBLIC_URLS = {"/",
            "/index/**", "/css/**", "/register/**", "/webjars/**", "/resources/**", "/assets/**","/js/**","/error"};
}
