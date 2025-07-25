package com.openclassrooms.paymybuddy.constant;

public final class SecurityConstant {

    private SecurityConstant() {
        throw new UnsupportedOperationException("this class is not meant to be instantiated");
    }
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

    public static final String[] PRIVATE_URLS = {
            "/transactions/**",
            "/profile/**",
            "/addRelationship/**",
            "/historic/**"
    };
}
