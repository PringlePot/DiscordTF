Document findByID = new Document();
        findByID.put("userid", event.getAuthor().getId());
        Document tokenDoc = DBConnection.findFromDatabase("tokenlist", findByID).first();
        Document flagDoc = DBConnection.findFromDatabase("flaglist", findByID).first();
        Document rconDoc = DBConnection.findFromDatabase("rconlist", findByID).first();

        if(tokenDoc == null){

        textChannel.sendMessage("you haven't set your serveme api token").complete();
        textChannel.sendMessage("dm me with \"settoken (token)\"").complete();
        return;

        }

        if(flagDoc == null){

        textChannel.sendMessage("you haven't set your preferred country").complete();
        textChannel.sendMessage("dm me with \"setflag (de,fr,nl)\"").complete();
        return;

        }

        if(rconDoc == null){

        textChannel.sendMessage("you haven't set your rcon password").complete();
        textChannel.sendMessage("dm me with \"setrcon (your rcon password)\"").complete();
        return;

        }

        String[] messageArgs = messageContent.split(" ");

        if(messageArgs.length != 4 || (!leagueArg.contains(messageArgs[1]) || !typeArg.contains(messageArgs[2]) || !mapArg.contains(messageArgs[3]))){

        if(!messageArgs[2].equalsIgnoreCase("ultiduo") && !messageArgs[2].equalsIgnoreCase("bball")){

        textChannel.sendMessage("-newserver (etf2l) (ultiduo/bball/6//9) (5cp/koth)").complete();
        textChannel.sendMessage("no map type required for ultiduo and bball").complete();
        return;

        }

        }

        String servemeToken = tokenDoc.getString("token");
        String servemeFlag = flagDoc.getString("flag");
        String servemeRcon = rconDoc.getString("rcon");

        String configName = "";
        String whitelistName = "";

        //pick config name
        if(messageArgs[1].equalsIgnoreCase("etf2l")){

        if(messageArgs[2].equalsIgnoreCase("ultiduo")){

        whitelistName = "etf2l_whitelist_ultiduo.txt";
        configName = "etf2l_ultiduo";

        }else if(messageArgs[2].equalsIgnoreCase("bball")){

        whitelistName = "etf2l_whitelist_bball.txt";
        configName = "etf2l_bball";

        }else if(messageArgs[2].equalsIgnoreCase("6")){

        whitelistName = "etf2l_whitelist_6v6.txt";

        if(messageArgs[3].equalsIgnoreCase("5cp")){

        configName = "etf2l_6v6_5cp";

        }else if(messageArgs[3].equalsIgnoreCase("koth")){

        configName = "etf2l_6v6_koth";

        }

        }else if(messageArgs[2].equalsIgnoreCase("9")){

        whitelistName = "etf2l_whitelist_9v9.txt";

        if(messageArgs[3].equalsIgnoreCase("5cp")){

        configName = "etf2l_9v9_5cp";

        }else if(messageArgs[3].equalsIgnoreCase("koth")){

        configName = "etf2l_9v9_koth";

        }

        }

        }


        try{

        String urlString = "https://serveme.tf/api/reservations/new?api_key=" + servemeToken;

        //set up the httpclient
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //get request
        HttpGet httpget = new HttpGet(urlString);
        //execute get, save the response
        HttpResponse httpresponse = httpclient.execute(httpget);
        //read the response
        Scanner scanner = new Scanner(httpresponse.getEntity().getContent());

        //response as json
        String newReservationJsonString;
        //define builder
        StringBuilder builder = new StringBuilder();

        //while there is still more response to be read
        while(scanner.hasNext()){
        //add to the string builder
        builder.append(scanner.nextLine());
        }

        //build the response into a json string
        newReservationJsonString = builder.toString();
        //create json reader and read the response
        JsonReader reader = Json.createReader(new StringReader(newReservationJsonString));
        //convert json string into object
        JsonObject reservationObject = reader.readObject();
        //close the reader
        reader.close();
        //make a reservation json object as string
        String reservationJson = "{\"reservation\":" + reservationObject.get("reservation") + "}";

        urlString = "https://serveme.tf/api/reservations/find_servers?api_key=" + servemeToken;

        //json to be sent as body of post request
        StringEntity entity = new StringEntity(reservationJson);

        //post request
        HttpPost httpPost = new HttpPost(urlString);
        //accept json as response
        httpPost.setHeader("Accept", "application/json");
        //body is json
        httpPost.setHeader("Content-Type", "application/json");
        //set body as reservation json
        httpPost.setEntity(entity);
        //execute post request and get the response
        CloseableHttpResponse response = httpclient.execute(httpPost);

        //check if good
        if(response.getStatusLine().getStatusCode() == 200){

        //try reading the response
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"))){
        //define builder
        StringBuilder responseBuilder = new StringBuilder();
        //response as json string
        String responseLine;

        //append to builder
        while((responseLine = bufferedReader.readLine()) != null){
        responseBuilder.append(responseLine.trim());
        }

        bufferedReader.close();

        //builder to string
        String serverJsonString = responseBuilder.toString();

        //close the response
        response.close();

        //read repsonse with server list
        reader = Json.createReader(new StringReader(serverJsonString));
        //save it to json object
        JsonObject jsonWithLists = reader.readObject();
        //close reader
        reader.close();

        //get list of servers
        JsonArray serverList = jsonWithLists.getJsonArray("servers");
        //server object
        JsonObject serverObj;

        //check if there are servers available
        if(serverList.size() > 0){

        //loop through server list
        for(int i = 0; i < serverList.size(); i++){

        //set server object
        serverObj = serverList.getJsonObject(i);

        //check if server matches flag, if couldnt find flag, pick the last one
        if(serverObj.getString("flag").equalsIgnoreCase(servemeFlag) || (!serverObj.getString("flag").equalsIgnoreCase(servemeFlag) && i == (serverList.size() - 1))){

        //choose server id
        int serverID = serverObj.getInt("id");

        //get list of configs
        JsonArray configList = jsonWithLists.getJsonArray("server_configs");
        //config object
        JsonObject configObj;

        //loop through configs
        for(i = 0; i < configList.size(); i++){

        //set config object
        configObj = configList.getJsonObject(i);

        //if config contains type of league / etf2l, rgl, classic
        if(configObj.getString("file").equalsIgnoreCase(configName)){

        //choose config id
        int configID = configObj.getInt("id");

        //get whitelist list
        JsonArray whitelistList = jsonWithLists.getJsonArray("whitelists");
        //whitelist object
        JsonObject whitelistObj;

        //loop through whitelists
        for(i = 0; i < whitelistList.size(); i++){

        //set whitelist object
        whitelistObj = whitelistList.getJsonObject(i);

        //if whitelist contains type of league / etf2l, rgl, classic
        if(whitelistObj.getString("file").contains(messageArgs[1])){

        //if whitelist equals whitelist name
        if(whitelistObj.getString("file").equalsIgnoreCase(whitelistName)){

        //choose whitelist id
        int whitelistID = whitelistObj.getInt("id");

        String joinPassword = generatePassword();

        //build json body to post
        JsonObjectBuilder reservationBuilder = Json.createObjectBuilder();
        reservationBuilder.add("starts_at", reservationObject.getJsonObject("reservation").getString("starts_at"));
        reservationBuilder.add("ends_at", reservationObject.getJsonObject("reservation").getString("ends_at"));
        reservationBuilder.add("rcon", servemeRcon);
        reservationBuilder.add("password", joinPassword);
        reservationBuilder.add("tv_password", joinPassword);
        reservationBuilder.add("whitelist_id", whitelistID);
        reservationBuilder.add("server_id", serverID);
        reservationBuilder.add("server_config_id", configID);

        urlString = "https://serveme.tf/api/reservations?api_key=" + servemeToken;

        //body json to string
        String serverJsonStr = "{\"reservation\":" + reservationBuilder.build().toString() + "}";

        //set post body
        entity = new StringEntity(serverJsonStr);

        //post request
        httpPost = new HttpPost(urlString);
        //accept json response
        httpPost.setHeader("Accept", "application/json");
        //post body is json
        httpPost.setHeader("Content-Type", "application/json");
        //set body
        httpPost.setEntity(entity);
        //execute post
        response = httpclient.execute(httpPost);

        try{

        //if successful
        if(response.getStatusLine().getStatusCode() == 200){

        String responseJsonString;

        //read response
        try(BufferedReader bufferedResultReader = new BufferedReader(new InputStreamReader(response.getEntity().
        getContent(), "utf-8"))){

        //response as json string
        responseBuilder = new StringBuilder();
        while((responseLine = bufferedResultReader.readLine()) != null){
        responseBuilder.append(responseLine.trim());
        }
        bufferedResultReader.close();

        responseJsonString = responseBuilder.toString();

        }catch(Exception ex){
        textChannel.sendMessage("reserved server but couldn't get information about it").complete();
        return;
        }

        //close response
        //response.close();
        reader = Json.createReader(new StringReader(responseJsonString));
        JsonObject serverObject = reader.readObject();

        //System.out.println(responseJsonString);

        String ip = serverObject.getJsonObject("reservation").getJsonObject("server").getString("ip");
        String port = serverObject.getJsonObject("reservation").getJsonObject("server").getString("port");
        String stvPort = Integer.toString(Integer.parseInt(port) + 5);

        String password = serverObject.getJsonObject("reservation").getString("password");

        textChannel.sendMessage("```connect```").complete();
        textChannel.sendMessage("steam://connect/" + ip + ":" + port + "/" + password).complete();
        textChannel.sendMessage("```sourcetv connect```").complete();
        textChannel.sendMessage("steam://connect/" + ip + ":" + stvPort + "/" + password).complete();

        //textChannel.sendMessage("reserved a server").complete();
        //System.out.println(responseBuilder.toString());
        return;

        }else{

        //System.out.println(response.getStatusLine());
        //close response
        response.close();
        textChannel.sendMessage("couldn't reserve a server").complete();
        return;

        }

        }finally{
        response.close();
        }

        }

        }

        }

        }

        }

        }

        }

        }else{

        textChannel.sendMessage("there are no servers available for you").complete();

        }

        }catch(IOException e){
        e.printStackTrace();
        }

        }else{

        //System.out.println(response.getStatusLine());
        textChannel.sendMessage("couldn't reserve a server").complete();

        }

        }catch(IOException e){
        e.printStackTrace();
        }
