package archive;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.ArrayList;

class Lobby{

    private Guild guild;
    private Integer lobbyID;
    private Integer maxPlayers;
    private String ownerID;
    private Integer players;
    private Boolean reserveServer;
    private String type;

    //scoutAmount, soldierAmount, pyroAmount, demomanAmount, heavyAmount, engineerAmount, medicAmount, sniperAmount, spyAmount;
    private ArrayList<Integer> redClassAmounts = new ArrayList<>();
    private ArrayList<Integer> bluClassAmounts = new ArrayList<>();

    private ArrayList<Integer> totalClassAmounts = new ArrayList<>();

    //player ids
    private ArrayList<String> redPlayers = new ArrayList<>();
    private ArrayList<String> bluPlayers = new ArrayList<>();
    private ArrayList<String> totalPlayers = new ArrayList<>();

    private String lobbyMessage;

    private Role redRole;
    private Role bluRole;
    private Role lobbyRole;

    private VoiceChannel redChannel;
    private VoiceChannel bluChannel;

    Lobby(Guild guild, Integer id, Integer maxPlayers, String ownerID, Boolean reserveServer, String type){

        this.guild = guild;
        this.lobbyID = id;
        this.maxPlayers = maxPlayers;
        this.ownerID = ownerID;
        this.players = 1;
        this.reserveServer = reserveServer;
        this.type = type;

    }

    Guild getGuild(){
        return guild;
    }

    Integer getLobbyID(){
        return lobbyID;
    }

    Integer getMaxPlayers(){
        return maxPlayers;
    }

    Integer getPlayers(){
        return players;
    }

    void setPlayers(Integer players){
        this.players = players;
    }

    String getOwnerID(){
        return ownerID;
    }


    ArrayList<Integer> getRedClassAmounts(){
        return redClassAmounts;
    }

    ArrayList<Integer> getBluClassAmounts(){
        return bluClassAmounts;
    }

    ArrayList<Integer> getTotalClassAmounts(){
        return totalClassAmounts;
    }

    void setTotalClassAmounts(ArrayList<Integer> totalClassAmounts){
        this.totalClassAmounts = totalClassAmounts;
    }

    void setRedClassAmounts(ArrayList<Integer> redClassAmounts){
        this.redClassAmounts = redClassAmounts;
    }

    void setBluClassAmounts(ArrayList<Integer> bluClassAmounts){
        this.bluClassAmounts = bluClassAmounts;
    }

    ArrayList<String> getRedPlayers(){
        return redPlayers;
    }


    void setRedPlayers(ArrayList<String> redPlayers){
        this.redPlayers = redPlayers;
    }

    ArrayList<String> getBluPlayers(){
        return bluPlayers;
    }

    void setBluPlayers(ArrayList<String> bluPlayers){
        this.bluPlayers = bluPlayers;
    }

    Boolean getReserveServer(){
        return reserveServer;
    }

    String getLobbyMessage(){
        return lobbyMessage;
    }

    void setLobbyMessage(String lobbyMessage){
        this.lobbyMessage = lobbyMessage;
    }

    Role getBluRole(){
        return bluRole;
    }

    void setBluRole(Role bluRole){
        this.bluRole = bluRole;
    }

    Role getRedRole(){
        return redRole;
    }

    void setRedRole(Role redRole){
        this.redRole = redRole;
    }

    VoiceChannel getRedChannel(){
        return redChannel;
    }

    VoiceChannel getBluChannel(){
        return bluChannel;
    }

    void setRedChannel(VoiceChannel redChannel){
        this.redChannel = redChannel;
    }

    void setBluChannel(VoiceChannel bluChannel){
        this.bluChannel = bluChannel;
    }

    Role getLobbyRole(){
        return lobbyRole;
    }

    void setLobbyRole(Role lobbyRole){
        this.lobbyRole = lobbyRole;
    }

    ArrayList<String> getTotalPlayers(){
        return totalPlayers;
    }

    public String getType(){
        return type;
    }

}
