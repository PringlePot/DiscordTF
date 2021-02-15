package archive;

import com.mongodb.client.FindIterable;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

class Mixer extends ListenerAdapter{

    private static final ArrayList<String> hlClassList = new ArrayList<>(Arrays.asList("scout", "soldier", "pyro", "demoman", "heavy", "engineer", "medic", "sniper", "spy"));

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){

        //newmix
        //leavemix
        //joinmix
        //kickmix

        //display lobbies per server
        //join lobby by id
        //classes
        //maps

        if(!event.getAuthor().isBot()){

            Member author = event.getMember();

            if(author != null){

                if(event.getMessage().getContentRaw().startsWith("-setlobbychannel")){

                    if(author.isOwner()){

                        LobbyManager.setLobbyChannel(event.getGuild(), event.getTextChannel());
                        event.getTextChannel().sendMessage("setting this channel as the main lobby channel").queue();

                    }

                }else if(event.getMessage().getContentRaw().startsWith("-newlobby")){

                    String[] messageArgs = event.getMessage().getContentRaw().split(" ");
                    if(messageArgs.length == 2){

                        int maxPlayers = 12;
                        String type = "6s";

                        if(messageArgs[1].equalsIgnoreCase("bball") || messageArgs[1].equalsIgnoreCase("basketball")){

                            maxPlayers = 4;
                            type = "basketball";

                        }else if(messageArgs[1].equalsIgnoreCase("ultiduo")){

                            maxPlayers = 4;
                            type = "ultiduo";

                        }else if(messageArgs[1].equalsIgnoreCase("hl") || messageArgs[1].equalsIgnoreCase("highlander")){

                            maxPlayers = 18;
                            type = "highlander";

                        }

                        if(!LobbyManager.lobbiesInServer.containsKey(event.getGuild().getId()) || LobbyManager.lobbiesInServer.containsKey(event.getGuild().getId()) && LobbyManager.lobbiesInServer.get(event.getGuild().getId()).size() < 3){

                            LobbyManager.createNewLobby(event.getGuild(), event.getTextChannel(), author.getId(), maxPlayers, type);

                        }else{

                            event.getTextChannel().sendMessage("there are 3 lobbies already").queue();

                        }

                    }else{

                        event.getTextChannel().sendMessage("-newlobby (ultiduo/bball/6/9)").queue();

                    }

                }else if(event.getMessage().getContentRaw().startsWith("-endlobby")){

                    String[] messageArgs = event.getMessage().getContentRaw().split(" ");
                    if(messageArgs[1].equalsIgnoreCase("1") || messageArgs[1].equalsIgnoreCase("2") || messageArgs[1].equalsIgnoreCase("3")){

                        if(LobbyManager.lobbiesInServer.containsKey(event.getGuild().getId()) && LobbyManager.lobbiesInServer.get(event.getGuild().getId()).get(Integer.parseInt(messageArgs[1])) != null){

                            LobbyManager.endLobby(event.getGuild(), LobbyManager.lobbyChannel.get(event.getGuild().getId()), Integer.parseInt(messageArgs[1]));
                            event.getTextChannel().sendMessage("lobby #" + messageArgs[1] + " ended").queue();

                        }else{

                            event.getTextChannel().sendMessage("couldn't find the lobby").queue();

                        }

                    }else{

                        event.getTextChannel().sendMessage("couldn't find that lobby").queue();

                    }

                }else if(event.getMessage().getContentRaw().startsWith("-classemotes")){

                    if(author.isOwner()){

                        event.getTextChannel().sendMessage("react to this message to get your class roles").queue(message -> {

                            for(String classEmoteName : hlClassList){

                                for(Emote emote : event.getGuild().getEmotes()){

                                    if(emote.getName().equalsIgnoreCase( "red" + classEmoteName)){

                                       message.addReaction(emote).queue();

                                    }

                                }

                            }

                            Document saveClassMessage = new Document();
                            saveClassMessage.put("guildid", event.getGuild().getId());

                            FindIterable<Document> foundDocs = DBConnection.findFromDatabase("classmessagelist", saveClassMessage);

                            saveClassMessage.put("messageid", message.getId());

                            if(foundDocs == null){

                                DBConnection.writeToDatabase("classmessagelist", saveClassMessage);

                            }else if(foundDocs.first() == null){

                                DBConnection.writeToDatabase("classmessagelist", saveClassMessage);

                            }else{

                                DBConnection.replaceInDatabase("classmessagelist", foundDocs.first(), saveClassMessage);

                            }

                        });

                    }

                }

            }

        }

    }

    //delete channel if its named red # and its empty

}
