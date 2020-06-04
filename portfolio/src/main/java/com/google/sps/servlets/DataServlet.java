// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery.Builder;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final String defaultMaxComment = "20";

  /*
   * Called when a client submits a GET request to the /data URL
   * Displays all recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int maxCommentDisplay = Integer.parseInt(getFieldFromResponse(request, "maxcomments", defaultMaxComment));
    String sortMetric = getFieldFromResponse(request, "metric", "time");
    String sortOrder = getFieldFromResponse(request, "order", "desc");

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Builder builder = Query.newEntityQueryBuilder();
    builder = builder.setKind("Comment").setLimit(maxCommentDisplay);
    if (sortOrder.equals("desc")) {
      builder = builder.setOrderBy(StructuredQuery.OrderBy.desc(sortMetric));
    } else {
      builder = builder.setOrderBy(StructuredQuery.OrderBy.asc(sortMetric));
    }
    Query<Entity> query = builder.build();
    QueryResults<Entity> results = datastore.run(query);

    ArrayList<UserComment> comments = new ArrayList<>();
    while (results.hasNext()) {
      Entity entity = results.next();
      long id = entity.getKey().getId();
      String name = entity.getString("name");
      String email = entity.getString("email");
      long time =  entity.getLong("time");
      String comment = entity.getString("comment");
      UserComment userComment = UserComment.create(name, email, comment, time);
      comments.add(userComment);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  /*
   * Called when a client submits a POST request to the /data URL
   * Adds submitted comment to internal record if the comment is
   * non-empty. 
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String parsedBody = CharStreams.toString(request.getReader());

    JsonParser parser = new JsonParser();
    JsonElement parsedJson = parser.parse(parsedBody);
    JsonObject jsonObject = parsedJson.getAsJsonObject();

    String userComment = getFieldFromJsonObject(jsonObject, "comment", "");
    if (userComment.length() != 0) {
      String userName = getFieldFromJsonObject(jsonObject, "name", "Anonymous");
      String userEmail = getFieldFromJsonObject(jsonObject, "email", "janedoe@gmail.com");
      String currDate = String.valueOf(System.currentTimeMillis());
      long userDate = Long.parseLong(getFieldFromJsonObject(jsonObject, "timestamp", currDate));
      addToDatastore(userName, userEmail, userDate, userComment);
    }
  }

  /*
   * Extracts the value of fieldName attribute from jsonObject if present
   * and returns defaultValue if it is not or the value is empty
   */
  private String getFieldFromJsonObject(JsonObject jsonObject, String fieldName, String defaultValue) {
    if (jsonObject.has(fieldName)) {
      String fieldValue = jsonObject.get(fieldName).getAsString();
      return (fieldValue.length() == 0) ? defaultValue : fieldValue;
    }
    return defaultValue;
  }

  // Adds a comment with the given metadata to the database  
  private void addToDatastore(String name, String email, long dateTime, String comment) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    IncompleteKey key = keyFactory.setKind("Comment").newKey();
    FullEntity<IncompleteKey> thisComment =
        FullEntity.newBuilder(key)
          .set("name", name)
          .set("email", email)
          .set("time", dateTime)
          .set("comment", comment)
          .build();
    datastore.add(thisComment);
  }

  /*
   * Get value for fieldName for request if present. Return defaultValue if fieldName is not present
   * or the associated value is empty. Raise an exception if fieldName is mapped to multiple values.
   */
  private String getFieldFromResponse(HttpServletRequest request, String fieldName, String defaultValue) {
    String[] defaultArr = {defaultValue};
    String[] fieldValues = request.getParameterMap().getOrDefault(fieldName, defaultArr);
    if (fieldValues.length > 1) {
      throw new IllegalArgumentException("Found multiple values for single key in form");
    } else {
      String userValue = fieldValues[0];
      if (userValue.length() == 0) {
        return defaultValue;
      } else {
        return userValue;
      }
    }
  }
}
