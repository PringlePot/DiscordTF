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
import java.util.concurrent.TimeUnit;

public class NewMixer extends ListenerAdapter{

    private ArrayList<String> lobbyTypes = new ArrayList<>(Arrays.asList("experimental", "bball", "basketball", "ultiduo", "6", "9", "hl", "highlander"));

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){

        Member member = event.getMember();
        if(!event.getAuthor().isBot() && member != null){

            if(event.getMessage().getContentRaw().startsWith("-newlobby")){

                Document findByID = new Document();
                findByID.put("guildid", event.getGuild().getId());

                FindIterable<Document> foundDocs = DBConnection.findFromDatabase("hosterstatuslist", findByID);
                boolean isHosterEnabled = true;

                if(foundDocs != null && foundDocs.first() != null){

                    Document foundDoc = foundDocs.first();
                    if(foundDoc != null){
                        isHosterEnabled = foundDoc.getBoolean("enabled");
                    }

                }

                if(!isHosterEnabled){
                    lobbyContinue(event);
                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                    return;
                }else if(member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)){
                    lobbyContinue(event);
                    event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                    return;
                }

                List<Role> hostRole = event.getGuild().getRolesByName("Hoster", true);
                for(Role role : hostRole){

                    if(member.getRoles().contains(role)){

                        lobbyContinue(event);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;

                    }

                }

            }else if(event.getMessage().getContentRaw().startsWith("-endlobby")){

                String[] args = event.getMessage().getContentRaw().split(" ");

                if(args.length == 2 && (args[1].equalsIgnoreCase("0") || args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("2"))){

                    Document findByID = new Document();
                    findByID.put("guildid", event.getGuild().getId());

                    FindIterable<Document> foundDocs = DBConnection.findFromDatabase("hosterstatuslist", findByID);
                    boolean isHosterEnabled = true;

                    if(foundDocs != null && foundDocs.first() != null){

                        Document foundDoc = foundDocs.first();
                        if(foundDoc != null){
                            isHosterEnabled = foundDoc.getBoolean("enabled");
                        }

                    }


                    if(!isHosterEnabled){
                        NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;
                    }else if(member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)){
                        NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                        event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                        return;
                    }

                    List<Role> hostRole = event.getGuild().getRolesByName("Hoster", true);
                    for(Role role : hostRole){

                        if(member.getRoles().contains(role)){
                            NewLobbyManager.endLobby(event.getGuild(), event.getTextChannel(), Integer.parseInt(args[1]), true);
                            event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
                            return;
                        }

                    }

                }else{

                    event.getTextChannel().sendMessage("-endlobby (lobby id)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            }else if(event.getMessage().getContentRaw().startsWith("-givehoster")){

                if(member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)){

                    if(event.getMessage().getMentionedMembers().size() != 0){

                        List<Role> hosterRole = event.getGuild().getRolesByName("Hoster", true);
                        StringBuilder mentionedPeople = new StringBuilder();
                        if(hosterRole.isEmpty()){
                            event.getGuild().createRole().setName("Hoster").setColor(Color.decode("#7EBDC2")).queue(role -> {
                                for(Member mentionedMember : event.getMessage().getMentionedMembers()){
                                    event.getGuild().addRoleToMember(mentionedMember, role).queue();
                                    mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                                }
                            });
                        }else{
                            for(Member mentionedMember : event.getMessage().getMentionedMembers()){
                                event.getGuild().addRoleToMember(mentionedMember, hosterRole.get(0)).queue();
                                mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                            }
                        }

                        event.getTextChannel().sendMessage("Gave Hoster role to " + mentionedPeople.toString()).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            }else if(event.getMessage().getContentRaw().startsWith("-takehoster")){

                if(member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)){

                    if(event.getMessage().getMentionedMembers().size() != 0){

                        List<Role> hosterRole = event.getGuild().getRolesByName("Hoster", true);
                        StringBuilder mentionedPeople = new StringBuilder();
                        if(hosterRole.isEmpty()){
                            event.getGuild().createRole().setName("Hoster").setColor(Color.decode("#7EBDC2")).queue();
                        }else{
                            for(Member mentionedMember : event.getMessage().getMentionedMembers()){
                                event.getGuild().removeRoleFromMember(mentionedMember, hosterRole.get(0)).queue();
                                mentionedPeople.append(mentionedMember.getEffectiveName()).append(" ");
                            }
                        }


                        event.getTextChannel().sendMessage("Removed Hoster role from " + mentionedPeople.toString()).queue(message -> message.delete().queueAfter(30, TimeUnit.SECONDS));

                    }

                }

                event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);

            }

        }

    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event){

        Member author = event.getMember();

        if(event.getUser() != null && !event.getUser().isBot() && author != null){

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if(NewLobbyManager.lobbyMessagesInServer.containsKey(event.getGuild().getId()) && NewLobbyManager.lobbyMessagesInServer.get(event.getGuild().getId()).containsValue(message.getId())){

                    event.getReaction().retrieveUsers().queue(users -> {
                        for(User user : users){
                            if(user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())){

                                String title = message.getEmbeds().get(0).getTitle();

                                if(title != null){

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

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event){

        Member author = event.getMember();

        if(event.getUser() != null && !event.getUser().isBot() && author != null){

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if(NewLobbyManager.lobbyMessagesInServer.containsKey(event.getGuild().getId()) && NewLobbyManager.lobbyMessagesInServer.get(event.getGuild().getId()).containsValue(message.getId())){

                    event.getReaction().retrieveUsers().queue(users -> {
                        for(User user : users){
                            if(user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())){

                                String title = message.getEmbeds().get(0).getTitle();

                                if(title != null){

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

    private void lobbyContinue(MessageReceivedEvent event){

        Member member = event.getMember();

        String[] args = event.getMessage().getContentRaw().split(" ");
        //-newlobby 12 no/etf2l
        if(args.length >= 3){

            boolean reserveServer = true;
            String map = "cp_gullywash_final1";

            if(args[2].equalsIgnoreCase("noserver")){
                reserveServer = false;
            }else if(args[2].equalsIgnoreCase("etf2l") || args[2].equalsIgnoreCase("ugc")){
                map = args[3];
            }else{
                event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                return;
            }

            if(!lobbyTypes.contains(args[1])){
                event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                return;
            }

            String type = "6s";
            int maxPlayers = 12;

            if(args[1].equalsIgnoreCase("bball") || args[1].equalsIgnoreCase("basketball")){
                type = "bball";
                maxPlayers = 4;
            }else if(args[1].equalsIgnoreCase("ultiduo")){
                type = "ultiduo";
                maxPlayers = 4;
            }else if(args[1].equalsIgnoreCase("experimental")){
                type = "ultiduo";
                maxPlayers = 1;
            }else if(args[1].equalsIgnoreCase("hl") || args[1].equalsIgnoreCase("highlander") || args[1].equalsIgnoreCase("9")){
                type = "highlander";
                maxPlayers = 18;
            }else if(!args[1].equalsIgnoreCase("6")){
                event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));
                return;
            }

            NewLobbyManager.createLobby(event.getGuild(), event.getTextChannel(), type, args[2], map, reserveServer, member, maxPlayers);

        }else{

            event.getTextChannel().sendMessage("-newlobby (bball/ultiduo/6/9) (noserver/etf2l/ugc) (complete map name)").queue(botMessage -> botMessage.delete().queueAfter(30, TimeUnit.SECONDS));

        }

    }

    private Integer parseLobbyID(String messageContent){
        String stringID = Character.toString(messageContent.charAt((messageContent.indexOf("#") + 1)));
        return Integer.parseInt(stringID);
    }

}
