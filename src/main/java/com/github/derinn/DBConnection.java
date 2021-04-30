package com.github.derinn;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

class DBConnection {

    private final static Logger logger = LoggerFactory.getLogger(DBConnection.class);

    private static MongoDatabase database;
    private static String dbUsername, dbPassword, dbDatabaseName;
    private static int connectionEstablishmentTries;

    /**
     * Connect to the mongodb database
     *
     * @param databaseName the name of the database
     * @param username     username for authentication
     * @param password     password for authentication
     */
    static void establishConnection(String databaseName, String username, String password) {

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

    /**
     * Writes to database collection
     * @param collectionName the collection Name
     * @param inputObject the document to write
     */
    static void writeToDatabase(String collectionName, Document inputObject) {

        if (database == null) {

            if (connectionEstablishmentTries > 10) {

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            writeToDatabase(collectionName, inputObject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertOne(inputObject);

    }

    /**
     * Write to database safely?
     * @param collectionName the collection
     * @param findObject the document to find
     * @param inputDoc the input doc
     */
    static void safeWriteToDatabase(String collectionName, Document findObject, Document inputDoc) {

        FindIterable<Document> existingDocs = DBConnection.findFromDatabase(collectionName, findObject);

        if (existingDocs != null && existingDocs.first() != null) {

            Document existingDoc = existingDocs.first();
            DBConnection.replaceInDatabase(collectionName, existingDoc, inputDoc);

        } else {

            DBConnection.writeToDatabase(collectionName, inputDoc);

        }

    }

    /**
     * Find document from database
     * @param collectionName the collection Name
     * @param searchObject the search object
     * @return The objects it found
     */
    static FindIterable<Document> findFromDatabase(String collectionName, Document searchObject) {

        if (database == null) {

            if (connectionEstablishmentTries > 10) {

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            return null;

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(searchObject);

    }

    /**
     * Delete a document from the database
     * @param collectionName the collection of the object
     * @param deleteObject the object to delete
     */
    static void deleteFromDatabase(String collectionName, Document deleteObject) {

        if (database == null) {

            if (connectionEstablishmentTries > 10) {

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            deleteFromDatabase(collectionName, deleteObject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(deleteObject);

    }

    /**
     * Replace something in the database
     * @param collectionName the name of the collection
     * @param initialObject the initial object
     * @param replaceObject the object to replace  it with
     */
    static void replaceInDatabase(String collectionName, Document initialObject, Document replaceObject) {

        if (database == null) {

            if (connectionEstablishmentTries > 10) {

                logger.error("Database is null, quitting");
                System.exit(1);

            }

            establishAgain();
            replaceInDatabase(collectionName, initialObject, replaceObject);

        }

        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.replaceOne(initialObject, replaceObject);

    }

    private static void establishAgain() {

        establishConnection(dbUsername, dbPassword, dbDatabaseName);
        connectionEstablishmentTries++;

    }

}