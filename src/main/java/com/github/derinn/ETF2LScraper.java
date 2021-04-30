package com.github.derinn;

import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

class ETF2LScraper{

    /**
     * Fetch matches by textchannel and ID
     * @param textChannel the text channel
     * @param teamID the team ID
     */
    static void getMatchesForTeam(TextChannel textChannel, String teamID){

        try{

            String urlString = "https://api.etf2l.org/team/" + teamID + "/matches?only_scheduled=1";

            //set up the httpclient
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //get request
            HttpGet httpget = new HttpGet(urlString);
            //accept json
            httpget.setHeader("Accept", "application/json");
            //execute get, save the response
            HttpResponse httpresponse = httpclient.execute(httpget);
            //read the response
            Scanner scanner = new Scanner(httpresponse.getEntity().getContent());

            //response as json string
            String teamJsonString;
            //define builder
            StringBuilder builder = new StringBuilder();

            //while there is still more response to be read
            while(scanner.hasNext()){
                //add to the string builder
                builder.append(scanner.nextLine());
            }

            //build the response into a json string
            teamJsonString = builder.toString();

            //create json reader and read the response
            JsonReader reader = Json.createReader(new StringReader(teamJsonString));
            //convert json string into object
            JsonObject teamObject = reader.readObject();
            //close the reader
            reader.close();

            if(teamObject.getJsonObject("status").getString("message").equalsIgnoreCase("OK")){

                //JsonArray matches = teamObject.getJsonArray("matches");

                for(JsonValue match :teamObject.getJsonArray("matches")){

                    JsonObject matchObject = match.asJsonObject();

                    Date date = new java.util.Date(matchObject.getInt("time") * 1000L);
                    SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+2"));
                    String formattedDate = sdf.format(date);

                    textChannel.sendMessage(
                            "```markdown\n" +
                                    "# " + matchObject.getJsonObject("division").getString("name") + " Match\n" +
                                    "===========================================\n" +
                                    formattedDate + "\n" +
                                    "[" + matchObject.getJsonObject("clan1").getString("name") + "](https://etf2l.org/teams/" + matchObject.getJsonObject("clan1").getInt("id") + ")\n" +
                                    "[" + matchObject.getJsonObject("clan2").getString("name") + "](https://etf2l.org/teams/" + matchObject.getJsonObject("clan2").getInt("id") + ")\n" +
                                    "```"
                    ).complete();

                }

            }else{

                textChannel.sendMessage("something went wrong while loading etf2l").complete();

            }

        }catch(IOException ex){
            textChannel.sendMessage("something went wrong while getting matches").complete();
        }

    }


    /**
     * Checks if the team exists
     * @param teamID the team id
     * @return 1 or 0
     */
    static int doesTeamExist(String teamID){

        try{

            String urlString = "https://api.etf2l.org/team/" + teamID + "/matches?only_scheduled=1";

            //set up the httpclient
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //get request
            HttpGet httpget = new HttpGet(urlString);
            //execute get, save the response
            HttpResponse httpresponse = httpclient.execute(httpget);
            //read the response
            Scanner scanner = new Scanner(httpresponse.getEntity().getContent());

            //response as json string
            String teamJsonString;
            //define builder
            StringBuilder builder = new StringBuilder();

            //while there is still more response to be read
            while(scanner.hasNext()){
                //add to the string builder
                builder.append(scanner.nextLine());
            }

            //build the response into a json string
            teamJsonString = builder.toString();
            //create json reader and read the response
            JsonReader reader = Json.createReader(new StringReader(teamJsonString));
            //convert json string into object
            JsonObject teamObject = reader.readObject();
            //close the reader
            reader.close();

            if(teamObject.getJsonObject("status").getString("message").equalsIgnoreCase("Team does not exist.")){

                return 1;

            }else{

                return 0;

            }

        }catch(IOException ex){
            return 2;
        }

    }

}
