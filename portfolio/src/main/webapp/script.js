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

// Function that shows or hides the list of courses for a particular semester upon user click
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

// function that changes slideshow's picture to the next picture when the user presses the next button
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

/* function that changes slideshow's picture to the previous
   picture when the user presses the previous button */
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

/* function that is called every sliderSpeed seconds
   changes slideshow to display next image in list of
   images */
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

// modifies slider speed if user changes value of slider on gallery page
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

// function to pause or resume slideshow when user clicks on that window
function togglePause() {
  if (paused === true) {
    const statusImg = document.getElementById("pauseplay")
    statusImg.src = "/images/play.png";
    statusImg.style.display = "block";
    window.setTimeout(function () {
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
    window.setTimeout(function () {
      $("#pauseplay").fadeOut();
      statusImg.style.display = "none";
    }, 500);
    paused = true;
  }
}

// function that fetches data from the /data URL and displays it on the page
function loadComments() {
  fetch('/data').then(response => response.json()).then(comments => {
    const commentList = document.getElementById('comments');
    while (commentList.lastChild) {
      commentList.removeChild(commentList.lastChild);
    }
    for (const index in comments) {
      commentList.appendChild(createListElement(comments[index]));
    }
  });
}

// create a list element with the given text
function createListElement(txt) {
  const listElem = document.createElement("li");
  listElem.innerText = txt;
  return listElem;
}