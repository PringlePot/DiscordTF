package archive;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ArchivedMixer extends ListenerAdapter{

    //mix key is server id, value blu and red player numbers
    private HashMap<String, Integer> mixHash = new HashMap<>();
    //key server id, value message id
    private HashMap<String, String> mixMessage = new HashMap<>();
    //key server id, value if mix started or not
    private HashMap<String, Boolean> mixStarted = new HashMap<>();
    //key server id, value person who started mix id
    private HashMap<String, String> whoStartedMix = new HashMap<>();

    //teams
    private HashMap<String, String> redPlayers = new HashMap<>();
    private HashMap<String, String> bluPlayers = new HashMap<>();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){

        Member author = event.getMember();

        if(author != null && !event.getAuthor().isBot()){

            if(event.getTextChannel().getName().contains("mix")){

                String message = event.getMessage().getContentRaw();

                if(message.startsWith("-")){

                    if(!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES) && !event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)){
                        event.getTextChannel().sendMessage("do not have permission to manage roles or messages, can't set up a mix").complete();
                        return;
                    }

                    if(event.getGuild().getRolesByName("red", true).isEmpty() || event.getGuild().getRolesByName("blu", true).isEmpty()){
                        event.getTextChannel().sendMessage("red and blu roles do not exist, can't set up a mix").complete();
                        return;
                    }

                }

                if(message.startsWith("-newmix")){

                    if(mixHash.containsKey(event.getGuild().getId())){

                        event.getTextChannel().sendMessage("there is a mix going on right now, please wait until it's done.").complete();
                        return;

                    }

                    String[] messageArgs = event.getMessage().getContentRaw().split(" ");

                    int maxPlayerPerTeam = 12;

                    if(messageArgs.length == 2 && messageArgs[1].equalsIgnoreCase("9")){

                        maxPlayerPerTeam = 18;

                    }

                    Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                    Role bluRole = event.getGuild().getRolesByName("blu", true).get(0);

                    if(author.getRoles().contains(redRole) || author.getRoles().contains(bluRole)){

                        event.getTextChannel().sendMessage("you already have a team, do -endmix first or wait for the mix to end.").complete();
                        return;

                    }

                    mixHash.put(event.getGuild().getId(), 1);
                    mixMessage.remove(event.getGuild().getId());
                    mixStarted.remove(event.getGuild().getId());
                    whoStartedMix.put(event.getGuild().getId(), author.getId());

                    event.getGuild().addRoleToMember(author, redRole).complete();

                    Message sentMessage = event.getTextChannel().sendMessage("```Teams:\n" +
                            "Red: " + author.getEffectiveName() + "\n" +
                            "Blu:```").complete();
                    mixMessage.put(event.getGuild().getId(), sentMessage.getId());

                }else if(message.startsWith("-endmix")){

                    Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                    Role bluRole = event.getGuild().getRolesByName("blu", true).get(0);

                    mixHash.remove(event.getGuild().getId());
                    mixMessage.remove(event.getGuild().getId());
                    mixStarted.remove(event.getGuild().getId());
                    whoStartedMix.remove(event.getGuild().getId());

                    for(Member member : event.getGuild().getMembersWithRoles(redRole)){

                        event.getGuild().removeRoleFromMember(member, redRole).complete();

                    }

                    for(Member member : event.getGuild().getMembersWithRoles(bluRole)){

                        event.getGuild().removeRoleFromMember(member, bluRole).complete();

                    }

                    event.getTextChannel().sendMessage("mix ended, you can start a new one by doing -newmix").complete();

                }else if(message.startsWith("-leavemix")){

                    Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                    Role bluRole = event.getGuild().getRolesByName("blu", true).get(0);

                    if(mixStarted.containsKey(event.getGuild().getId()) && mixStarted.get(event.getGuild().getId())){

                        event.getTextChannel().sendMessage("you can't leave a mix that has started already").complete();
                        return;

                    }

                    for(Role role : author.getRoles()){

                        if(role.equals(redRole)){

                            event.getGuild().removeRoleFromMember(author, redRole).complete();
                            break;

                        }else if(role.equals(bluRole)){

                            event.getGuild().removeRoleFromMember(author, bluRole).complete();
                            break;

                        }

                    }

                    event.getTextChannel().sendMessage("you left the mix").complete();

                }else if(message.startsWith("-joinmix")){

                    String[] messageArgs = message.split(" ");

                    if(messageArgs.length != 2){

                        event.getTextChannel().sendMessage("choose a class").complete();
                        return;

                    }

                    /*if(){



                    }*/

                    Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                    Role bluRole = event.getGuild().getRolesByName("blu", true).get(0);

                    String guildID = event.getGuild().getId();

                    if(mixHash.containsKey(guildID)){

                        for(Role role : author.getRoles()){

                            if(role.equals(redRole)){

                                event.getTextChannel().sendMessage("you are already in the red team").complete();
                                return;

                            }else if(role.equals(bluRole)){

                                event.getTextChannel().sendMessage("you are already in the blu team").complete();
                                return;

                            }

                        }

                        Integer playerNumber = mixHash.get(guildID);
                        if(playerNumber < 12){

                            String redNames = "";
                            String bluNames = "";

                            if(isEven(playerNumber)){

                                event.getGuild().addRoleToMember(author, redRole).complete();
                                if(redPlayers.containsKey(guildID)){

                                    redNames = redPlayers.get(guildID) + ", " + author.getEffectiveName();

                                }else{

                                    redNames = author.getEffectiveName();
                                    redPlayers.put(guildID, redNames);

                                }

                            }else{

                                event.getGuild().addRoleToMember(author, bluRole).complete();
                                if(bluPlayers.containsKey(guildID)){

                                    bluNames = bluPlayers.get(guildID) + ", " + author.getEffectiveName();

                                }else{

                                    bluNames = author.getEffectiveName();
                                    bluPlayers.put(guildID, bluNames);

                                }

                            }

                            if(mixMessage.containsKey(guildID)){

                                event.getChannel().deleteMessageById(mixMessage.get(guildID)).complete();

                            }

                            Message sentMessage = event.getTextChannel().sendMessage("```Teams:\n" +
                                    "Red: " + redNames + "\n" +
                                    "Blu: " + bluNames + "```").complete();

                            mixMessage.put(guildID, sentMessage.getId());
                            playerNumber++;
                            mixHash.put(guildID, playerNumber);

                            if(playerNumber == 12){

                                mixStarted.put(event.getGuild().getId(), true);

                                event.getTextChannel().sendMessage("mix is starting, everyone has to be in the mix voice channel to be moved or you can join your respective voice channel manually").complete();

                                for(VoiceChannel voiceChannel : event.getGuild().getVoiceChannelsByName("mix", true)){

                                    if(voiceChannel.getMembers().size() == playerNumber){

                                        VoiceChannel redChannel = event.getGuild().getVoiceChannelsByName("red", true).get(0);
                                        VoiceChannel bluChannel = event.getGuild().getVoiceChannelsByName("blu", true).get(0);

                                        for(Member moveMember : voiceChannel.getMembers()){

                                            if(moveMember.getRoles().contains(redRole)){

                                                event.getGuild().moveVoiceMember(moveMember, redChannel).complete();

                                            }else if(moveMember.getRoles().contains(bluRole)){

                                                event.getGuild().moveVoiceMember(moveMember, bluChannel).complete();

                                            }

                                        }

                                    }

                                }

                            }

                        }else{

                            event.getTextChannel().sendMessage("there are enough players, you can do -newmix").complete();

                        }

                    }else{

                        event.getTextChannel().sendMessage("new mix, type -playmix to join, if you have set your serveme api token, a server will be reserved to your name").complete();

                    }

                }

            }

        }

    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event){

        if(mixStarted.containsKey(event.getGuild().getId()) && mixStarted.get(event.getGuild().getId())){

            if(mixHash.containsKey(event.getGuild().getId())){

                int playerAmount = mixHash.get(event.getGuild().getId());
                if(event.getChannelJoined().getName().contains("mix") && event.getChannelJoined().getMembers().size() == playerAmount){

                    event.getGuild().getTextChannelsByName("mix", true).get(0).sendMessage("there are enough players in the mix voice channel, moving everyone to their respective channels.").complete();

                    VoiceChannel voiceChannel = event.getChannelJoined();

                    Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                    Role bluRole = event.getGuild().getRolesByName("blu", true).get(0);

                    if(voiceChannel.getMembers().size() == playerAmount){

                        VoiceChannel redChannel = event.getGuild().getVoiceChannelsByName("red", true).get(0);
                        VoiceChannel bluChannel = event.getGuild().getVoiceChannelsByName("blu", true).get(0);

                        for(Member moveMember : voiceChannel.getMembers()){

                            if(moveMember.getRoles().contains(redRole)){

                                event.getGuild().moveVoiceMember(moveMember, redChannel).complete();

                            }else if(moveMember.getRoles().contains(bluRole)){

                                event.getGuild().moveVoiceMember(moveMember, bluChannel).complete();

                            }

                        }

                    }

                }

            }

        }


    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event){

        if(mixStarted.containsKey(event.getGuild().getId()) && mixStarted.get(event.getGuild().getId()) && event.getMember().getId().equalsIgnoreCase(whoStartedMix.get(event.getGuild().getId()))){

            Member member = event.getMember();

            if(member.getOnlineStatus().equals(OnlineStatus.OFFLINE)){

                Role redRole = event.getGuild().getRolesByName("red", true).get(0);
                event.getGuild().removeRoleFromMember(member, redRole).complete();
                whoStartedMix.put(event.getGuild().getId(), event.getGuild().getMembersWithRoles(redRole).get(1).getId());
                event.getGuild().getTextChannelsByName("mix", true).get(0).sendMessage(event.getGuild().getMembersWithRoles(redRole).get(1).getAsMention() + " is now the admin of the mix and can end it by doing -endmix.").complete();

            }

        }

    }

    private boolean isEven(int number){
        return number % 2 == 0;
    }

}
