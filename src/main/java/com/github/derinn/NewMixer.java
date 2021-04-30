package com.github.derinn;

import com.mongodb.client.FindIterable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NewMixer extends ListenerAdapter {

    private final ArrayList<String> lobbyTypes = new ArrayList<>(Arrays.asList("experimental", "bball", "basketball", "ultiduo", "6", "9", "hl", "highlander"));

    /**
     * Gets called on message receivement. Handles commands
     *
     * @param event The messagereceived event from JDA
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Member member = event.getMember();
        if (!event.getAuthor().isBot() && member != null) {

            if (event.getMessage().getContentRaw().startsWith("-newlobby")) {


                boolean isHosterEnabled = isHosterEnabled(event);

                if (!isHosterEnabled) {
                    lobbyContinue(event);
                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                    return;
                } else if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) {
                    lobbyContinue(event);
                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                    return;
                }

                List<Role> hostRole = event.getGuild().getRolesByName("Hoster", true);
                for (Role role : hostRole) {

                    if (member.getRoles().contains(role)) {

                        lobbyContinue(event);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;

                    }

                }

            } else if (event.getMessage().getContentRaw().startsWith("-endlobby")) {

                String[] args = event.getMessage().getContentRaw().split(" ");

                if (args.length == 2 && (args[1].equalsIgnoreCase("0") || args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("2"))) {

                    boolean isHosterEnabled = isHosterEnabled(event);


                    if (!isHosterEnabled) {
                        NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;
                    } else if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) {
                        NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;
                    }

                    List<Role> hostRole = event.getGuild().getRolesByName("Hoster", true);
                    for (Role role : hostRole) {

                        if (member.getRoles().contains(role)) {
                            NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                            event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                            return;
                        }

                    }

                } else {

                    event.getTextChannel().sendMessage("-endlobby (lobby id)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            } else if (event.getMessage().getContentRaw().startsWith("-givehoster")) {

                if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) {

                    if (event.getMessage().getMentionedMembers().size() != 0) {

                        List<Role> hosterRole = event.getGuild().getRolesByName("Hoster", true);
                        StringBuilder mentionedPeople = new StringBuilder();
                        if (hosterRole.isEmpty()) {
                            event.getGuild().createRole().setName("Hoster").setColor(Color.decode("#7EBDC2")).queue(role -> {
                                for (Member mentionedMember : event.getMessage().getMentionedMembers()) {
                                    event.getGuild().addRoleToMember(mentionedMember, role).queue();
                                    mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                                }
                            });
                        } else {
                            for (Member mentionedMember : event.getMessage().getMentionedMembers()) {
                                event.getGuild().addRoleToMember(mentionedMember, hosterRole.get(0)).queue();
                                mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                            }
                        }

                        event.getTextChannel().sendMessage("Gave Hoster role to " + mentionedPeople).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            } else if (event.getMessage().getContentRaw().startsWith("-takehoster")) {

                if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) {

                    if (event.getMessage().getMentionedMembers().size() != 0) {

                        List<Role> hosterRole = event.getGuild().getRolesByName("Hoster", true);
                        StringBuilder mentionedPeople = new StringBuilder();
                        if (hosterRole.isEmpty()) {
                            event.getGuild().createRole().setName("Hoster").setColor(Color.decode("#7EBDC2")).queue();
                        } else {
                            for (Member mentionedMember : event.getMessage().getMentionedMembers()) {
                                event.getGuild().removeRoleFromMember(mentionedMember, hosterRole.get(0)).queue();
                                mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                            }
                        }


                        event.getTextChannel().sendMessage("Removed Hoster role from " + mentionedPeople).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            }

        }

    }

    /**
     * Checks if the hosterlist contains the guildId
     *
     * @param event The message event
     * @return returns  if the hoster IS enabled
     */
    private boolean isHosterEnabled(@NotNull MessageReceivedEvent event) {
        Document findByID = new Document();
        findByID.put("guildid", event.getGuild().getId());

        FindIterable<Document> foundDocs = DBConnection.findFromDatabase("hosterstatuslist", findByID);
        boolean isHosterEnabled = true;

        if (foundDocs != null && foundDocs.first() != null) {

            Document foundDoc = foundDocs.first();
            if (foundDoc != null) {
                isHosterEnabled = foundDoc.getBoolean("enabled");
            }

        }
        return isHosterEnabled;
    }

    /**
     * Gets called on reaction added
     *
     * @param event The messagereactionAddEvent
     */
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {

        Member author = event.getMember();

        if (event.getUser() != null && !event.getUser().isBot() && author != null) {

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if (NewLobbyManager.lobbyMessagesInServer.containsKey(event.getGuild().getId()) && NewLobbyManager.lobbyMessagesInServer.get(event.getGuild().getId()).containsValue(message.getId())) {

                    event.getReaction().retrieveUsers().queue(users -> {
                        for (User user : users) {
                            if (user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())) {

                                String title = message.getEmbeds().get(0).getTitle();

                                if (title != null) {

                                    Integer lobbyID = parseLobbyID(title);

                                    NewLobbyManager.joinLobby(event.getGuild(), event.getTextChannel(), lobbyID, event.getMember(), event.getReactionEmote());
                                    return;

                                }
                            }
                        }
                    });
                }
            });

        }

    }

    /**
     * Gets called on message reaction remove
     *
     * @param event The event
     */

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {

        Member author = event.getMember();

        if (event.getUser() != null && !event.getUser().isBot() && author != null) {

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if (NewLobbyManager.lobbyMessagesInServer.containsKey(event.getGuild().getId()) && NewLobbyManager.lobbyMessagesInServer.get(event.getGuild().getId()).containsValue(message.getId())) {

                    event.getReaction().retrieveUsers().queue(users -> {
                        for (User user : users) {
                            if (user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())) {

                                String title = message.getEmbeds().get(0).getTitle();

                                if (title != null) {

                                    Integer lobbyID = parseLobbyID(title);
                                    NewLobbyManager.leaveLobby(event.getGuild(), event.getTextChannel(), lobbyID, event.getMember(), event.getReactionEmote());
                                    return;

                                }

                            }
                        }
                    });
                }
            });

        }

    }

    /**
     * continues the lobby creation process
     *
     * @param event the message event
     */
    private void lobbyContinue(MessageReceivedEvent event) {

        Member member = event.getMember();

        String[] args = event.getMessage().getContentRaw().split(" ");
        if (args.length >= 3) {

            boolean reserveServer = true;
            String map = "cp_gullywash_final1";

            if (args[2].equalsIgnoreCase("noserver")) {
                reserveServer = false;
            } else if (args[2].equalsIgnoreCase("etf2l") || args[2].equalsIgnoreCase("ugc")) {
                map = args[3];
            } else {
                event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                return;
            }

            if (!lobbyTypes.contains(args[1])) {
                event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                return;
            }

            String type = "6s";
            int maxPlayers = 12;
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "bball":
                case "basketball": {
                    type = "bball";
                    maxPlayers = 4;
                    break;
                }
                case "ultiduo": {
                    type = "ultiduo";
                    maxPlayers = 4;
                    break;
                }
                case "experimental": {
                    type = "ultiduo";
                    maxPlayers = 1;
                    break;
                }
                case "hl":
                case "highlander":
                case "9": {
                    type = "highlander";
                    maxPlayers = 18;
                    break;
                }
                case "6":
                    break;
                default: {
                    event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                    return;
                }
            }

            NewLobbyManager.createLobby(event.getGuild(), event.getTextChannel(), type, args[2], map, reserveServer, member, maxPlayers);

        } else {

            event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

        }

    }

    /**
     * Gets lobby from essage
     * @param messageContent content of received message
     * @return the lobby id
     */
    private Integer parseLobbyID(String messageContent) {
        String stringID = Character.toString(messageContent.charAt((messageContent.indexOf("#") + 1)));
        return Integer.parseInt(stringID);
    }

}
