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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.JsonObject;
import javax.servlet.http.HttpServletRequest;

public class UtilityFunctions {

  /*
   * Extracts the value of fieldName attribute from jsonObject if present
   * and returns defaultValue if it is not or the value is empty
   */
  public static String getFieldFromJsonObject(JsonObject jsonObject, String fieldName
        , String defaultValue) {
    if (jsonObject.has(fieldName)) {
      String fieldValue = jsonObject.get(fieldName).getAsString();
      return (fieldValue.length() == 0) ? defaultValue : fieldValue;
    }
    return defaultValue;
  }

  // Adds a comment with the given metadata to the database  
  public static void addToDatastore(String name, String email, long dateTime, String comment
        , long parentId, long rootId, boolean isReply, long upvotes, long downvotes) {
    if ((isReply && (parentId == 0)) || (isReply && (rootId == 0))) {
        return;
    }
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    IncompleteKey key = keyFactory.setKind("Comment").newKey();
    FullEntity<IncompleteKey> thisComment =
        FullEntity.newBuilder(key)
          .set("name", name)
          .set("email", email)
          .set("time", dateTime)
          .set("comment", comment)
          .set("parentid", parentId)
          .set("rootid", rootId)
          .set("upvotes", upvotes)
          .set("score", upvotes - downvotes)
          .set("voters", "{}")
          .build();
    datastore.add(thisComment);
  }

  /*
   * Get value for fieldName for request if present. Return defaultValue if fieldName is not present
   * or the associated value is empty. Raise an exception if fieldName is mapped to multiple values.
   */
  public static String getFieldFromResponse(HttpServletRequest request, String fieldName
      , String defaultValue) {
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
