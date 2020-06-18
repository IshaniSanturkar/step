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

package com.google.sps;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public final class FindMeetingQuery {

  /*
   * Creates a time range of length at least duration from block and its subsequent optional
   * blocks in busyTimes (which should have non-overlapping elements). Returns the end time 
   * of this new block or -1 if it is not possible
   */
  private int createCombinedBlock(TimeRange block, TreeSet<TimeRange> busyTimes, long duration, HashSet<String> blockOptBusy) {
    // Stores all time ranges later than this range
    Iterator<TimeRange> sub = busyTimes.tailSet(block, false).iterator();
    // Stores the start time of the larger time block
    int blockStart = block.start();
    // Stores the end time of the larger time block
    int blockEnd = block.end();
    // Stores the duration of the larger time block
    int blockDuration = block.duration();
    
    blockOptBusy.addAll(block.getOptBusy());
    while (sub.hasNext() && blockDuration < duration) {
      TimeRange next = sub.next();
      if (next.isReq()) {
        break;
      }
      blockDuration += next.duration();
      blockEnd = next.end();
      blockOptBusy.addAll(next.getOptBusy());
    }
    return (blockDuration >= duration) ? blockEnd : -1;
  }

  /*
   * Given a list of busy times in non-overlapping, sorted order and a meeting duration,
   * returns a list of time slots of atleast 'duration' length during which no meetings
   * are scheduled. First, the function tries to accomodate all optional and required
   * attendees. If that is not possible, it tries to find timeslots that all required
   * attendees and as many optional attendees as possible can attend. If no such
   * timeslots exist, timeslots that all required attendees can make are returned.
   */
  private ArrayList<TimeRange> findFreeTimes(TreeSet<TimeRange> busyTimes, long duration) {

    // stores the list of ranges where all required attendees are free
    ArrayList<TimeRange> reqFreeTimes = new ArrayList<>();
    // stores the list of ranges where required and all optional attendees are free
    ArrayList<TimeRange> freeTimes = new ArrayList<>();
    // represents the minimum number of busy optional attendees for any range so far
    int minOptBusy = Integer.MAX_VALUE;
    // stores all time ranges with minOptBusy number of busy optional attendees
    ArrayList<TimeRange> maxOptAttendTimes = new ArrayList<>();

    /*
     * This variable represents the start time of the current free (optional or required)
     * block. It starts out as the value of the start of the day and iteratively takes on
     * the value of the end of each busy block
     */
    int start = TimeRange.START_OF_DAY;
    /*
     * This variable represents the start time of the current required free block. It starts
     * out as the value of the start of the day and iteratively takes on the value of the
     * end of each required busy block
     */
    Iterator<TimeRange> iter = busyTimes.iterator();
    int reqStart = TimeRange.START_OF_DAY;
    while (iter.hasNext()) {
      TimeRange curr = iter.next();
      HashSet<String> optBusy = curr.getOptBusy();

      int thisStart = curr.start();
      int thisEnd = curr.end();
      /*
       * create a free slot from the beginning of the free period to 'thisStart',
       * the beginning of the next busy one
       */
      TimeRange newFree = TimeRange.fromStartEnd(start, thisStart, false);
      if (newFree.duration() >= duration) {
        freeTimes.add(newFree);
      }

      // We skip over optional blocks while making modifications to reqFreeTimes
      if (curr.isReq()) {
        TimeRange newFreeReq = TimeRange.fromStartEnd(reqStart, thisStart, false);
        if (newFreeReq.duration() >= duration) {
          reqFreeTimes.add(newFreeReq);
        }
        reqStart = thisEnd;
      } else {
        /*
         * Checks if this is a valid optional block with fewer busy attendees than the existing
         * minimum (in which case it replaces the minimum with the current value) or equal busy
         * attendees than the existing minimum in which case it is added to the list of such
         * ranges
         */
        
        /*
         * Use this if else statement to try to convert the current block into a large 
         * enough block for the meeting
         */
        if(curr.duration() >= duration) {
          // This block is valid by itself

          // In case this time block ends at the end of the day, make its end inclusive
          if (thisEnd == TimeRange.END_OF_DAY) {
            curr = TimeRange.fromStartEnd(thisStart, thisEnd, true);
          }
        } else {
          // If this block doesn't work, we may be able to coalesce it with
          // adjacent optional blocks and create a valid block

          // Stores all optional attendees who are busy during the larger time block
          HashSet<String> blockOptBusy = new HashSet<>();
          int blockEnd = createCombinedBlock(curr, busyTimes, duration, blockOptBusy);

          if (blockEnd != -1) {
            if (blockEnd == TimeRange.END_OF_DAY) {
              curr = TimeRange.fromStartEnd(thisStart, blockEnd, true);
            } else {
              curr = TimeRange.fromStartEnd(thisStart, blockEnd, false);
            }
          }
          optBusy = blockOptBusy;
        }
        if (curr.duration() >= duration && optBusy.size() < minOptBusy) {
          minOptBusy = optBusy.size();
          maxOptAttendTimes = new ArrayList<>();
          maxOptAttendTimes.add(curr);
        } else if (optBusy.size() == minOptBusy) {
          maxOptAttendTimes.add(curr);
        }
      }
      start = thisEnd;
    }
    /*
     * This represents the integer value of the end of the day. If possible, a free block
     * will be created from the end of the last busy block to the end of the day
     */
    int end = TimeRange.END_OF_DAY;
    TimeRange newFree = TimeRange.fromStartEnd(start, end, true);
    TimeRange newFreeReq = TimeRange.fromStartEnd(reqStart, end, true);
    if (newFree.duration() >= duration) {
      freeTimes.add(newFree);
    }
    if(newFreeReq.duration() >= duration) {
      reqFreeTimes.add(newFreeReq);
    }
    if (freeTimes.size() > 0) {
      // Try to find times that work for everyone
      return freeTimes;
    } else if (maxOptAttendTimes.size() != 0) {
      // if that's not possible, try to get all required and a maximal number of optional attendees
      // this case occurs when the duration of every optional time slot is too small
      return maxOptAttendTimes;
    }
    // If no times work with optional attendees, just return times that work for required attendees
    return reqFreeTimes;
  }

  /*
   * Definitions:
   * Optional block - one that can be attended by all required attendees but not by some
   * optional attendees (|---optional---|)
   * Required block - one that cannot be attended by all required attendees (|__required___|)
   */

  /*
   * Coalesces two overlapping optional blocks (constrained only by optional attendees)
   * in busyTimes, returning the next block to begin coalescing from
   */
  private TimeRange coalesceOptOpt(
      TimeRange first, TimeRange second, TreeSet<TimeRange> busyTimes) {
    /*
     * Lists of those optional attendees of the new meeting who are busy during the first and second
     * time ranges
     */
    HashSet<String> firstOptBusy = first.getOptBusy();
    HashSet<String> secondOptBusy = second.getOptBusy();

    if (first.contains(second)) {
      /*
       * Before coalesce: |-------------first--------------|
       *                          |-----second-----|
       *
       * After coalesce:  |--nF--||-----second-----||--nT--|
       */
      second.addOptBusy(firstOptBusy);
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newThird.addOptBusy(firstOptBusy);
      busyTimes.remove(first);
      busyTimes.add(newFirst);
      busyTimes.add(newThird);
      return newFirst;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |-------------second--------------|
       *                  |-----first-----|
       *
       * Note that since first is before second in the sorted list, it must start before
       * or at the same time as second
       *
       * After coalesce:  |-----first-----||-------nS-------|
       */
      first.addOptBusy(secondOptBusy);
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.remove(second);
      busyTimes.add(newSecond);
      return first;
    } else {
      /*
       * Before coalesce: |-------------first--------------|
       *                          |---------------second-------------|
       *
       * After coalesce:  |--nF--||------------nS----------||---nT---|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newSecond = TimeRange.fromStartEnd(second.start(), first.end(), false);
      newSecond.addOptBusy(firstOptBusy);
      newSecond.addOptBusy(secondOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newThird.addOptBusy(secondOptBusy);
      busyTimes.remove(first);
      busyTimes.remove(second);
      busyTimes.add(newFirst);
      busyTimes.add(newSecond);
      busyTimes.add(newThird);
      return newFirst;
    }
  }

  /*
   * Coalesces two overlapping blocks, the first optional and the second required
   * in busyTimes, returning the next block to begin coalescing from
   */
  private TimeRange coalesceOptReq(
      TimeRange first, TimeRange second, TreeSet<TimeRange> busyTimes) {
    /*
     * List of those optional attendees of the new meeting who are busy during the first
     * time range
     */
    HashSet<String> firstOptBusy = first.getOptBusy();
    if (first.contains(second)) {
      /*
       * Before coalesce: |-------------first-------------|
       *                          |_____second____|
       *
       * After coalesce:  |--nF--||_____second____||--nT--|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      TimeRange newThird = TimeRange.fromStartEnd(second.end(), first.end(), false);
      newThird.addOptBusy(firstOptBusy);
      busyTimes.remove(first);
      busyTimes.add(newFirst);
      busyTimes.add(newThird);
      return newFirst;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |___________second___________|
       *                  |----first-----|
       *
       * After coalesce:  |___________second___________|
       */
      busyTimes.remove(first);
      return second;
    } else {
      /*
       * Before coalesce: |----first-----|
       *                          |___________second___________|
       *
       * After coalesce:  |--nF--||___________second___________|
       */
      TimeRange newFirst = TimeRange.fromStartEnd(first.start(), second.start(), false);
      newFirst.addOptBusy(firstOptBusy);
      busyTimes.remove(first);
      busyTimes.add(newFirst);
      return newFirst;
    }
  }

  /*
   * Coalesces two overlapping blocks, the first required and the second optional
   * in busyTimes, returning the next block to begin coalescing from
   */
  private TimeRange coalesceReqOpt(
      TimeRange first, TimeRange second, TreeSet<TimeRange> busyTimes) {

    /*
     * List of those optional attendees of the new meeting who are busy during the second
     * time range
     */
    HashSet<String> secondOptBusy = second.getOptBusy();
    if (first.contains(second)) {
      /*
       * Before coalesce: |___________first___________|
       *                          |----second-----|
       *
       * After coalesce:  |___________first___________|
       */
      busyTimes.remove(second);
      return first;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |-------------second-------------|
       *                  |_____first____|
       *
       * After coalesce:  |_____first____||-------nS-------|
       */
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.remove(second);
      busyTimes.add(newSecond);
      return first;
    } else {
      /*
       * Before coalesce: |____first____|
       *                          |------second-----|
       *
       * After coalesce:  |_____first___||----nS----|
       */
      TimeRange newSecond = TimeRange.fromStartEnd(first.end(), second.end(), false);
      newSecond.addOptBusy(secondOptBusy);
      busyTimes.remove(second);
      busyTimes.add(newSecond);
      return first;
    }
  }

  /*
   * Coalesces two overlapping required blocks in busyTimes, returning the next block
   * to proceed with coalescing from
   */
  private TimeRange coalesceReqReq(
      TimeRange first, TimeRange second, TreeSet<TimeRange> busyTimes) {

    if (first.contains(second)) {
      /*
       * Before coalesce: |___________first_____________|
       *                         |___second___|
       *
       * After coalesce:  |___________first_____________|
       */
      busyTimes.remove(second);
      return first;
    } else if (second.contains(first)) {
      /*
       * Before coalesce: |___________second_____________|
       *                  |___first___|
       *
       * After coalesce:  |___________second_____________|
       */
      busyTimes.remove(first);
      return second;
    } else {
      /*
       * Before coalesce: |___________first_____________|
       *                                 |_______second_________|
       *
       * After coalesce:  |_____________nC______________________|
       */
      TimeRange newCombined = TimeRange.fromStartEnd(first.start(), second.end(), false);
      busyTimes.remove(first);
      busyTimes.remove(second);
      busyTimes.add(newCombined);
      // the next range after first in the ordered set
      return busyTimes.higher(first);
    }
  }

  /*
   * Given a set of busy times and a time range, coalesces the range with the
   * next larger range according to set order if they overlap. If there is no
   * such range, return. Otherwise continue the process with the next range to
   * coalesce.
   */
  private void coalesceOverlap(TreeSet<TimeRange> busyTimes, TimeRange curr) {
    // Get the first time range that is "larger" than this one
    TimeRange next = busyTimes.higher(curr);
    if (next == null) {
      return;
    }
    TimeRange nextStart = next;
    if (curr.overlaps(next)) {
      if (curr.isReq() && next.isReq()) {
        nextStart = coalesceReqReq(curr, next, busyTimes);
      } else if (curr.isReq()) {
        nextStart = coalesceReqOpt(curr, next, busyTimes);
      } else if (next.isReq()) {
        nextStart = coalesceOptReq(curr, next, busyTimes);
      } else {
        nextStart = coalesceOptOpt(curr, next, busyTimes);
      }
    }
    coalesceOverlap(busyTimes, nextStart);
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // A list of all busy times when at least one optional or required meeting attendee is busy
    TreeSet<TimeRange> busy = new TreeSet<>(TimeRange.ORDER_BY_START_END);
    HashSet<String> attendees = new HashSet<>(request.getAttendees());
    HashSet<String> optAttendees = new HashSet<>(request.getOptionalAttendees());
    Iterator<Event> iterator = events.iterator();

    while (iterator.hasNext()) {
      Event meeting = iterator.next();
      Set<String> meetingAttendees = meeting.getAttendees();

      if (!Sets.intersection(attendees, meetingAttendees).isEmpty()) {
        /*
         * If this meeting involves at least one required attendee of the new meeting, add it to
         * the list as a required busy time
         */
        TimeRange meetingTime = meeting.getWhen();
        busy.add(meetingTime);
      } else {
        SetView<String> optInMeeting = Sets.intersection(optAttendees, meetingAttendees);
        /*
         * If this meeting involves at least one optional attendee of the new meeting but no
         * required attendees, add it to the list as an optional busy time with the common
         * optional attendees
         */
        if (optInMeeting.size() != 0) {
          TimeRange meetingTime = meeting.getWhen();
          meetingTime.addOptBusy(optInMeeting);
          busy.add(meetingTime);
        }
      }
    }
    if (!(busy.size() == 0)) {
      // find the earliest range in the list
      TimeRange first = busy.first();
      coalesceOverlap(busy, first);
    }
    ArrayList<TimeRange> freeTimes = findFreeTimes(busy, request.getDuration());
    return freeTimes;
  }
}
