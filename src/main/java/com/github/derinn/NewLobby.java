package com.github.derinn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
@AllArgsConstructor
public class NewLobby {

    //number
    private final int id;
    //bball, ultiduo, 6s, hl
    private final String type;
    //league name
    private final String league;
    //map name
    private final String map;
    //reserve server or not
    private final Boolean reserveServer;

    //member, reserve server to his name
    private final Member host;

    //max players
    private final Integer maxPlayers;

    private final ArrayList<Member> redPlayers = new ArrayList<>();

    private final ArrayList<Member> bluPlayers = new ArrayList<>();

    private final ArrayList<Member> combinedPlayers = new ArrayList<>();
    private final ArrayList<Integer> combinedClassAmounts = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0));

    private Role redRole;
    private Role bluRole;


    private TextChannel textChannel = null;

    //wtf is wrong with me
    private final ArrayList<ArrayList<String>> redPlayersOfClasses = new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    //aaaand i did it twice
    private final ArrayList<ArrayList<String>> bluPlayersOfClasses = new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

    NewLobby(Integer id, String type, String league, String map, Boolean reserveServer, Member host, Integer maxPlayers) {

        this.id = id;
        this.type = type;
        this.league = league;
        this.map = map;
        this.reserveServer = reserveServer;
        this.host = host;
        this.maxPlayers = maxPlayers;

    }

}
