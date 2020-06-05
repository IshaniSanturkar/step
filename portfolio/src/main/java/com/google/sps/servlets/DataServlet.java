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
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
    String sortMetric = UtilityFunctions.getFieldFromResponse(request, "metric", "time");
    String sortOrder = UtilityFunctions.getFieldFromResponse(request, "order", "desc");

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Builder builder = Query.newEntityQueryBuilder();
    builder = builder.setKind("Comment").setFilter(PropertyFilter.eq("parentid", 0));
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
      UserComment userComment = entityToComment(entity);
      comments.add(userComment);
      Builder childBuilder = Query.newEntityQueryBuilder();
      childBuilder = childBuilder.setKind("Comment").setFilter(PropertyFilter.eq("rootid", id));
      if (sortOrder.equals("desc")) {
        childBuilder = childBuilder.setOrderBy(StructuredQuery.OrderBy.desc(sortMetric));
      } else {
        childBuilder = childBuilder.setOrderBy(StructuredQuery.OrderBy.asc(sortMetric));
      }
      Query<Entity> query = builder.build();
      Iterator<Entity> childResults = datastore.run(query);
      ArrayList<Entity> childList = Lists.newArrayList(childResults);
      comments.addAll(childList);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  private UserComment entityToComment(Entity entity) {
    long id = entity.getKey().getId();
    String name = entity.getString("name");
    String email = entity.getString("email");
    long time =  entity.getLong("time");
    String comment = entity.getString("comment");
    long parentId = entity.getLong("parentid");
    long rootId = entity.getLong("rootid");
    UserComment userComment = UserComment.create(name, email, comment, time, id, parentId, rootId);
    return userComment;
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

    String userComment = UtilityFunctions.getFieldFromJsonObject(jsonObject, "comment", "");
    if (userComment.length() != 0) {
      String userName = UtilityFunctions.getFieldFromJsonObject(jsonObject, "name", "Anonymous");
      String userEmail = UtilityFunctions.getFieldFromJsonObject(jsonObject, "email", "janedoe@gmail.com");
      String currDate = String.valueOf(System.currentTimeMillis());
      long userDate = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonObject, "timestamp", currDate));
      UtilityFunctions.addToDatastore(userName, userEmail, userDate, userComment, 0, 0, false);

    }
  }
}
