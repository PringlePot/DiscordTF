package com.github.derinn;

import com.mongodb.client.FindIterable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.bson.Document;

import javax.json.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

class NewLobbyManager{

    //discord code string
    private static final String codeString = "```";
    private static final ArrayList<String> leagueArg = new ArrayList<>(Arrays.asList("etf2l", "ugc"));
    private static final ArrayList<String> typeArg = new ArrayList<>(Arrays.asList("ultiduo", "bball", "mge", "4", "6", "9"));
    private final static String stvPassword = "tv";
    //class lists
    private static final ArrayList<String> ultiduoClassList = new ArrayList<>(Arrays.asList("soldier", "medic"));
    private static final ArrayList<String> bballClassList = new ArrayList<>(Collections.singletonList("soldier"));
    private static final ArrayList<String> sixClassList = new ArrayList<>(Arrays.asList("scout", "soldier", "demoman", "medic"));
    private static final ArrayList<String> hlClassList = new ArrayList<>(Arrays.asList("scout", "soldier", "pyro", "demoman", "heavy", "engineer", "medic", "sniper", "spy"));
    //max class amounts
    private static final ArrayList<Integer> bballMaxClasses = new ArrayList<>(Arrays.asList(0, 4, 0, 0, 0, 0, 0, 0, 0));
    private static final ArrayList<Integer> ultiduoMaxClasses = new ArrayList<>(Arrays.asList(0, 2, 0, 0, 0, 0, 2, 0, 0));
    private static final ArrayList<Integer> sixesMaxClasses = new ArrayList<>(Arrays.asList(4, 4, 0, 2, 0, 0, 2, 0, 0));
    private static final ArrayList<Integer> hlMaxClasses = new ArrayList<>(Arrays.asList(2, 2, 2, 2, 2, 2, 2, 2, 2));
    //lobby messages in guild, guildid (lobbyid, messageid)
    static HashMap<String, HashMap<Integer, String>> lobbyMessagesInServer = new HashMap<>();
    //serveme stuff
    private static final HashMap<String, ArrayList<Integer>> playersReservation = new HashMap<>();
    private static final HashMap<String, Integer> playerReservationTries = new HashMap<>();
    //list of players in lobbies in guild
    private static final HashMap<String, ArrayList<Member>> playersInServer = new HashMap<>();
    //lobbies in guild, guildid lobby
    static HashMap<String, HashMap<Integer, NewLobby>> lobbiesInServer = new HashMap<>();

    /**
     * Creates a lobby
     * @param guild discord guild
     * @param textChannel discord text channel
     * @param type lobby type
     * @param league lobby league
     * @param map lobby map
     * @param reserveServer if there should be a reserve server
     * @param host the host of the lobby
     * @param maxPlayers the max amount of players
     */
    static void createLobby(Guild guild, TextChannel textChannel, String type, String league, String map, Boolean reserveServer, Member host, Integer maxPlayers){

        if(!lobbiesInServer.containsKey(guild.getId()) || (lobbiesInServer.containsKey(guild.getId()) && lobbiesInServer.get(guild.getId()).keySet().size() < 3)){

            Integer id = 0;

            if(lobbiesInServer.containsKey(guild.getId())){
                id = lobbiesInServer.get(guild.getId()).size();
            }else{
                lobbiesInServer.put(guild.getId(), new HashMap<>());
            }

            NewLobby lobby = new NewLobby(id, type, league, map, reserveServer, host, maxPlayers);

            String textName = "lobby-" + id;
            String redName = "Red #" + id;
            String bluName = "Blu #" + id;

            List<TextChannel> textChannelList = guild.getTextChannelsByName(textName, true);
            for(TextChannel tempText : textChannelList){
                tempText.delete().queue();
            }

            List<Role> redRoleList = guild.getRolesByName(redName, true);
            List<Role> bluRoleList = guild.getRolesByName(bluName, true);

            for(Role redRole : redRoleList){
                redRole.delete().queue();

            }
            for(Role bluRole : bluRoleList){
                bluRole.delete().queue();
            }

            guild.createTextChannel(textName)
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_READ))
                    .queue(lobbyTextChannel -> {
                        lobby.setTextChannel(lobbyTextChannel);
                        guild.createRole().setName(redName).setColor(Color.RED).queue(role -> {
                            lobby.setRedRole(role);
                            lobbyTextChannel.createPermissionOverride(role).queue(permissionOverride -> permissionOverride.getManager().grant(Permission.MESSAGE_READ).queue());
                        });

                        guild.createRole().setName(bluName).setColor(Color.BLUE).queue(role -> {
                            lobby.setBluRole(role);
                            lobbyTextChannel.createPermissionOverride(role).queue(permissionOverride -> permissionOverride.getManager().grant(Permission.MESSAGE_READ).queue());
                        });
                    });

