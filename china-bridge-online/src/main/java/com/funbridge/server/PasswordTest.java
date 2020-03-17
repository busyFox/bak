package com.funbridge.server;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordTest {
    public static void main(String[] args) {
        System.out.println(cryptPassword("qiwenxin19970629"));
    }
    public static String cryptPassword(@NotNull String password){
        String cryptedPass = null;

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Change this to UTF-16 if needed ??????????UTF-16
            md.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            cryptedPass = String.format("%064x", new BigInteger(1, digest));

        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        return cryptedPass;
    }
}
