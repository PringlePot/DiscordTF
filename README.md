# discordTF
TF2 oriented Discord bot
uses mongodb, apache httpclient and jda

## Usage
private message the bot first with<br/>
setrcon (your desired rcon password)<br/>
then your desired location for the server<br/>
setflag (de,nl,fr)<br/>
then your serveme api token<br/>
settoken (token)<br/>
  
You can then reserve a serveme server with<br/>
-newserver <etf2l> <ultiduo, bball, 6, 9> <5cp, koth>

## Other Commands and Alike
the bot converts all connects to clickable links<br/>

-matches (etf2l team id)<br/>
returns all upcoming matches for a team

you can also set your team id by messaging the bot with<br/>
setteam (your etf2l team id)

and after that you can simply do -matches in a server and get your upcoming matches

ultiduo and bball don't need specific map type such as 5cp or koth
only etf2l configs for now

Add the bot to your server: 
https://discord.com/api/oauth2/authorize?client_id=701907244861882448&permissions=8&redirect_uri=https%3A%2F%2Fgithub.com%2Fderin-n%2FDiscordTF&response_type=code&scope=bot%20messages.read

### TODO
- [ ] Add rgl configs
- [ ] Add map selection
