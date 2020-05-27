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

// Function that shows or hides the list of courses
// for a particular semester upon user click
function toggleCourseList(listObject) {
    let semester = listObject.id;
    let semCourseList = "courselist" + semester;
    let lst = document.getElementById(semCourseList);
    if(lst.style.display === "none" || lst.style.display === "")
    {
        lst.style.display = "block";
    } else {
        lst.style.display = "none";
    }
}

// gets the number of the image currently being 
// displayed on the slideshow
function getImageNum() {
    let currImg = document.getElementById("galleryimg");
    let imgName = currImg.src;
    const loc = "/images/life/"
    let startOfImgNum = imgName.indexOf(loc) + loc.length;
    let endOfImgNum = imgName.indexOf(".jpg");
    let imgNum = Number(imgName.slice(startOfImgNum, endOfImgNum));
    return imgNum;
}

// function that changes slideshow's picture to the next 
// picture when the user presses the next button
function next() {
    const loc = "/images/life/"
    let imgNum = getImageNum();
    let newImageNum = (imgNum + 1);
    if(newImageNum === 17)
    {
        newImageNum = 1;
    }
    let newImgPath = loc + newImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
}

// function that changes slideshow's picture to the previous 
// picture when the user presses the previous button
function prev() {
    const loc = "/images/life/"
    let imgNum = getImageNum();
    let newImageNum = (imgNum - 1);
    if(newImageNum === 0)
    {
        newImageNum = 16;
    }
    let newImgPath = loc + newImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
}