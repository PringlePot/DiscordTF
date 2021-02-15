package com.github.derinn;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

class NewLobby{

    //number
    private int id;
    //bball, ultiduo, 6s, hl
    private String type;
    //league name
    private String league;
    //map name
    private String map;
    //reserve server or not
    private Boolean reserveServer;

    //member, reserve server to his name
    private Member host;

    //max players
    private Integer maxPlayers;

    private ArrayList<Member> redPlayers = new ArrayList<>();

    private ArrayList<Member> bluPlayers = new ArrayList<>();

    private ArrayList<Member> combinedPlayers = new ArrayList<>();
    private ArrayList<Integer> combinedClassAmounts = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0));

    private Role redRole;
    private Role bluRole;

    private TextChannel textChannel = null;

    //wtf is wrong with me
    private ArrayList<ArrayList<String>> redPlayersOfClasses = new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    //aaaand i did it twice
    private ArrayList<ArrayList<String>> bluPlayersOfClasses = new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

    NewLobby(Integer id, String type, String league, String map, Boolean reserveServer, Member host, Integer maxPlayers){

        this.id = id;
        this.type = type;
        this.league = league;
        this.map = map;
        this.reserveServer = reserveServer;
        this.host = host;
        this.maxPlayers = maxPlayers;

    }

    int getId(){
        return id;
    }

    String getType(){
        return type;
    }

    String getMap(){
        return map;
    }

    Boolean getReserveServer(){
        return reserveServer;
    }

    ArrayList<Member> getRedPlayers(){
        return redPlayers;
    }

    ArrayList<Member> getBluPlayers(){
        return bluPlayers;
    }

    ArrayList<Member> getCombinedPlayers(){
        return combinedPlayers;
    }

    ArrayList<Integer> getCombinedClassAmounts(){
        return combinedClassAmounts;
    }

    ArrayList<ArrayList<String>> getBluPlayersOfClasses(){
        return bluPlayersOfClasses;
    }

    ArrayList<ArrayList<String>> getRedPlayersOfClasses(){
        return redPlayersOfClasses;
    }

    String getLeague(){
        return league;
    }

    Integer getMaxPlayers(){
        return maxPlayers;
    }

    Member getHost(){
        return host;
    }

    Role getRedRole(){
        return redRole;
    }

    Role getBluRole(){
        return bluRole;
    }

    TextChannel getTextChannel(){
        return textChannel;
    }

    void setBluRole(Role bluRole){
        this.bluRole = bluRole;
    }

    void setRedRole(Role redRole){
        this.redRole = redRole;
    }

    void setTextChannel(TextChannel textChannel){
        this.textChannel = textChannel;
    }
}
