package com.kingpixel.cobbledaycare.database;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBClient extends DatabaseClient {

  private MongoClient mongoClient;
  private MongoDatabase database;
  private MongoCollection<Document> collection;

  public MongoDBClient() {
  }

  @Override
  public void connect(DataBaseConfig config) {
    var settings = MongoClientSettings.builder()
      .applicationName("CobbleDaycare")
      .applyConnectionString(new ConnectionString(config.getUrl()))
      .build();
    mongoClient = MongoClients.create(settings);
    database = mongoClient.getDatabase(config.getDatabase());
    collection = database.getCollection("user_information");
  }

  @Override
  public void disconnect() {
    var entries = DatabaseClient.USERS.asMap().entrySet();
    for (var entry : entries) {
      var key = entry.getKey();
      var value = entry.getValue();
      saveOrUpdateUserInformation(
        key,
        value
      );
    }
    if (mongoClient != null) mongoClient.close();
    mongoClient = null;
  }

  @Override
  public UserInformation getUserInformation(ServerPlayerEntity player) {
    UUID uuid = player.getUuid();
    UserInformation userInformation = DatabaseClient.USERS.getIfPresent(uuid);
    if (userInformation != null) return userInformation;
    Document document = collection.find(Filters.eq("playerUUID", uuid.toString())).first();
    if (document != null) {
      // Load user information from the document
      userInformation = UserInformation.fromDocument(document);
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID,
        "User information loaded from MongoDB: " + userInformation.getPlayerName() + " (" + userInformation.getPlayerUUID() + ")");
      DatabaseClient.USERS.put(uuid, userInformation);
      return userInformation;
    } else {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "No user information found for player " + player.getGameProfile().getName() + ", creating new entry.");
      userInformation = new UserInformation(player);
      saveOrUpdateUserInformation(player, userInformation);
      return userInformation;
    }
  }

  @Override
  public void saveOrUpdateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    saveOrUpdateUserInformation(player.getUuid(), userInformation);
  }

  private void saveOrUpdateUserInformation(UUID playerUUID, UserInformation userInformation) {
    Bson filter = eq("playerUUID", playerUUID.toString());
    Document document = userInformation.toDocument();
    collection.replaceOne(filter, document, new ReplaceOptions().upsert(true));
  }


}