            lobbiesInServer.get(guild.getId()).put(id, lobby);
            updateLobbyMessages(guild, textChannel, id, false);

        }else{

            textChannel.sendMessage("There are already 3 lobbies in this guild, wait for them to fill up first").queue();

        }

    }

    /**
     * Updates the lobby messages
     * @param guild discord guild
     * @param textChannel discord textchannel
     * @param id the id
     * @param ending if the match is ending
     */
    private static void updateLobbyMessages(Guild guild, TextChannel textChannel, Integer id, Boolean ending){

        String type = lobbiesInServer.get(guild.getId()).get(id).getType();
        String guildID = guild.getId();

        ArrayList<Member> combinedPlayers = lobbiesInServer.get(guildID).get(id).getCombinedPlayers();

        ArrayList<ArrayList<String>> redPlayers = lobbiesInServer.get(guildID).get(id).getRedPlayersOfClasses();
        ArrayList<ArrayList<String>> bluPlayers = lobbiesInServer.get(guildID).get(id).getBluPlayersOfClasses();

        String regex = "[\\[\\]]";

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.decode("#55828B"));

        String bluPlayersOutOfMax = lobbiesInServer.get(guildID).get(id).getBluPlayers().size() + "/" + (lobbiesInServer.get(guildID).get(id).getMaxPlayers() / 2);
        String redPlayersOutOfMax = lobbiesInServer.get(guildID).get(id).getRedPlayers().size() + "/" + (lobbiesInServer.get(guildID).get(id).getMaxPlayers() / 2);

        String mapName;
        if(lobbiesInServer.get(guildID).get(id).getReserveServer()){
            mapName = lobbiesInServer.get(guildID).get(id).getMap();
        }else{
            mapName = "No Map";
        }

        if(type.equalsIgnoreCase("bball")){
            embedBuilder.setTitle("BBall Lobby #" + id + " - " + combinedPlayers.size() + "/4" + " - " + mapName);
        }else if(type.equalsIgnoreCase("ultiduo")){
            embedBuilder.setTitle("Ultiduo Lobby #" + id + " - " + combinedPlayers.size() + "/4" + " - " + mapName);
        }else if(type.equalsIgnoreCase("6s")){
            embedBuilder.setTitle("6s Lobby #" + id + " - " + combinedPlayers.size() + "/12" + " - " + mapName);
        }else if(type.equalsIgnoreCase("highlander")){
            embedBuilder.setTitle("Highlander Lobby #" + id + " - " + combinedPlayers.size() + "/18" + " - " + mapName);
        }

        if(ending){

            embedBuilder.setDescription("Lobby is starting\nThis message will be deleted in 1 minute");

        }

        embedBuilder.addField("Blu - " + bluPlayersOutOfMax, bluPlayers.toString().replaceAll(regex, "").replaceAll(", ", ""), true);
        embedBuilder.addField("Red - " + redPlayersOutOfMax, redPlayers.toString().replaceAll(regex, "").replaceAll(", ", ""), true);

        if(lobbyMessagesInServer.containsKey(guildID) && lobbyMessagesInServer.get(guildID).containsKey(id)){

            textChannel.retrieveMessageById(lobbyMessagesInServer.get(guildID).get(id)).queue(message -> message.editMessage(embedBuilder.build()).queue(editedMessage -> {
                if(ending){
                    endLobby(guild, textChannel, id, false);
                    editedMessage.delete().queueAfter(1, TimeUnit.MINUTES);
                    lobbyMessagesInServer.get(guildID).remove(id);
                }
            }));

        }else{

            if(!lobbyMessagesInServer.containsKey(guildID)){
                lobbyMessagesInServer.put(guildID, new HashMap<>());
            }
            textChannel.sendMessage(embedBuilder.build()).queue(message -> {
                addClassReactions(guild, message, type);
                lobbyMessagesInServer.get(guildID).put(id, message.getId());
                message.pin().queue();
            });

        }

    }

    /**
     * Adds reactions for classes
     * @param guild the discord guild
     * @param message the message
     * @param type the lobby type
     */
    private static void addClassReactions(Guild guild, Message message, String type){


        ArrayList<String> tempClasses;


        switch (type){
            case "bball":{
                tempClasses = bballClassList;
                break;
            }
            case "ultiduo":{
                tempClasses = ultiduoClassList;
                break;
            }
            case "6s":{
                tempClasses = sixClassList;
                break;
            }
            default:{
                tempClasses = hlClassList;
            }

        }
        for(String classEmoteName : tempClasses){

            for(Emote emote : guild.getEmotes()){

                if(emote.getName().contains(classEmoteName)){

                    message.addReaction(emote).queue();

                }

            }

        }

    }

    /**
     * Joins a lobby
     * @param guild discord guild
     * @param textChannel text channel
     * @param id id
     * @param member member
     * @param reaction the reaction
     */
    static void joinLobby(Guild guild, TextChannel textChannel, Integer id, Member member, MessageReaction.ReactionEmote reaction){

        if(!playersInServer.containsKey(guild.getId())){
            playersInServer.put(guild.getId(), new ArrayList<>());
        }

        String guildID = guild.getId();

        if(!playersInServer.get(guildID).contains(member)){

            if(lobbiesInServer.containsKey(guildID) && lobbiesInServer.get(guildID).get(id) != null){


                //lobbiesInServer.get(guild.getId()).get(id)
                String className = reaction.getName().substring(3);

                if(hlClassList.contains(className)){

                    int classID = hlClassList.indexOf(className);
                    String type = lobbiesInServer.get(guildID).get(id).getType();
                    int possibleClassAmount = lobbiesInServer.get(guildID).get(id).getCombinedClassAmounts().get(classID) + 1;

                    if(type.equalsIgnoreCase("bball") && possibleClassAmount > bballMaxClasses.get(classID)){
                        return;
                    }else if(type.equalsIgnoreCase("ultiduo") && possibleClassAmount > ultiduoMaxClasses.get(classID)){
                        return;
                    }else if(type.equalsIgnoreCase("6s") && possibleClassAmount > sixesMaxClasses.get(classID)){
                        return;
                    }else if(type.equalsIgnoreCase("highlander") && possibleClassAmount > hlMaxClasses.get(classID)){
                        return;
                    }

                    playersInServer.get(guildID).add(member);

                    String serialized = "<:" + reaction.getName() + ":" + reaction.getId() + ">";

                    if(reaction.getName().contains("red")){
                        lobbiesInServer.get(guildID).get(id).getRedPlayers().add(member);
                        guild.addRoleToMember(member, lobbiesInServer.get(guildID).get(id).getRedRole()).queue();
                        lobbiesInServer.get(guildID).get(id).getRedPlayersOfClasses().get(classID).add(serialized + " " + member.getEffectiveName() + "\n");
                    }else{
                        lobbiesInServer.get(guildID).get(id).getBluPlayersOfClasses().get(classID).add(serialized + " " + member.getEffectiveName() + "\n");
                        lobbiesInServer.get(guildID).get(id).getBluPlayers().add(member);
                        guild.addRoleToMember(member, lobbiesInServer.get(guildID).get(id).getBluRole()).queue();
                    }

                    lobbiesInServer.get(guildID).get(id).getCombinedPlayers().add(member);
                    lobbiesInServer.get(guildID).get(id).getCombinedClassAmounts().set(classID, possibleClassAmount);

                    if(lobbiesInServer.get(guildID).get(id).getCombinedPlayers().size() == lobbiesInServer.get(guildID).get(id).getMaxPlayers()){

                        startLobby(guild, textChannel, id);
                        updateLobbyMessages(guild, textChannel, id, true);

                    }else{

                        updateLobbyMessages(guild, textChannel, id, false);

                    }

                }

            }

        }

    }

    /**
     * Leaves a lobby
     * @param guild guild
     * @param textChannel text channel
     * @param id the id
     * @param member the member
     * @param reaction the reaction emote
     */
    static void leaveLobby(Guild guild, TextChannel textChannel, Integer id, Member member, MessageReaction.ReactionEmote reaction){

        if(!playersInServer.containsKey(guild.getId())){
            playersInServer.put(guild.getId(), new ArrayList<>());
        }

        String guildID = guild.getId();

        if(playersInServer.get(guildID).contains(member)){

            if(lobbiesInServer.containsKey(guildID) && lobbiesInServer.get(guildID).get(id) != null){

                String className = reaction.getName().substring(3);

                String team = reaction.getName().substring(0, 3);
                String serialized = "<:" + reaction.getName() + ":" + reaction.getId() + ">";

                if(hlClassList.contains(className) && ((team.equalsIgnoreCase("red") && lobbiesInServer.get(guildID).get(id).getRedPlayers().contains(member)) || (team.equalsIgnoreCase("blu") && lobbiesInServer.get(guildID).get(id).getBluPlayers().contains(member)))){

                    playersInServer.get(guildID).remove(member);
                    if(reaction.getName().contains("red")){
                        lobbiesInServer.get(guildID).get(id).getRedPlayers().remove(member);
                        guild.removeRoleFromMember(member, lobbiesInServer.get(guildID).get(id).getRedRole()).queue();
                        lobbiesInServer.get(guildID).get(id).getRedPlayersOfClasses().get(hlClassList.indexOf(className)).remove(serialized + " " + member.getEffectiveName() + "\n");
                    }else{
                        lobbiesInServer.get(guildID).get(id).getBluPlayersOfClasses().get(hlClassList.indexOf(className)).remove(serialized + " " + member.getEffectiveName() + "\n");
                        lobbiesInServer.get(guildID).get(id).getBluPlayers().remove(member);
                        guild.removeRoleFromMember(member, lobbiesInServer.get(guildID).get(id).getBluRole()).queue();
                    }

                    lobbiesInServer.get(guildID).get(id).getCombinedPlayers().remove(member);
                    lobbiesInServer.get(guildID).get(id).getCombinedClassAmounts().set(hlClassList.indexOf(className), lobbiesInServer.get(guildID).get(id).getCombinedClassAmounts().get(hlClassList.indexOf(className)) - 1);

                    updateLobbyMessages(guild, textChannel, id, false);

                }

            }

        }

    }

    /**
     * Starts a lobby
     * @param guild the discord guild
     * @param textChannel the discord text channel
     * @param id the id
     */
    private static void startLobby(Guild guild, TextChannel textChannel, Integer id){

        //if reserveserver
        if(lobbiesInServer.get(guild.getId()).get(id).getReserveServer()){

            String servemeType;
            servemeType = lobbiesInServer.get(guild.getId()).get(id).getType().toLowerCase(Locale.ROOT);
            if(lobbiesInServer.get(guild.getId()).get(id).getType().equalsIgnoreCase("highlander")){
                servemeType = "9";
            }else if(lobbiesInServer.get(guild.getId()).get(id).getType().equalsIgnoreCase("6s")){
                servemeType = "6";
            }

            String args = "-newserver " + lobbiesInServer.get(guild.getId()).get(id).getLeague() + " " + servemeType + " " + lobbiesInServer.get(guild.getId()).get(id).getMap();
            if(lobbiesInServer.get(guild.getId()).get(id).getTextChannel() == null){
                reserveServer(lobbiesInServer.get(guild.getId()).get(id).getHost(), textChannel, args);
            }else{
                reserveServer(lobbiesInServer.get(guild.getId()).get(id).getHost(), lobbiesInServer.get(guild.getId()).get(id).getTextChannel(), args);
            }

        }

    }

    /**
     * Ends a lobby
     * @param guild the discord guild
     * @param textChannel the discord text channel
     * @param id the game id
     * @param deleteMessage should it delete the message
     */
    static void endLobby(Guild guild, TextChannel textChannel, Integer id, Boolean deleteMessage){

        if(lobbiesInServer.containsKey(guild.getId())){

            if(deleteMessage){

                if(lobbyMessagesInServer.containsKey(guild.getId()) && lobbyMessagesInServer.get(guild.getId()).containsKey(id)){

                    textChannel.retrieveMessageById(lobbyMessagesInServer.get(guild.getId()).get(id)).queue(message -> message.delete().queue());
                    lobbyMessagesInServer.get(guild.getId()).remove(id);

                }

                String textName = "lobby-" + id;
                String redName = "Red #" + id;
                String bluName = "Blu #" + id;

                List<TextChannel> textChannels = guild.getTextChannelsByName(textName, true);
                for(TextChannel foundText : textChannels){
                    foundText.delete().queue();
                }

                List<Role> redRoles = guild.getRolesByName(redName, true);
                for(Role redRole : redRoles){
                    redRole.delete().queue();
                }
                List<Role> bluRoles = guild.getRolesByName(bluName, true);
                for(Role bluRole : bluRoles){
                    bluRole.delete().queue();
                }

            }

            if(lobbiesInServer.get(guild.getId()).get(id) != null && lobbiesInServer.get(guild.getId()).get(id).getCombinedPlayers() != null && lobbiesInServer.get(guild.getId()).get(id).getCombinedPlayers().size() != 0){

                for(Member member : lobbiesInServer.get(guild.getId()).get(id).getCombinedPlayers()){
                    playersInServer.get(guild.getId()).remove(member);
                }

                lobbiesInServer.get(guild.getId()).remove(id);

            }

        }

    }

    /**
     * Reserve a server
     * @param member the member
     * @param textChannel textchannel
     * @param messageContent messageContent
     */
    static void reserveServer(Member member, TextChannel textChannel, String messageContent){

        Document findByID = new Document();
        findByID.put("userid", member.getId());
        Document tokenDoc = DBConnection.findFromDatabase("tokenlist", findByID).first();
        Document flagDoc = DBConnection.findFromDatabase("flaglist", findByID).first();
        Document rconDoc = DBConnection.findFromDatabase("rconlist", findByID).first();

        if(tokenDoc == null){

            textChannel.sendMessage("You haven't set your serveme api token").queue();
            textChannel.sendMessage("Dm me with \"settoken (token)\"").queue();
            return;

        }

        if(flagDoc == null){

            textChannel.sendMessage("You haven't set your preferred country").queue();
            textChannel.sendMessage("Dm me with \"setflag (de,fr,nl)\"").queue();
            return;

        }

        if(rconDoc == null){

            textChannel.sendMessage("You haven't set your rcon password").queue();
            textChannel.sendMessage("Dm me with \"setrcon (your rcon password)\"").queue();
            return;

        }

        String playerAndGuildCombined = member.getId() + "-" + textChannel.getGuild().getId();

        if(playersReservation.containsKey(playerAndGuildCombined) && playersReservation.get(playerAndGuildCombined).size() == 5){

            if(playerReservationTries.containsKey(playerAndGuildCombined) && playerReservationTries.get(playerAndGuildCombined) == 5){

                textChannel.sendMessage("Bot is spamming serveme.tf, shutting down").queue();
                messageCourtier();
                return;

            }

            textChannel.sendMessage("Only 5 reservations allowed per person through the bot").queue();
            return;

        }

        if(!playersReservation.containsKey(playerAndGuildCombined)){
            playersReservation.put(playerAndGuildCombined, new ArrayList<>());
        }

        if(!playerReservationTries.containsKey(playerAndGuildCombined)){
            playerReservationTries.put(playerAndGuildCombined, 0);
        }

        String[] messageArgs = messageContent.split(" ");
        String mapName;

        if(messageArgs.length != 4 || !leagueArg.contains(messageArgs[1]) || !typeArg.contains(messageArgs[2])){


            textChannel.sendMessage("-newserver (etf2l, ugc) (ultiduo, bball, mge, 6, 9) (map name)").queue();
            return;

        }

        if(!messageArgs[3].contains("_")){

            textChannel.sendMessage("You need to use full names of the maps").queue();
            return;

        }else{

            mapName = messageArgs[3];

        }

        String servemeToken = tokenDoc.getString("token");
        String servemeFlag = flagDoc.getString("flag");
        String servemeRcon = rconDoc.getString("rcon");

        if(servemeToken == null || servemeFlag == null || servemeRcon == null){

            textChannel.sendMessage("Couldn't find your token/flag/rcon, try resetting them").queue();
            return;

        }

        String configName = "";
        String whitelistName = "";

        //pick config name
        if(messageArgs[1].equalsIgnoreCase("etf2l")){

            if(messageArgs[2].equalsIgnoreCase("ultiduo")){

                whitelistName = "etf2l_whitelist_ultiduo.txt";
                configName = "etf2l_ultiduo";

            }else if(messageArgs[2].equalsIgnoreCase("bball")){

                whitelistName = "etf2l_whitelist_bball.txt";
                configName = "etf2l_bball";

            }else if(messageArgs[2].equalsIgnoreCase("mge")){

                whitelistName = "";
                configName = "ugc_HL_koth";

            }else if(messageArgs[2].equalsIgnoreCase("6")){

                whitelistName = "etf2l_whitelist_6v6.txt";

                if(messageArgs[3].startsWith("cp")){

                    configName = "etf2l_6v6_5cp";

                }else if(messageArgs[3].startsWith("koth")){

                    configName = "etf2l_6v6_koth";

                }

            }else if(messageArgs[2].equalsIgnoreCase("9")){

                whitelistName = "etf2l_whitelist_9v9.txt";

                if(messageArgs[3].startsWith("cp")){

                    configName = "etf2l_9v9_5cp";

                }else if(messageArgs[3].startsWith("koth")){

                    configName = "etf2l_9v9_koth";

                }

            }

        }else if(messageArgs[1].equalsIgnoreCase("ugc")){

            if(messageArgs[2].equalsIgnoreCase("mge")){

                whitelistName = "";
                configName = "ugc_HL_koth";

            }else if(messageArgs[2].equalsIgnoreCase("4")){

                whitelistName = "item_whitelist_ugc_4v4.txt";

                if(messageArgs[3].startsWith("cp")){

                    configName = "ugc_4v_standard";

                }else if(messageArgs[3].startsWith("koth")){

                    configName = "ugc_4v_koth";

                }

            }else if(messageArgs[2].equalsIgnoreCase("6")){

                whitelistName = "item_whitelist_ugc_6v6.txt";

                if(messageArgs[3].startsWith("cp")){

                    configName = "ugc_6v_standard";

                }else if(messageArgs[3].startsWith("koth")){

                    configName = "ugc_6v_koth";

                }

            }else if(messageArgs[2].equalsIgnoreCase("9")){

                whitelistName = "item_whitelist_ugc_HL.txt";

                if(messageArgs[3].startsWith("cp")){

                    configName = "ugc_HL_standard";

                }else if(messageArgs[3].startsWith("koth")){

                    configName = "ugc_HL_koth";

                }

            }

        }

        try{

            String urlString = "https://serveme.tf/api/reservations/new?api_key=" + servemeToken;

            //set up the httpclient
            CloseableHttpClient httpclient = HttpClientBuilder.create().setUserAgent("Mozilla/5.0 Firefox/26.0").setRedirectStrategy(new LaxRedirectStrategy()).build();
            //get request
            HttpGet httpget = new HttpGet(urlString);
            //execute get, save the response
            HttpResponse httpresponse = httpclient.execute(httpget);
            //read the response
            Scanner scanner = new Scanner(httpresponse.getEntity().getContent());

            //response as json
            String newReservationJsonString;
            //define builder
            StringBuilder builder = new StringBuilder();

            //while there is still more response to be read
            while(scanner.hasNext()){
                //add to the string builder
                builder.append(scanner.nextLine());
            }

            //build the response into a json string
            newReservationJsonString = builder.toString();
            //create json reader and read the response
            JsonReader reader = Json.createReader(new StringReader(newReservationJsonString));
            //convert json string into object
            JsonObject reservationObject = reader.readObject();
            //close the reader
            reader.close();
            //make a reservation json object as string
            String reservationJson = "{\"reservation\":" + reservationObject.get("reservation") + "}";

            urlString = "https://serveme.tf/api/reservations/find_servers?api_key=" + servemeToken;

            //json to be sent as body of post request
            StringEntity entity = new StringEntity(reservationJson);

            //post request
            HttpPost httpPost = new HttpPost(urlString);
            //accept json as response
            httpPost.setHeader("Accept", "application/json");
            //body is json
            httpPost.setHeader("Content-Type", "application/json");
            //set body as reservation json
            httpPost.setEntity(entity);
            //execute post request and get the response
            CloseableHttpResponse response = httpclient.execute(httpPost);

            //check if good
            if(response.getStatusLine().getStatusCode() == 200){

                //try reading the response
                try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))){

                    //define builder
                    StringBuilder responseBuilder = new StringBuilder();
                    //response as json string
                    String responseLine;

                    //append to builder
                    while((responseLine = bufferedReader.readLine()) != null){
                        responseBuilder.append(responseLine.trim());
                    }

                    bufferedReader.close();

                    //builder to string
                    String serverJsonString = responseBuilder.toString();

                    //close the response
                    response.close();

                    //read repsonse with server list
                    reader = Json.createReader(new StringReader(serverJsonString));
                    //save it to json object
                    JsonObject jsonWithLists = reader.readObject();
                    //close reader
                    reader.close();

                    //get list of servers
                    JsonArray serverList = jsonWithLists.getJsonArray("servers");
                    //server object
                    JsonObject serverObj;

                    //check if there are servers available
                    if(serverList.size() > 0){

                        //loop through server list
                        for(int i = 0; i < serverList.size(); i++){

                            //set server object
                            serverObj = serverList.getJsonObject(i);

                            //check if server matches flag, if couldnt find flag, pick the last one
                            if(serverObj.getString("flag").equalsIgnoreCase(servemeFlag) || (!serverObj.getString("flag").equalsIgnoreCase(servemeFlag) && i == (serverList.size() - 1))){

                                //choose server id
                                int serverID = serverObj.getInt("id");

                                //get list of configs
                                JsonArray configList = jsonWithLists.getJsonArray("server_configs");
                                //config object
                                JsonObject configObj;

                                //loop through configs
                                for(int cfgX = 0; cfgX < configList.size(); cfgX++){

                                    //set config object
                                    configObj = configList.getJsonObject(cfgX);

                                    String objectName = configObj.getString("file");

                                    //if config contains type of league / etf2l, rgl, classic
                                    if(objectName.equalsIgnoreCase(configName)){

                                        //choose config id
                                        int configID = configObj.getInt("id");

                                        //get whitelist list
                                        JsonArray whitelistList = jsonWithLists.getJsonArray("whitelists");
                                        //whitelist object
                                        JsonObject whitelistObj;

                                        //loop through whitelists
                                        for(int whtX = 0; whtX < whitelistList.size(); whtX++){

                                            //set whitelist object
                                            whitelistObj = whitelistList.getJsonObject(whtX);

                                            //if whitelist contains type of league / etf2l, rgl, classic
                                            //if(whitelistObj.getString("file").contains(messageArgs[1])){

                                            //if whitelist equals whitelist name
                                            if(whitelistObj.getString("file").equalsIgnoreCase(whitelistName) || whitelistName.equalsIgnoreCase("")){

                                                //choose whitelist id
                                                int whitelistID = whitelistObj.getInt("id");

                                                String joinPassword = generatePassword();

                                                //build json body to post
                                                JsonObjectBuilder reservationBuilder = Json.createObjectBuilder();
                                                reservationBuilder.add("starts_at", reservationObject.getJsonObject("reservation").getString("starts_at"));
                                                reservationBuilder.add("ends_at", reservationObject.getJsonObject("reservation").getString("ends_at"));
                                                reservationBuilder.add("rcon", servemeRcon);
                                                reservationBuilder.add("password", joinPassword);
                                                reservationBuilder.add("tv_password", joinPassword);
                                                if(!mapName.startsWith("mge")){
                                                    reservationBuilder.add("whitelist_id", whitelistID);
                                                }
                                                reservationBuilder.add("server_id", serverID);
                                                reservationBuilder.add("first_map", mapName);
                                                reservationBuilder.add("server_config_id", configID);
                                                reservationBuilder.add("auto_end", true);

                                                urlString = "https://serveme.tf/api/reservations?api_key=" + servemeToken;

                                                //body json to string
                                                String serverJsonStr = "{\"reservation\":" + reservationBuilder.build().toString() + "}";

                                                //set post body
                                                entity = new StringEntity(serverJsonStr);

                                                //post request
                                                httpPost = new HttpPost(urlString);
                                                //accept json response
                                                httpPost.setHeader("Accept", "application/json");
                                                //post body is json
                                                httpPost.setHeader("Content-Type", "application/json");
                                                //set body
                                                httpPost.setEntity(entity);
                                                //execute post
                                                response = httpclient.execute(httpPost);

                                                int tries = playerReservationTries.get(playerAndGuildCombined) + 1;
                                                playerReservationTries.put(playerAndGuildCombined, tries);

                                                try{

                                                    //if successful
                                                    if(response.getStatusLine().getStatusCode() == 200){

                                                        String responseJsonString;

                                                        //read response
                                                        try(BufferedReader bufferedResultReader = new BufferedReader(new InputStreamReader(response.getEntity().
                                                                getContent(), StandardCharsets.UTF_8))){

                                                            //response as json string
                                                            responseBuilder = new StringBuilder();
                                                            while((responseLine = bufferedResultReader.readLine()) != null){
                                                                responseBuilder.append(responseLine.trim());
                                                            }
                                                            bufferedResultReader.close();

                                                            responseJsonString = responseBuilder.toString();

                                                        }catch(Exception ex){
                                                            textChannel.sendMessage("reserved server but couldn't get information about it").queue();
                                                            return;
                                                        }

                                                        //close response
                                                        //response.close();
                                                        reader = Json.createReader(new StringReader(responseJsonString));
                                                        JsonObject serverObject = reader.readObject();

                                                        //System.out.println(responseJsonString);

                                                        playersReservation.get(playerAndGuildCombined).add(serverObject.getJsonObject("reservation").getInt("id"));

                                                        String ip = serverObject.getJsonObject("reservation").getJsonObject("server").getString("ip");
                                                        String port = serverObject.getJsonObject("reservation").getJsonObject("server").getString("port");
                                                        String stvPort = Integer.toString(Integer.parseInt(port) + 5);

                                                        String password = serverObject.getJsonObject("reservation").getString("password");

                                                        String normalConnectLink = "steam://connect/" + ip + ":" + port + "/" + password;
                                                        String stvConnectLink = "steam://connect/" + ip + ":" + stvPort + "/" + stvPassword;

                                                        String normalConnect = "connect " + ip + ":" + port + "; password " + password;
                                                        String stvConnect = "connect " + ip + ":" + stvPort + "; password " + stvPassword;

                                                        EmbedBuilder embedBuilder = new EmbedBuilder();
                                                        embedBuilder.setDescription("join server: " + normalConnectLink +
                                                                "\njoin sourcetv: " + stvConnectLink +
                                                                "\nconnect: " +
                                                                codeString + normalConnect + codeString +
                                                                "sourcetv connect:" +
                                                                codeString + stvConnect + codeString);
                                                        embedBuilder.setColor(Color.decode("#11B5E4"));

                                                        textChannel.sendMessage(embedBuilder.build()).queue();

                                                        if(messageArgs[2].equalsIgnoreCase("bball") || messageArgs[2].equalsIgnoreCase("mge")){

                                                            RconHandler.disableFreezecam(ip, Integer.parseInt(port), password);

                                                        }

                                                    }else{

                                                        //System.out.println(response.getStatusLine());
                                                        //close response
                                                        response.close();
                                                        textChannel.sendMessage("couldn't reserve a server").queue();

                                                    }
                                                    return;

                                                }catch(Exception ex){
                                                    ex.printStackTrace();
                                                }finally{
                                                    response.close();
                                                }

                                            }

                                            //}

                                        }

                                    }

                                }

                            }

                        }

                    }else{

                        textChannel.sendMessage("there are no servers available for you").queue();

                    }

                }catch(IOException e){
                    e.printStackTrace();
                }

            }else{

                //System.out.println(response.getStatusLine());
                textChannel.sendMessage("couldn't reserve a server").queue();

            }

        }catch(IOException e){
            e.printStackTrace();
        }

    }

    /**
     * end a server
     * @param member the member
     * @param textChannel the text channel
     */
    static void endServer(Member member, TextChannel textChannel){

        //check then end
        String playerAndGuildCombined = member.getId() + "-" + textChannel.getGuild().getId();
        if(playersReservation.containsKey(playerAndGuildCombined) && playersReservation.get(playerAndGuildCombined).size() != 0){

            Document findByID = new Document();
            findByID.put("userid", member.getId());
            FindIterable<Document> tokenDocs = DBConnection.findFromDatabase("tokenlist", findByID);

            if(tokenDocs == null){

                textChannel.sendMessage("you haven't set your serveme api token").queue();
                textChannel.sendMessage("dm me with \"settoken (token)\"").queue();
                return;

            }

            Document tokenDoc = tokenDocs.first();

            if(tokenDoc == null){
                textChannel.sendMessage("you haven't set your serveme api token").queue();
                textChannel.sendMessage("dm me with \"settoken (token)\"").queue();
                return;
            }

            String servemeToken = tokenDoc.getString("token");

            String urlString = "https://serveme.tf/api/reservations/" + playersReservation.get(playerAndGuildCombined).get(playersReservation.get(playerAndGuildCombined).size() - 1) + "?api_key=" + servemeToken;

            //set up the httpclient
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //get request
            HttpGet httpget = new HttpGet(urlString);
            try{

                //execute get, save the response
                HttpResponse httpresponse = httpclient.execute(httpget);
                //read the response
                Scanner scanner = new Scanner(httpresponse.getEntity().getContent());

                //response as json
                String newReservationJsonString;
                //define builder
                StringBuilder builder = new StringBuilder();

                //while there is still more response to be read
                while(scanner.hasNext()){
                    //add to the string builder
                    builder.append(scanner.nextLine());
                }

                //build the response into a json string
                newReservationJsonString = builder.toString();
                //create json reader and read the response
                JsonReader reader = Json.createReader(new StringReader(newReservationJsonString));
                //convert json string into object
                JsonObject reservationObject = reader.readObject();
                //close the reader
                reader.close();
                //make a reservation json object as string
                boolean ended = reservationObject.getJsonObject("reservation").getBoolean("ended");

                if(ended){
                    textChannel.sendMessage("reservation already ended").queue();
                }else{

                    HttpDelete httpDelete = new HttpDelete(urlString);
                    //accept json response
                    httpDelete.setHeader("Accept", "application/json");
                    //post body is json
                    httpDelete.setHeader("Content-Type", "application/json");
                    //set body
                    //execute post
                    httpresponse = httpclient.execute(httpDelete);
                    if(httpresponse.getStatusLine().getStatusCode() == 200){

                        textChannel.sendMessage("Ended reservation").queue();
                        playersReservation.remove(playerAndGuildCombined);

                    }else if(httpresponse.getStatusLine().getStatusCode() == 204){

                        textChannel.sendMessage("Reservation already ended").queue();

                    }

                }

            }catch(IOException e){
                e.printStackTrace();
                textChannel.sendMessage("Something went wrong").queue();
            }

        }else{

            textChannel.sendMessage("Couldn't find your last reservation").queue();

        }
    }

    /**
     * Message courtier that its crashed lol
     */
    private static void messageCourtier(){
        File file = new File("crashed.txt");
        try{
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("bot is fucked folk");
            fileWriter.close();
            System.exit(0);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Generate a random password
     * @return the generated password
     */
    private static String generatePassword(){
        String password;
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 6; i++){
            builder.append(random.nextInt(9));
        }
        password = builder.toString();
        return password;
    }

}
