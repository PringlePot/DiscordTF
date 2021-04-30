package com.github.derinn;

import net.kronos.rkon.core.Rcon;
import net.kronos.rkon.core.ex.AuthenticationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class RconHandler {

    /**
     * Disables freezecam
     * @param host host to disable on
     * @param port port of rcon
     * @param password rcon password
     */
    static void disableFreezecam(String host, Integer port, String password) {

        try {

            Rcon rcon = new Rcon(host, port, password.getBytes(StandardCharsets.UTF_8));
            rcon.connect(host, port, password.getBytes(StandardCharsets.UTF_8));
            rcon.command("tftrue_freezecam 0");

        } catch (IOException | AuthenticationException e) {
            e.printStackTrace();
        }

    }

}
