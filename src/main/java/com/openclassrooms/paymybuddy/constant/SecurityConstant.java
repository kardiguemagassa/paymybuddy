package com.openclassrooms.paymybuddy.constant;

public class SecurityConstant {
    public static final String[] PUBLIC_URLS = {
            "/",
            "/index/**",
            "/register","/register/**",
            "/login/**",
            "/error",
            "/resources/**",
            "/assets/**",
            "/css/**",
            "/webjars/**",
            "/js/**"
    };

    public static final String[] PRIVATE_URL = {
            "/transactions/**",
            "/profile/**",
            "/addRelationship/**",
            "/historic/**"
    };
}
