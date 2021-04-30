package com.github.derinn;

import com.mongodb.client.FindIterable;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

class MessageHandler extends ListenerAdapter {

    /**
     * Main message handler
     * @param event event
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || event.getAuthor().isFake()){
            return;
        }
        Message message = event.getMessage();

        String messageContent = message.getContentRaw();


        if (event.isFromType(ChannelType.TEXT) ) {

            TextChannel textChannel = event.getTextChannel();

            if (messageContent.startsWith("connect")) {

                String[] editedMessage = messageContent.split(" ");
                if (editedMessage.length == 4 && messageContent.contains(";") && messageContent.contains("password")) {

                    Document findDoc = new Document();
                    findDoc.put("guildid", event.getGuild().getId());
                    FindIterable<Document> foundDocs = DBConnection.findFromDatabase("connectstatuslist", findDoc);
                    if (foundDocs != null) {

                        Document foundDoc = foundDocs.first();
                        if (foundDoc != null && foundDoc.getBoolean("enabled") != null && !foundDoc.getBoolean("enabled")) {
                            return;
                        }

                    }

                    findDoc = new Document();
                    findDoc.put("guildid", event.getGuild().getId());

                    String connectLink = "steam://connect/" + editedMessage[1].replace(";", "") + "/" + editedMessage[3];

                    foundDocs = DBConnection.findFromDatabase("connectchannellist", findDoc);
                    if (foundDocs != null) {

                        Document foundDoc = foundDocs.first();
                        if (foundDoc != null && foundDoc.getString("channelid") != null) {
                            TextChannel foundChannel = event.getGuild().getTextChannelById(foundDoc.getString("channelid"));
                            if (foundChannel != null) {
                                foundChannel.sendMessage(connectLink).queue();
                            }
                        }

                    } else {

                        textChannel.sendMessage(connectLink).queue();

                    }

                }

            }

            if (messageContent.startsWith("-")) {

                if (messageContent.startsWith("-frostyserver")) {

                    textChannel.sendMessage("steam://connect/94.156.35.43:27015/frosty").queue();

                } else if (messageContent.startsWith("-matches")) {

                    String[] messageArgs = messageContent.split(" ");

                    Document findByID = new Document();
                    findByID.put("userid", event.getAuthor().getId());
                    FindIterable<Document> teamDocs = DBConnection.findFromDatabase("teamlist", findByID);

                    if (teamDocs == null) {

                        textChannel.sendMessage("you haven't set your etf2l team id").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                        textChannel.sendMessage("dm me with \"setteam (etf2l team id)\"").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                        return;

                    }

                    Document teamDoc = teamDocs.first();

                    if (teamDoc == null) {

                        textChannel.sendMessage("you haven't set your etf2l team id").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                        textChannel.sendMessage("dm me with \"setteam (etf2l team id)\"").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                        return;

                    }

                    if (messageArgs.length == 2) {

                        if (ETF2LScraper.doesTeamExist(messageArgs[1]) == 1) {

                            textChannel.sendMessage("couldn't find etf2l team by that id").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                            return;

                        }
                        if (ETF2LScraper.doesTeamExist(messageArgs[1]) == 2) {

                            textChannel.sendMessage("something went wrong while loading etf2l").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                            return;

                        }


                        ETF2LScraper.getMatchesForTeam(textChannel, messageArgs[1]);


                    } else {

                        ETF2LScraper.getMatchesForTeam(textChannel, teamDoc.getString("teamid"));

                    }

                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                } else if (messageContent.startsWith("-togglehoster")) {

                    Document findByID = new Document();
                    findByID.put("guildid", event.getGuild().getId());

                    FindIterable<Document> foundDocs = DBConnection.findFromDatabase("hosterstatuslist", findByID);

                    if (foundDocs != null && foundDocs.first() != null) {

                        Document foundDoc = foundDocs.first();
                        if (foundDoc != null) {

                            boolean input;
                            input = !foundDoc.getBoolean("enabled");
                            Document inputDoc = new Document();
                            inputDoc.put("guildid", event.getGuild().getId());
                            inputDoc.put("enabled", input);

                            DBConnection.safeWriteToDatabase("hosterstatuslist", findByID, inputDoc);

                            if (input) {
                                event.getChannel().sendMessage("Only people with the hoster role or admins can host lobbies").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                            } else {
                                event.getChannel().sendMessage("Everyone can host lobbies").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                            }

                        }

                    } else {

                        Document inputDoc = new Document();
                        inputDoc.put("guildid", event.getGuild().getId());
                        inputDoc.put("enabled", false);

                        DBConnection.safeWriteToDatabase("hosterstatuslist", findByID, inputDoc);

                        event.getChannel().sendMessage("Everyone can host lobbies").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                } else if (messageContent.startsWith("-setconnect")) {

                    if (event.getMember() != null && event.getMember().isOwner()) {

                        Document findByID = new Document();
                        findByID.put("guildid", event.getGuild().getId());

                        Document inputDoc = new Document();
                        inputDoc.put("guildid", event.getGuild().getId());
                        inputDoc.put("channelid", event.getTextChannel().getId());

                        DBConnection.safeWriteToDatabase("connectchannellist", findByID, inputDoc);

                        event.getChannel().sendMessage("connect channel set").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                } else if (messageContent.startsWith("-toggleconnect")) {

                    Document findByID = new Document();
                    findByID.put("guildid", event.getGuild().getId());

                    FindIterable<Document> foundDocs = DBConnection.findFromDatabase("connectstatuslist", findByID);

                    if (foundDocs != null && foundDocs.first() != null) {

                        Document foundDoc = foundDocs.first();
                        if (foundDoc != null) {
                            boolean input;
                            input = !foundDoc.getBoolean("enabled");
                            Document inputDoc = new Document();
                            inputDoc.put("guildid", event.getGuild().getId());
                            inputDoc.put("enabled", input);

                            DBConnection.safeWriteToDatabase("connectstatuslist", findByID, inputDoc);

                            event.getChannel().sendMessage("connect links enabled: " + input).queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                        }

                    } else {

                        Document inputDoc = new Document();
                        inputDoc.put("guildid", event.getGuild().getId());
                        inputDoc.put("enabled", false);

                        DBConnection.safeWriteToDatabase("connectstatuslist", findByID, inputDoc);

                        event.getChannel().sendMessage("connect links enabled: " + false).queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                } else if (messageContent.startsWith("-newserver")) {

                    Member member = event.getMember();

                    if (member != null) {

                        NewLobbyManager.reserveServer(member, event.getTextChannel(), messageContent);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                    }

                } else if (messageContent.startsWith("-endserver")) {

                    Member member = event.getMember();

                    if (member != null) {

                        NewLobbyManager.endServer(member, event.getTextChannel());
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

                    }

                }

            }

        } else if (event.isFromType(ChannelType.PRIVATE) ) {

            if (messageContent.startsWith("connect")) {

                String[] editedMessage = messageContent.split(" ");
                if (editedMessage.length == 4) {

                    String connectLink = "steam://connect/" + editedMessage[1].replace(";", "") + "/" + editedMessage[3];
                    //System.out.println(connectLink);
                    event.getChannel().sendMessage(connectLink).queue();

                }

            } else if (messageContent.startsWith("settoken")) {

                String[] editedMessage = messageContent.split(" ");
                if (editedMessage.length == 2) {

                    Document findByID = new Document();
                    findByID.put("userid", event.getAuthor().getId());

                    Document inputDoc = new Document();
                    inputDoc.put("userid", event.getAuthor().getId());
                    inputDoc.put("token", editedMessage[1]);

                    DBConnection.safeWriteToDatabase("tokenlist", findByID, inputDoc);

                    event.getChannel().sendMessage("token set").queue();

                } else {

                    event.getChannel().sendMessage("settoken (your serveme api token)").queue();

                }

            } else if (messageContent.startsWith("setflag")) {

                String[] editedMessage = messageContent.split(" ");
                if (editedMessage.length == 2) {

                    if (editedMessage[1].equalsIgnoreCase("de") || editedMessage[1].equalsIgnoreCase("nl") || editedMessage[1].equalsIgnoreCase("fr")) {

                        Document findByID = new Document();
                        findByID.put("userid", event.getAuthor().getId());

                        Document inputDoc = new Document();
                        inputDoc.put("userid", event.getAuthor().getId());
                        inputDoc.put("flag", editedMessage[1]);

                        DBConnection.safeWriteToDatabase("flaglist", findByID, inputDoc);

                        event.getChannel().sendMessage("flag set").queue();

                    } else {

                        event.getChannel().sendMessage("flag needs to be one of de/fr/nl").queue();

                    }

                } else {

                    event.getChannel().sendMessage("setflag (de, fr, nl)").queue();

                }

            } else if (messageContent.startsWith("setrcon")) {

                String[] editedMessage = messageContent.split(" ");
                if (editedMessage.length == 2) {

                    Document findByID = new Document();
                    findByID.put("userid", event.getAuthor().getId());

                    Document inputDoc = new Document();
                    inputDoc.put("userid", event.getAuthor().getId());
                    inputDoc.put("rcon", editedMessage[1]);

                    DBConnection.safeWriteToDatabase("rconlist", findByID, inputDoc);

                    event.getChannel().sendMessage("rcon set").queue();

                } else {

                    event.getChannel().sendMessage("setrcon (your rcon password)").queue();

                }

            } else if (messageContent.startsWith("setteam")) {

                String[] messageArgs = messageContent.split(" ");

                if (messageArgs.length == 2) {

                    Document findByID = new Document();
                    findByID.put("userid", event.getAuthor().getId());

                    Document inputDoc = new Document();
                    inputDoc.put("userid", event.getAuthor().getId());
                    inputDoc.put("teamid", messageArgs[1]);

                    if (ETF2LScraper.doesTeamExist(messageArgs[1]) == 1) {

                        event.getChannel().sendMessage("couldn't find etf2l team by that id").queue();
                        return;

                    }
                    if (ETF2LScraper.doesTeamExist(messageArgs[1]) == 2) {

                        event.getChannel().sendMessage("something went wrong while loading etf2l").queue();
                        return;

                    }

                    DBConnection.safeWriteToDatabase("teamlist", findByID, inputDoc);

                    event.getChannel().sendMessage("team set").queue();

                } else {

                    event.getChannel().sendMessage("setteam (etf2l team id)").queue();

                }

            } else if (messageContent.startsWith("help")) {

                event.getChannel().sendMessage(
                        "```markdown\n" +
                                "# TF2Helper\n" +
                                "# if the bot doesn't react to pug messages, add these emotes to your server https://files.catbox.moe/2uqxp9.zip, make sure to use the exact same names\n" +
                                "# remove parantheses, if there are multiple things inside, pick one\n\n" +
                                "# =========================Private Commands========================\n" +
                                "help -> get some help\n\n" +

                                "setteam (etf2l team id) -> set your etf2l team\n\n" +

                                "setrcon (your rcon) -> set your serveme rcon password\n" +
                                "setflag (preffered country/de,nl,fr) -> set your preferred server location\n" +
                                "settoken (serveme api token) -> set your serveme api token\n" +
                                "```"
                ).queue();
                event.getChannel().sendMessage(
                        "```markdown\n" +
                                "# =========================Guild Commands========================\n" +
                                "-setconnect -> set the channel you posted to as the connect channel, the bot will post connect links here, if not set the bot will reply in the same channel\n" +
                                "-toggleconnect -> enable/disable connect links [ENABLED BY DEFAULT]\n\n" +

                                "-togglehost -> if enabled people will need to have a role named Hoster to start lobbies [ENABLED BY DEFAULT]\n" +
                                "-givehost (tag person/people) -> gives Hoster role to the mentioned people (can be multiple), if the role doesn't exist, the bot creates it\n" +
                                "-takehost (tag person/people) -> take away people's Hoster role\n\n" +

                                "-newserver (etf2l, ugc) (ultiduo, bball, mge, 6, 9) (full map name) -> reserve a serveme server, you have to set your rcon password, flag and token for this to work\n" +
                                "# etf2l game modes -> ultiduo, bball, mge, 6, 9\n" +
                                "# ugc game modes -> mge, 4, 6, 9\n" +
                                "-endserver -> ends your last reservation in that guild, you can repeatedly send this and end all reservations\n\n" +

                                "# bot executes tftrue_freezecam 0 for the server if the gamemode is bball or mge for you\n\n" +

                                "-newlobby (ultiduo, bball, 6, 9) (noserver, etf2l, ugc) (full map name) -> start a pug, people can join by reacting to the bot's message\n" +
                                "# noserver if you don't want the bot to reserve a server for the lobby\n" +
                                "# person who starts the lobby needs a role named Hoster, or needs to be administrator if enabled\n" +
                                "-endlobby (lobby id) -> ends/cancels the lobby, id is the number after the hashtag\n\n" +

                                "-matches (etf2l team id) -> gets your team's upcoming etf2l matches, if team id is empty, bot will check matches for the team you set in private messages\n" +
                                "```"
                ).queue();

            }

        }

    }

}
