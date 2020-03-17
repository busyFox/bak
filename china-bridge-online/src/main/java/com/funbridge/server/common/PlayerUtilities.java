package com.funbridge.server.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by nboujnah on 25/03/2019.
 */

@Component(value="playerUtilities")
@Scope(value="singleton")
public class PlayerUtilities {

    protected static Logger log = LogManager.getLogger(PlayerUtilities.class);

    /**
     * crypt password
     * @param password
     * @return the crypted password
     */
    public static String cryptPassword(@NotNull String password){
        String cryptedPass = null;

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Change this to UTF-16 if needed ??????????UTF-16
            md.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            cryptedPass = String.format("%064x", new BigInteger(1, digest));

        } catch(NoSuchAlgorithmException e){
            log.error("The password can't be crypted",e.getMessage());
        }

        return cryptedPass;
    }
}
