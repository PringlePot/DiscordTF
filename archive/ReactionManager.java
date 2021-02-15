package archive;

import com.mongodb.client.FindIterable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class ReactionManager extends ListenerAdapter{

    //TODO 1 6s class and 1 9s class

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event){

        Member author = event.getMember();

        if(event.getUser() != null && !event.getUser().isBot() && event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES) && author != null){

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if(LobbyManager.lobbyMessages.containsKey(event.getGuild().getId()) && LobbyManager.lobbyMessages.get(event.getGuild().getId()).contains(message.getId())){

                    event.getReaction().retrieveUsers().queue(users -> {
                        for(User user : users){
                            if(user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())){

                                Integer lobbyID = parseLobbyID(message.getContentRaw());

                                String emoteName = event.getReactionEmote().getName();
                                String teamName = emoteName.substring(0, 3);
                                String className = emoteName.substring(3);
                                LobbyManager.joinLobby(event.getGuild(), event.getTextChannel(), event.getUserId(), lobbyID, teamName, className);
                                return;
                            }
                        }
                    });
                }
            });

            if(LobbyManager.classMessageOfServer.containsKey(event.getGuild().getId())){

                if(event.getMessageId().equalsIgnoreCase(LobbyManager.classMessageOfServer.get(event.getGuild().getId()))){

                    String roleName = event.getReactionEmote().getName().toLowerCase();
                    event.getGuild().addRoleToMember(author, event.getGuild().getRolesByName(roleName, true).get(0)).queue();

                }

            }else{

                Document findDoc = new Document();
                findDoc.put("guildid", event.getGuild().getId());
                FindIterable<Document> foundDocs = DBConnection.findFromDatabase("classmessagelist", findDoc);
                if(foundDocs != null){

                    Document foundFirst = foundDocs.first();

                    if(foundFirst != null && foundFirst.getString("message") != null){

                        if(event.getMessageId().equalsIgnoreCase(foundFirst.getString("messageid"))){

                            String roleName = event.getReactionEmote().getName().toLowerCase();
                            event.getGuild().addRoleToMember(author, event.getGuild().getRolesByName(roleName, true).get(0)).queue();

                        }

                    }

                }

            }

        }

    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event){

        Member author = event.getMember();

        if(event.getUser() != null && !event.getUser().isBot() && event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES) && author != null){

            event.getTextChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if(LobbyManager.lobbyMessages.containsKey(event.getGuild().getId()) && LobbyManager.lobbyMessages.get(event.getGuild().getId()).contains(message.getId())){

                    event.getReaction().retrieveUsers().queue(users -> {
                        for(User user : users){
                            if(user.getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())){

                                Integer lobbyID = parseLobbyID(message.getContentRaw());

                                String emoteName = event.getReactionEmote().getName();
                                String teamName = emoteName.substring(0, 3);
                                String className = emoteName.substring(3);
                                LobbyManager.leaveLobby(event.getGuild(), event.getUserId(), lobbyID, teamName, className, event.getTextChannel());
                                return;
                            }
                        }
                    });
                }
            });

            //check
            if(LobbyManager.classMessageOfServer.containsKey(event.getGuild().getId())){

                if(event.getMessageId().equalsIgnoreCase(LobbyManager.classMessageOfServer.get(event.getGuild().getId()))){

                    String roleName = event.getReactionEmote().getName().toLowerCase();
                    event.getGuild().removeRoleFromMember(author, event.getGuild().getRolesByName(roleName, true).get(0)).queue();

                }

            }else{

                Document findDoc = new Document();
                findDoc.put("guildid", event.getGuild().getId());
                FindIterable<Document> foundDocs = DBConnection.findFromDatabase("classmessagelist", findDoc);
                if(foundDocs != null){

                    Document foundFirst = foundDocs.first();

                    if(foundFirst != null && foundFirst.getString("message") != null){

                        if(event.getMessageId().equalsIgnoreCase(foundFirst.getString("messageid"))){

                            String roleName = event.getReactionEmote().getName().toLowerCase();
                            event.getGuild().removeRoleFromMember(author, event.getGuild().getRolesByName(roleName, true).get(0)).queue();

                        }

                    }

                }

            }

        }

    }

    private Integer parseLobbyID(String messageContent){

        String stringID = StringUtils.substringBetween(messageContent, "#", "#");
        return Integer.parseInt(stringID);
    }

}
