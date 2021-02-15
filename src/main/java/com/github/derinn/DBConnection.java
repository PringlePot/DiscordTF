package com.github.derinn;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

class DBConnection{

    private final static Logger logger = LoggerFactory.getLogger(DBConnection.class);

    private static MongoDatabase database;
    private static String dbUsername, dbPassword, dbDatabaseName;
    private static int connectionEstablishmentTries;

    /**
     * Connect to the database
     */
    static void establishConnection(String databaseName, String username, String password){

        MongoCredential credential = MongoCredential.createCredential(username, databaseName, password.toCharArray());

        MongoClient mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Collections.singletonList(new ServerAddress("localhost", 27017))))
                        .credential(credential)
                        .build());

        database = mongoClient.getDatabase(databaseName);
        System.out.println("connected to database");
        dbUsername = username;
        dbPassword = password;
        dbDatabaseName = databaseName;

    }

    static void writeToDatabase(String collectionName, Document inputOject){

        if(database == null){

            if(connectionEstablishmentTries > 10){

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            writeToDatabase(collectionName, inputOject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertOne(inputOject);

    }

    static void safeWriteToDatabase(String collectionName, Document findObject, Document inputDoc){

        FindIterable<Document> existingDocs = DBConnection.findFromDatabase(collectionName, findObject);

        if(existingDocs != null && existingDocs.first() != null){

            Document existingDoc = existingDocs.first();
            DBConnection.replaceInDatabase(collectionName, existingDoc, inputDoc);

        }else{

            DBConnection.writeToDatabase(collectionName, inputDoc);

        }

    }

    static FindIterable<Document> findFromDatabase(String collectionName, Document searchObject){

        if(database == null){

            if(connectionEstablishmentTries > 10){

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            return null;

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(searchObject);

    }

    static void deleteFromDatabase(String collectionName, Document deleteObject){

        if(database == null){

            if(connectionEstablishmentTries > 10){

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            deleteFromDatabase(collectionName, deleteObject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(deleteObject);

    }

    static void replaceInDatabase(String collectionName, Document initialObject, Document replaceObject){

        if(database == null){

            if(connectionEstablishmentTries > 10){

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            replaceInDatabase(collectionName, initialObject, replaceObject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.replaceOne(initialObject, replaceObject);

    }

    private static void establishAgain(){

        establishConnection(dbUsername, dbPassword, dbDatabaseName);
        connectionEstablishmentTries++;

    }

}