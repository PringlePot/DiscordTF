package com.github.derinn;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.util.Scanner;

public class Bot{

    public static void main(String[] args){

        if(args.length != 4){

            System.out.println("need to input discord token, mongodb database name, user, and password");
            System.exit(1);

        }

        try{

            JDA jda = JDABuilder.createDefault(args[0])
                    .addEventListeners(new MessageHandler())
                    .addEventListeners(new NewMixer())
                    .setActivity(Activity.playing("dm me help"))
                    .build();
            jda.awaitReady();
            System.out.println("bot is running");

            DBConnection.establishConnection(args[1], args[2], args[3]);

            Scanner scanner = new Scanner(System.in);

            String input = scanner.nextLine();

            if(input.equalsIgnoreCase("exit")){

                jda.shutdown();

            }

        }catch(LoginException | InterruptedException loginException){

            loginException.printStackTrace();

        }

    }

}