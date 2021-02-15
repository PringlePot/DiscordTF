package archive;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.*;

class LobbyManager{

    private static final String codeString = "```";
    private static final ArrayList<String> ultiduoClassList = new ArrayList<>(Arrays.asList("soldier", "medic"));
    private static final ArrayList<String> bballClassList = new ArrayList<>(Collections.singletonList("soldier"));
    private static final ArrayList<String> sixClassList = new ArrayList<>(Arrays.asList("scout", "soldier", "demoman", "medic"));
    private static final ArrayList<String> hlClassList = new ArrayList<>(Arrays.asList("scout", "soldier", "pyro", "demoman", "heavy", "engineer", "medic", "sniper", "spy"));
    private static final ArrayList<String> leagueArg = new ArrayList<>(Arrays.asList("etf2l", "ugc"));
    private static final ArrayList<String> typeArg = new ArrayList<>(Arrays.asList("ultiduo", "bball", "mge", "4", "6", "9"));
    private final static String stvPassword = "tv";
    //only lobbies waiting for players
    static HashMap<String, ArrayList<Lobby>> lobbiesInServer = new HashMap<>();
    //lobby message id per guild
    static HashMap<String, ArrayList<String>> lobbyMessages = new HashMap<>();
    static HashMap<String, TextChannel> lobbyChannel = new HashMap<>();
    //get class roles by adding reactions
    static HashMap<String, String> classMessageOfServer = new HashMap<>();
    //list of players in guild
    private static HashMap<String, ArrayList<String>> playersInServer = new HashMap<>();
    private static HashMap<String, Integer> playersReservation = new HashMap<>();

