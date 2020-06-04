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

const loc = "/images/life/";
let currImageNum = 1;
let sliderSpeed = 5000;
let myTimer = setInterval(loopOverImages, sliderSpeed);
let paused = false;

// Shows or hides the list of courses for a particular semester upon user click
function toggleCourseList(listObject) {
  const semester = listObject.id;
  const semCourseList = "courselist" + semester;
  const lst = document.getElementById(semCourseList);
  if (listObject.className === "up") {
    lst.className = "lstdown";
    listObject.className = "down";
  } else {
    lst.className = "lstup";
    listObject.className = "up";
  }
}

// Changes slideshow's picture to the next picture when the user presses the next button
function next() {
  if (!paused) {
    clearInterval(myTimer);
  }
  currImageNum++;
  if (currImageNum === 17) {
    currImageNum = 1;
  }
  const newImgPath = loc + currImageNum + ".jpg";
  document.getElementById("galleryimg").src = newImgPath;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Changes slideshow's picture to the previous picture when the user presses the previous button
function prev() {
  if (!paused) {
    clearInterval(myTimer);
  }
  currImageNum--;
  if (currImageNum === 0) {
    currImageNum = 16;
  }
  const newImgPath = loc + currImageNum + ".jpg";
  document.getElementById("galleryimg").src = newImgPath;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Is called every sliderSpeed seconds, changes slideshow to display next image in list of images
function loopOverImages() {
  if (!paused) {
    currImageNum++;
    if (currImageNum === 17) {
      currImageNum = 1;
    }
    const newImgPath = loc + currImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
  }
}

// Modifies slider speed if user changes value of slider on gallery page
function sliderMoved() {
  if (!paused) {
    clearInterval(myTimer);
  }
  const sliderValue = document.getElementById("galleryslider").value;
  sliderSpeed = sliderValue * 1000;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Pauses or resumes slideshow when user clicks on that window
function togglePause() {
  if (paused === true) {
    const statusImg = document.getElementById("pauseplay")
    statusImg.src = "/images/play.png";
    statusImg.style.display = "block";
    window.setTimeout(function() {
      $("#pauseplay").fadeOut();
      statusImg.style.display = "none";
    }, 500);
    myTimer = setInterval(loopOverImages, sliderSpeed);
    paused = false;
  } else {
    clearInterval(myTimer);
    const statusImg = document.getElementById("pauseplay")
    statusImg.src = "/images/pause.png";
    statusImg.style.display = "block";
    window.setTimeout(function() {
      $("#pauseplay").fadeOut();
      statusImg.style.display = "none";
    }, 500);
    paused = true;
  }
}

/**
 * Retrieves parameters related to comment ordering and display,
 * submits a GET request to /data, receives the response
 * and displays it on the page
 */
function loadComments() {
  const maxcomments = document.getElementById("numcomments").value;
  const sortMetric = document.getElementById("sortby").value;
  let sortOrder = "";
  if (document.getElementById("sortorder").classList.contains("desc")) {
    sortOrder = "desc";
  } else {
    sortOrder = "asc";
  }
  const fetchString = `/data?maxcomments=${maxcomments}&metric=${sortMetric}&order=${sortOrder}`;
  fetch(fetchString).then(response => response.json()).then(comments => {
    const commentList = document.getElementById("toplevelcomments");
    while (commentList.lastChild) {
      commentList.removeChild(commentList.lastChild);
    }
    const commentTree = locateChildren(comments);
    let numDisplayed = 0;
    for (commentId in commentTree) {
      if (numDisplayed == maxcomments) {
        break;
      }
      let comment = commentTree[commentId];
      if (comment["parentId"] === 0) {
        numDisplayed++;
        commentList.appendChild(constructReplyTree(comment, commentTree, 40));
      }
    }
    commentList.style.marginLeft = "20px";
  });
}

function locateChildren(comments) {
  let commentTree = {};
  for (let i = 0; i < comments.length; i++) {
    let id = comments[i]["id"];
    commentTree[id] = comments[i];
    commentTree[id]["children"] = [];
    for (let j = 0; j < comments.length; j++) {
      if (comments[j]["parentId"] === comments[i]["id"]) {
        commentTree[id]["children"].push(comments[j]);
      }
    }
  }
  return commentTree;
}

function constructReplyTree(comment, commentTree, margin) {
  let children = commentTree[comment["id"]]["children"];
  if (children.length === 0) {
    let thisReply = createListElement(comment);
    return thisReply;
  } else {
    let thisReply = createListElement(comment);
    let replyTree = document.createElement("ul");
    replyTree.className = "comments";
    const newMargin = margin + 20;
    for (const child of children) {
      const subTree = constructReplyTree(child, commentTree, newMargin);
      replyTree.appendChild(subTree);
    }
    replyTree.style.marginLeft = `${margin}px`;
    thisReply.appendChild(replyTree);
    return thisReply;
  }
}

// Creates a list element with the given comment text and metadata (name, timestamp etc.)
function createListElement(comment) {
  const listElem = document.createElement("li");
  const metadata = formatCommentMetadata(comment);
  const quote = formatCommentText(comment);
  const reply = formatCommentReply(comment);
  const thisCommentDiv = document.createElement("div");
  thisCommentDiv.appendChild(metadata);
  thisCommentDiv.appendChild(quote);
  thisCommentDiv.appendChild(reply);
  thisCommentDiv.className = "comment";
  listElem.appendChild(thisCommentDiv);
  return listElem;
}

// Formats comment name and timestamp into an HTML p element
function formatCommentMetadata(comment) {
  let date = new Date(comment["timestamp"]);
  const metadata = `${comment["name"]} at ${date.toLocaleString()} said`;
  const pElem = document.createElement("p");
  pElem.innerText = metadata;
  pElem.className = "comment_metadata";
  return pElem;
}

// Formats comment text into an HTML blockquote element
function formatCommentText(comment) {
  const quote = document.createElement("blockquote");
  quote.innerText = comment["comment"];
  return quote;
}

function formatCommentReply(comment) {
  const replyDiv = document.createElement("div");
  replyDiv.className = "replydiv";
  const replyBar = document.createElement("textarea");
  replyBar.className = "replybar";
  replyBar.id = `${comment["id"]}-bar`;
  const replyButton = document.createElement("button");
  replyButton.innerText = "Reply";
  replyButton.className = "replybutton";
  replyButton.onclick = () => replyTo(comment);
  replyDiv.appendChild(replyBar);
  replyDiv.appendChild(replyButton);
  return replyDiv;
}

function replyTo(comment) {
  const replyId = `${comment["id"]}-bar`;
  const replyContent = document.getElementById(replyId).value;
  const replyObj = {};
  replyObj["comment"] = replyContent;
  replyObj["parentid"] = comment["id"];
  fetch('/reply', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(replyObj),
  }).then(response => {
    loadComments();
    document.getElementById("replyId").reset();
  });
}

/**
 * Sets value of form submission time to current time in client's timezone,
 * posts form data to server, reloads comments section and then clears
 * form in preparation for next entry
 */
function submitForm(form) {
  const today = new Date();
  form.timestamp.value = today.getTime();
  const formData = {};
  const dataArray = $("#newcommentform").serializeArray();
  dataArray.forEach(entry => formData[entry.name] = entry.value);
  fetch('/data', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(formData),
  }).then(response => {
    loadComments();
    document.getElementById("newcommentform").reset();
  });
}

/**
 * Submits a post request to /delete-data to delete all comments,
 * then reloads the comments section
 */
function clearComments() {
  fetch("/delete-data", {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: ''
  }).then(response => loadComments());
}

/**
 * Toggles sort order button between ascending and descending on click
 * and then reloads comment section to effect the change
 */
function changeSortOrder() {
  const sortOrderButton = document.getElementById("sortorder");
  if (sortOrderButton.classList.contains("desc")) {
    sortOrderButton.classList.replace("desc", "asc");
    sortOrderButton.innerText = "arrow_drop_up";
  } else {
    sortOrderButton.classList.replace("asc", "desc");
    sortOrderButton.innerText = "arrow_drop_down";
  }
  loadComments();
}