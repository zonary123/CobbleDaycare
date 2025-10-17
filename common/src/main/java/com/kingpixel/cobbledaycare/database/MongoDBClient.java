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

  public MongoDBClient(DataBaseConfig config) {
    connect(config);
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
    if (mongoClient != null) mongoClient.close();
    mongoClient = null;
  }

  @Override
  public UserInformation getUserInformation(ServerPlayerEntity player) {
    UUID uuid = player.getUuid();
    UserInformation userInformation = DatabaseClientFactory.userPlots.get(uuid);
    if (userInformation != null) return userInformation;
    Document document = collection.find(eq("playerUUID", uuid.toString())).first();
    if (document != null) {
      userInformation = UserInformation.fromDocument(document);
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "User information loaded from MongoDB: " + userInformation);
      }
      DatabaseClientFactory.userPlots.put(uuid, userInformation);
      return userInformation;
    } else {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "No user information found for player " + player.getGameProfile().getName() + ", creating new entry.");
      userInformation = new UserInformation(player);
      updateUserInformation(player, userInformation);
      return userInformation;
    }
  }

  @Override
  public void updateUserInformation(ServerPlayerEntity player, UserInformation userInformation) {
    try {
      if (player == null || userInformation == null) return;
      UUID uuid = player.getUuid();
      Bson filter = eq("playerUUID", uuid.toString());
      Document document = userInformation.toDocument();
      collection.replaceOne(filter, document, new ReplaceOptions().upsert(true));
    } catch (Exception e) {
      e.printStackTrace(); // Manejo básico de errores
    }
  }


}