    static void createNewLobby(Guild guild, TextChannel textChannel, String memberID, int maxPlayers, String type){

        loadLobbyChannel(guild, textChannel);

        boolean reserveServer = false;
        String guildID = guild.getId();

        //check if user has serveme
        Document findDoc = new Document();
        findDoc.put("userid", memberID);
        if(DBConnection.findFromDatabase("tokenlist", findDoc) == null){
            textChannel.sendMessage("won't be able to reserve a server for you because you haven't set your serveme api token").queue();
        }else{
            reserveServer = true;
        }

        Integer lobbyID;

        if(lobbiesInServer.containsKey(guildID)){

            lobbyID = lobbiesInServer.get(guildID).size();
            Lobby lobby = new Lobby(guild, lobbyID, maxPlayers, memberID, reserveServer, type);
            lobbiesInServer.get(guildID).add(lobby);

        }else{

            lobbyID = 0;
            Lobby lobby = new Lobby(guild, lobbyID, maxPlayers, memberID, reserveServer, type);
            lobbiesInServer.put(guildID, new ArrayList<>());
            lobbiesInServer.get(guildID).add(lobby);

        }

        ArrayList<Integer> emptyArray = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0));

        lobbiesInServer.get(guildID).get(lobbyID).setRedClassAmounts(emptyArray);
        lobbiesInServer.get(guildID).get(lobbyID).setRedPlayers(new ArrayList<>());
        lobbiesInServer.get(guildID).get(lobbyID).setBluClassAmounts(emptyArray);
        lobbiesInServer.get(guildID).get(lobbyID).setBluPlayers(new ArrayList<>());

        lobbiesInServer.get(guild.getId()).get(lobbyID).setPlayers(0);

        lobbiesInServer.get(guildID).get(lobbyID).setTotalClassAmounts(emptyArray);

        if(lobbyChannel.get(guildID) == null){

            serializeGuildLobbies(guild, textChannel);

        }else{

            serializeGuildLobbies(guild, lobbyChannel.get(guildID));

        }

        if(guild.getRolesByName("Red #" + lobbyID, true).size() != 0){

            for(Role deleteRole : guild.getRolesByName("Red #" + lobbyID, true)){

                deleteRole.delete().queue();

            }

        }

        if(guild.getRolesByName("Blu #" + lobbyID, true).size() != 0){

            for(Role deleteRole : guild.getRolesByName("Blu #" + lobbyID, true)){

                deleteRole.delete().queue();

            }

        }

        if(guild.getRolesByName("Lobby #" + lobbyID, true).size() != 0){

            for(Role deleteRole : guild.getRolesByName("Lobby #" + lobbyID, true)){

                deleteRole.delete().queue();

            }

        }

        guild.createRole()
                .setName("Red #" + lobbyID)
                .setPermissions(0L)
                .setColor(Color.RED)
                .queue(role -> {
                    lobbiesInServer.get(guildID).get(lobbyID).setRedRole(role);


                    if(guild.getVoiceChannelsByName("Red Team", true).size() != 0){

                        for(VoiceChannel voiceChannel : guild.getVoiceChannelsByName("Red Team", true)){

                            if(voiceChannel.getParent() != null && voiceChannel.getParent().equals(guild.getCategoriesByName("lobby channels", true).get(0)) && voiceChannel.getMembers().size() == 0){

                                voiceChannel.delete().queue();

                            }

                        }

                    }
                    guild.createVoiceChannel("Red Team")
                            .addPermissionOverride(role, EnumSet.of(Permission.VOICE_CONNECT), null)
                            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VOICE_CONNECT))
                            .setParent(guild.getCategoriesByName("lobby channels", true).get(0))
                            .queue(voiceChannel -> lobbiesInServer.get(guildID).get(lobbyID).setRedChannel(voiceChannel));

                    /*if(emptyChannels == 1){

                        guild.createVoiceChannel("Red Team")
                                .addPermissionOverride(role, EnumSet.of(Permission.VOICE_CONNECT), null)
                                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VOICE_CONNECT))
                                .setParent(guild.getCategoriesByName("lobby channels", true).get(0))
                                .queue(voiceChannel -> lobbiesInServer.get(guildID).get(lobbyID).setRedChannel(voiceChannel));

                    }else{

                        //pick 1 delete others

                    }*/

                });

        guild.createRole()
                .setName("Blu #" + lobbyID)
                .setPermissions(0L)
                .setColor(Color.BLUE)
                .queue(role -> {
                    lobbiesInServer.get(guildID).get(lobbyID).setBluRole(role);

                    if(guild.getVoiceChannelsByName("Blu Team", true).size() != 0){

                        for(VoiceChannel voiceChannel : guild.getVoiceChannelsByName("Blu Team", true)){

                            if(voiceChannel.getParent() != null && voiceChannel.getParent().equals(guild.getCategoriesByName("lobby channels", true).get(0)) && voiceChannel.getMembers().size() == 0){

                                voiceChannel.delete().queue();

                            }

                        }

                    }
                    guild.createVoiceChannel("Blu Team")
                            .addPermissionOverride(role, EnumSet.of(Permission.VOICE_CONNECT), null)
                            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VOICE_CONNECT))
                            .setParent(guild.getCategoriesByName("lobby channels", true).get(0))
                            .queue(voiceChannel -> lobbiesInServer.get(guildID).get(lobbyID).setBluChannel(voiceChannel));

                });

        guild.createRole()
                .setName("Lobby #" + lobbyID)
                .setPermissions(0L)
                .setColor(Color.GRAY)
                .queue(role -> lobbiesInServer.get(guildID).get(lobbyID).setLobbyRole(role));
    }

    private static void serializeGuildLobbies(Guild guild, TextChannel textChannel){

        if(lobbiesInServer.containsKey(guild.getId()) && lobbiesInServer.get(guild.getId()).size() >= 1){

            for(Lobby lobby : lobbiesInServer.get(guild.getId())){

                String message;
                StringBuilder builder = new StringBuilder();

                //update message

                builder.append(codeString);
                builder.append("Lobby #").append(lobby.getLobbyID()).append("#");
                //100 charaacters 1 line
                if(lobby.getType().equalsIgnoreCase("basketball")){

                    ArrayList<Integer> classAmount = lobby.getTotalClassAmounts();
                    builder.append(" BASKETBALL").append("\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");
                    builder.append(classAmount.get(1)).append("/4 SOLDIER").append("\n");
                    builder.append(lobby.getPlayers()).append(("/")).append(lobby.getMaxPlayers()).append(" Players\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");

                }else if(lobby.getType().equalsIgnoreCase("ultiduo")){

                    ArrayList<Integer> classAmount = lobby.getTotalClassAmounts();
                    builder.append(" ULTIDUO").append("\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");
                    builder.append(classAmount.get(1)).append("/2 SOLDIER | ").append(classAmount.get(6)).append("/2 MEDIC").append("\n");
                    builder.append(lobby.getPlayers()).append(("/")).append(lobby.getMaxPlayers()).append(" Players\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");

                }else if(lobby.getType().equalsIgnoreCase("6s")){

                    ArrayList<Integer> classAmount = lobby.getTotalClassAmounts();
                    builder.append(" 6s").append("\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");
                    builder.append(classAmount.get(0)).append("/4 SCOUT | ").append(classAmount.get(1)).append("/4 SOLDIER | ").append(classAmount.get(3)).append("/2 DEMOMAN | ").append(classAmount.get(6)).append("/2 MEDIC").append("\n");
                    builder.append(lobby.getPlayers()).append(("/")).append(lobby.getMaxPlayers()).append(" Players\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");

                }else if(lobby.getType().equalsIgnoreCase("highlander")){

                    ArrayList<Integer> classAmount = lobby.getTotalClassAmounts();
                    builder.append(" HIGHLANDER").append("\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");
                    builder.append(classAmount.get(0)).append("/2 SCOUT | ").append(classAmount.get(1)).append("/2 SOLDIER | ")
                            .append(classAmount.get(2)).append("/2 PYRO | ").append(classAmount.get(3)).append("/2 DEMOMAN | ")
                            .append(classAmount.get(4)).append("/2 HEAVY | ").append(classAmount.get(5)).append("/2 ENGINEER | ")
                            .append(classAmount.get(6)).append("/2 MEDIC | ").append(classAmount.get(7)).append("/2 SNIPER | ")
                            .append(classAmount.get(8)).append("/2 SPY").append("\n");
                    builder.append(lobby.getPlayers()).append(("/")).append(lobby.getMaxPlayers()).append(" Players\n");
                    builder.append("--------------------------------------------------------------------------------------------------------------").append("\n");

                }

                builder.append(codeString);

                message = builder.toString();

                if(lobby.getLobbyMessage() == null){

                    textChannel.sendMessage(message).queue(messageObject -> {

                        if(lobbyMessages.containsKey(guild.getId())){

                            lobbyMessages.get(guild.getId()).add(messageObject.getId());

                        }else{

                            lobbyMessages.put(guild.getId(), new ArrayList<>());
                            lobbyMessages.get(guild.getId()).add(messageObject.getId());

                        }

                        lobby.setLobbyMessage(messageObject.getId());

                        reactClassesToMessage(guild, textChannel, messageObject.getId(), lobby.getType());

                    });

                }else{

                    textChannel.editMessageById(lobby.getLobbyMessage(), message).queue();

                }

            }

        }

    }

    private static void reactClassesToMessage(Guild guild, TextChannel textChannel, String messageID, String type){

        ArrayList<String> tempClasses;

        if(type.equalsIgnoreCase("basketball")){

            tempClasses = bballClassList;

        }else if(type.equalsIgnoreCase("ultiduo")){

            tempClasses = ultiduoClassList;

        }else if(type.equalsIgnoreCase("6s")){

            tempClasses = sixClassList;

        }else{

            tempClasses = hlClassList;

        }

        for(String classEmoteName : tempClasses){

            for(Emote emote : guild.getEmotes()){

                if(emote.getName().contains(classEmoteName)){

                    textChannel.retrieveMessageById(messageID).queue(message -> message.addReaction(emote).queue());

                }

            }

        }

    }

    static void joinLobby(Guild guild, TextChannel textChannel, String memberID, Integer lobbyID, String team, String classType){

        Member member = guild.getMemberById(memberID);

        if(member != null){

            if(lobbiesInServer.containsKey(guild.getId())){

                if(lobbiesInServer.get(guild.getId()).get(lobbyID) != null){

                    if(playersInServer.containsKey(guild.getId()) && playersInServer.get(guild.getId()).contains(memberID)){

                        guild.getTextChannelsByName("mix", true).get(0).sendMessage(member.getAsMention() + " you are already in a lobby/team").queue();
                        return;

                    }

                    if(team.equalsIgnoreCase("red")){

                        lobbiesInServer.get(guild.getId()).get(lobbyID).getRedPlayers().add(memberID);
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getRedClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getRedClassAmounts().get(hlClassList.indexOf(classType) + 1));
                        guild.addRoleToMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getRedRole()).queue();

                    }else{

                        lobbiesInServer.get(guild.getId()).get(lobbyID).getBluPlayers().add(memberID);
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getBluClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getBluClassAmounts().get(hlClassList.indexOf(classType) + 1));
                        guild.addRoleToMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getBluRole()).queue();

                    }

                    guild.addRoleToMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getLobbyRole()).queue();

                    lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().get(hlClassList.indexOf(classType)) + 1);
                    //lobbiesInServer.get(guild.getId()).get(lobbyID).setPlayers(lobbiesInServer.get(guild.getId()).get(lobbyID).getPlayers());
                    lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalPlayers().add(memberID);

                    if(!playersInServer.containsKey(guild.getId())){

                        playersInServer.put(guild.getId(), new ArrayList<>());

                    }

                    playersInServer.get(guild.getId()).add(memberID);

                    if(hlClassList.contains(classType)){

                        lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().get(hlClassList.indexOf(classType)));

                    }else{

                        return;

                    }

                    lobbiesInServer.get(guild.getId()).get(lobbyID).setPlayers(lobbiesInServer.get(guild.getId()).get(lobbyID).getPlayers() + 1);

                    serializeGuildLobbies(guild, textChannel);

                    if(lobbiesInServer.get(guild.getId()).get(lobbyID).getMaxPlayers().equals(lobbiesInServer.get(guild.getId()).get(lobbyID).getPlayers())){

                        guild.getTextChannelsByName("mix", true).get(0).sendMessage(lobbiesInServer.get(guild.getId()).get(lobbyID).getLobbyRole().getAsMention() + " lobby #" + lobbyID + " is full, moving everyone to their voice channels").queue();

                        for(String redPlayerID : lobbiesInServer.get(guild.getId()).get(lobbyID).getRedPlayers()){

                            guild.retrieveMemberById(redPlayerID).queue(redPlayer -> {
                                if(redPlayer.getVoiceState() != null && redPlayer.getVoiceState().inVoiceChannel()){

                                    guild.moveVoiceMember(redPlayer, lobbiesInServer.get(guild.getId()).get(lobbyID).getRedChannel()).queue();

                                }
                            });

                        }

                        for(String bluPlayerID : lobbiesInServer.get(guild.getId()).get(lobbyID).getBluPlayers()){

                            guild.retrieveMemberById(bluPlayerID).queue(bluPlayer -> {
                                if(bluPlayer.getVoiceState() != null && bluPlayer.getVoiceState().inVoiceChannel()){

                                    guild.moveVoiceMember(bluPlayer, lobbiesInServer.get(guild.getId()).get(lobbyID).getBluChannel()).queue();

                                }
                            });

                        }

                        if(lobbiesInServer.get(guild.getId()).get(lobbyID).getReserveServer()){

                            String type = "5cp";
                            String playerAmount;

                            if(lobbiesInServer.get(guild.getId()).get(lobbyID).getMaxPlayers() == 12){

                                playerAmount = "6";

                            }else{

                                playerAmount = "9";

                            }

                            String message = "-newserver etf2l " + playerAmount + " " + type;

                            guild.getTextChannelsByName("mix", true).get(0).sendMessage(member.getAsMention() + " attempting to reserve server with your token").queue();
                            reserveServer(member, guild.getTextChannelsByName("mix", true).get(0), message);

                        }

                        //full, start lobby, end it also
                        //delete message
                        endLobby(guild, textChannel, lobbyID);

                    }

                }

            }

        }

    }

    static void leaveLobby(Guild guild, String memberID, Integer lobbyID, String team, String classType, TextChannel textChannel){

        if(playersInServer.containsKey(guild.getId()) && playersInServer.get(guild.getId()).contains(memberID)){
            //same shit as joinlobby but just remove

            Member member = guild.getMemberById(memberID);
            if(member != null){

                if(lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalPlayers().contains(memberID)){

                    lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalPlayers().remove(memberID);
                    if(team.equalsIgnoreCase("red")){
                        guild.removeRoleFromMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getRedRole()).queue();
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getRedPlayers().remove(memberID);
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getRedClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getRedClassAmounts().get(hlClassList.indexOf(classType)) - 1);
                    }else{
                        guild.removeRoleFromMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getBluRole()).queue();
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getBluPlayers().remove(memberID);
                        lobbiesInServer.get(guild.getId()).get(lobbyID).getBluClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getBluClassAmounts().get(hlClassList.indexOf(classType)) - 1);
                    }

                    lobbiesInServer.get(guild.getId()).get(lobbyID).setPlayers(lobbiesInServer.get(guild.getId()).get(lobbyID).getPlayers());
                    playersInServer.get(guild.getId()).remove(memberID);
                    lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().set(hlClassList.indexOf(classType), lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalClassAmounts().get(hlClassList.indexOf(classType)) - 1);

                    serializeGuildLobbies(guild, textChannel);

                    guild.removeRoleFromMember(member, lobbiesInServer.get(guild.getId()).get(lobbyID).getLobbyRole()).queue();


                }

            }

        }

    }

    static void endLobby(Guild guild, TextChannel textChannel, Integer lobbyID){

        for(String memberID : lobbiesInServer.get(guild.getId()).get(lobbyID).getTotalPlayers()){

            playersInServer.get(guild.getId()).remove(memberID);

        }

        lobbiesInServer.get(guild.getId()).get(lobbyID).getLobbyRole().delete().queue();
        lobbiesInServer.get(guild.getId()).get(lobbyID).getRedRole().delete().queue();
        lobbiesInServer.get(guild.getId()).get(lobbyID).getBluRole().delete().queue();

        lobbiesInServer.get(guild.getId()).remove(lobbiesInServer.get(guild.getId()).get(lobbyID));
        if(lobbyMessages.containsKey(guild.getId())){

            lobbyMessages.get(guild.getId()).remove(lobbyMessages.get(guild.getId()).get(lobbyID));
            textChannel.retrieveMessageById(lobbiesInServer.get(guild.getId()).get(lobbyID).getLobbyMessage()).queue(message -> message.delete().queue());
            guild.getTextChannelsByName("mix", true).get(0).sendMessage("lobby #" + lobbyID + "has started/ended").queue();

        }

    }

    static void setLobbyChannel(Guild guild, TextChannel textChannel){

        Document insertDoc = new Document();
        insertDoc.put("guildid", guild.getId());

        FindIterable<Document> foundDocs = DBConnection.findFromDatabase("lobbychannellist", insertDoc);

        insertDoc.put("channelid", textChannel.getId());

        if(foundDocs == null){

            DBConnection.writeToDatabase("lobbychannellist", insertDoc);

        }else if(foundDocs.first() == null){

            DBConnection.writeToDatabase("lobbychannellist", insertDoc);

        }else{

            DBConnection.replaceInDatabase("lobbychannellist", foundDocs.first(), insertDoc);

        }

    }

    private static void loadLobbyChannel(Guild guild, TextChannel textChannel){

        Document insertDoc = new Document();
        insertDoc.put("guildid", guild.getId());

        FindIterable<Document> foundDocs = DBConnection.findFromDatabase("lobbychannellist", insertDoc);

        if(foundDocs == null){

            textChannel.sendMessage("you haven't set a main lobby channel yet do so with -setlobbychannel in the channel").queue();

        }else{
            Document found = foundDocs.first();
            if(found == null){
                textChannel.sendMessage("you haven't set a main lobby channel yet do so with -setlobbychannel in the channel").queue();
                return;
            }
            lobbyChannel.put(guild.getId(), guild.getTextChannelById(found.getString("channelid")));

        }

    }

    static void reserveServer(Member member, TextChannel textChannel, String messageContent){

        Document findByID = new Document();
        findByID.put("userid", member.getId());
        Document tokenDoc = DBConnection.findFromDatabase("tokenlist", findByID).first();
        Document flagDoc = DBConnection.findFromDatabase("flaglist", findByID).first();
        Document rconDoc = DBConnection.findFromDatabase("rconlist", findByID).first();

        if(tokenDoc == null){

            textChannel.sendMessage("you haven't set your serveme api token").queue();
            textChannel.sendMessage("dm me with \"settoken (token)\"").queue();
            return;

        }

        if(flagDoc == null){

            textChannel.sendMessage("you haven't set your preferred country").queue();
            textChannel.sendMessage("dm me with \"setflag (de,fr,nl)\"").queue();
            return;

        }

        if(rconDoc == null){

            textChannel.sendMessage("you haven't set your rcon password").queue();
            textChannel.sendMessage("dm me with \"setrcon (your rcon password)\"").queue();
            return;

        }

        String[] messageArgs = messageContent.split(" ");
        String mapName;

        if(messageArgs.length != 4 || !leagueArg.contains(messageArgs[1]) || !typeArg.contains(messageArgs[2])){


            textChannel.sendMessage("-newserver (etf2l, ugc) (ultiduo, bball, mge, 6, 9) (map name)").queue();
            return;

        }

        if(!messageArgs[3].contains("_")){

            textChannel.sendMessage("you need to use full names of the maps").queue();
            return;

        }else{

            mapName = messageArgs[3];

        }

        String servemeToken = tokenDoc.getString("token");
        String servemeFlag = flagDoc.getString("flag");
        String servemeRcon = rconDoc.getString("rcon");

        if(servemeToken == null || servemeFlag == null || servemeRcon == null){

            textChannel.sendMessage("couldn't find your token/flag/rcon, try resetting them").queue();
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
                try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"))){

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
                                                reservationBuilder.add("enable_plugins", true);
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

                                                try{

                                                    //if successful
                                                    if(response.getStatusLine().getStatusCode() == 200){

                                                        String responseJsonString;

                                                        //read response
                                                        try(BufferedReader bufferedResultReader = new BufferedReader(new InputStreamReader(response.getEntity().
                                                                getContent(), "utf-8"))){

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

                                                        String playerAndGuildCombined = member.getId() + "-" + member.getGuild();
                                                        playersReservation.put(playerAndGuildCombined, serverObject.getJsonObject("reservation").getInt("id"));

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
                                                        embedBuilder.setColor(Color.decode("#EC4E20"));

                                                        textChannel.sendMessage(embedBuilder.build()).queue();
                                                        return;

                                                    }else{

                                                        //System.out.println(response.getStatusLine());
                                                        //close response
                                                        response.close();
                                                        textChannel.sendMessage("couldn't reserve a server").queue();
                                                        return;

                                                    }

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

    static void endServer(Member member, TextChannel textChannel){

        //check then end
        String playerAndGuildCombined = member.getId() + "-" + member.getGuild();
        if(playersReservation.containsKey(playerAndGuildCombined)){

            Document findByID = new Document();
            findByID.put("userid", member.getId());
            Document tokenDoc = DBConnection.findFromDatabase("tokenlist", findByID).first();

            if(tokenDoc == null){

                textChannel.sendMessage("you haven't set your serveme api token").queue();
                textChannel.sendMessage("dm me with \"settoken (token)\"").queue();
                return;

            }

            String servemeToken = tokenDoc.getString("token");

            String urlString = "https://serveme.tf/api/reservations/" + playersReservation.get(playerAndGuildCombined) + "?api_key=" + servemeToken;

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

                        textChannel.sendMessage("ended reservation").queue();
                        playersReservation.remove(playerAndGuildCombined);

                    }else if(httpresponse.getStatusLine().getStatusCode() == 204){

                        textChannel.sendMessage("reservation already ended").queue();

                    }

                }

            }catch(IOException e){
                e.printStackTrace();
                textChannel.sendMessage("something went wrong").queue();
            }

        }else{

            textChannel.sendMessage("couldn't find your last reservation").queue();

        }

    }


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
