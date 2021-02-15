package com.github.derinn;

import net.kronos.rkon.core.Rcon;
import net.kronos.rkon.core.ex.AuthenticationException;

import java.io.IOException;

class RconHandler{

    static void disableFreezecam(String host, Integer port, String password){

        try{

            Rcon rcon = new Rcon(host, port, password.getBytes("UTF-8"));
            rcon.connect(host, port, password.getBytes("UTF-8"));
            rcon.command("tftrue_freezecam 0");

        }catch(IOException | AuthenticationException e){
            e.printStackTrace();
        }

        //tftrue_freezecam

    }

}
