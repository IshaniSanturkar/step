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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/update-vote")
public class VoteServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /update-vote URL
   * Changes the number of upvotes/downvotes of the comment based on the
   * request as well as the net score of the comment
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonVote = UtilityFunctions.stringToJsonObject(parsedBody);

    long commentId = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(
        jsonVote, "id", "0"));
    long amount = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(
        jsonVote, "amt", "0"));

    if (commentId != 0 && amount != 0) {
      boolean isUpvote = Boolean.parseBoolean(UtilityFunctions.getFieldFromJsonObject(
        jsonVote, "isupvote", "true"));
      changeVoteInDatastore(commentId, isUpvote, amount);
    }
  }

  /* 
   * Registers that the comment represented by commentId has been upvoted (if isUpvote
   * is true) or downvoted (if isUpvote is false) if amount is 1 and the user hasn't
   * already upvoted or downvoted the same comment. If they have, no change occurs.
   * If amount is -1 and the user has upvoted [downvoted] a comment and isUpvote is true
   * [false] then the vote is deregistered so that the user has no vote towards this comment.
   * If amount is -1 and isUpvote is true and the user has downvoted the comment, no change 
   * occurs.
   */
  private void changeVoteInDatastore(long commentId, boolean isUpvote, long amount) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    Entity comment = datastore.get(keyFactory.newKey(commentId));
    
    String voters = comment.getString("voters");
    JsonObject obj = UtilityFunctions.stringToJsonObject(voters);
    String userId = UtilityFunctions.getCurrentUserId();

    /* 
     * The user has upvoted a comment and is trying to downvote it 
     * or has downvoted the comment and is trying to upvote it. In
     * this case, no change should occur. 
     */
    if (obj.has(userId) && amount == 1) {
      return;
    } else if (obj.has(userId) && amount == -1) {
      /*
       * User has upvoted/downvoted the comment and is trying to 
       * revert their vote
       */
      boolean status = obj.get(userId).getAsBoolean();
      // Making sure that the user is reverting the exact vote they made
      if(status != isUpvote) {
        return;
      } 
      obj.remove(userId);
    } else {
        // User has no vote on this comment currently and is making a fresh vote
        obj.addProperty(userId, isUpvote);
    }

    long upvotes = comment.getLong("upvotes");
    long score = comment.getLong("score");
    long downvotes = upvotes - score;
    Entity updatedComment;
    if (isUpvote) {
      updatedComment = Entity.newBuilder(comment).set("upvotes", upvotes + amount)
          .set("score", score + amount).set("voters", obj.toString()).build();
    } else {
      updatedComment = Entity.newBuilder(comment).set("score", score - amount)
          .set("voters", obj.toString()).build();
    }
    datastore.update(updatedComment);
  }
}
