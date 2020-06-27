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
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

public final class FindMeetingQuery {

  /** Returns true if {@event} is not attended by any attendees in the meeting request */
  private Boolean hasNoCommonAttendees(Event event, MeetingRequest meeting) {
    Collection<String> eventAttendees = event.getAttendees();
    Collection<String> requiredAttendees = meeting.getAttendees();
    return Collections.disjoint(eventAttendees, requiredAttendees);
  }

  /** Returns true if available duration in {@vacantSlot} is less than requested duration */
  private Boolean isSlotInsufficient(TimeRange vacantSlot, MeetingRequest meeting) {
    return vacantSlot.duration() < meeting.getDuration();
  }

  /** Returns true if {@requeste} has no attendees */
  private Boolean hasNoAttendees(MeetingRequest request) {
    return request.getAttendees().size() == 0;
  }

  /** Returns true if duration in {@request} is negative or over a day long */
  private Boolean isDurationInvalid(MeetingRequest request) {
    return request.getDuration() > TimeRange.END_OF_DAY || request.getDuration() < 0;
  }

  private void sortTimeRangeByStart(List<TimeRange> list) {
    Collections.sort(list, TimeRange.ORDER_BY_START);
  }

  /** Returns a List {@queue} with start/end-bounding TimeRange objects */
  private List<TimeRange> createInitializedOccupiedQueue() {
    List<TimeRange> queue = new ArrayList<TimeRange>();
    queue.add(TimeRange.fromStartDuration(TimeRange.START_OF_DAY, 0));
    queue.add(TimeRange.fromStartDuration(TimeRange.END_OF_DAY + 1, 0));
    return queue;
  }

  /**
   * Function that updates an TimeRange object at index {@currentSlotIdx} with an 
   * updated duration if there are TimeRange objects chaining/within the current object.
   * 03:00 |    B  06:00 |              03:00          07:00
   * |      A      |   07:00        ->    |      A      |
   * @param queue List of TimeRange objects, sorted by start time.
   * @param currentSlotIdx Index of element to be modified in {@queue}
   */
  private void combineTimeRange(List<TimeRange> queue, int currentSlotIdx) {
    int adjSlotIdx = currentSlotIdx + 1;
    TimeRange currentSlot = queue.get(currentSlotIdx);
    TimeRange adjSlot = queue.get(adjSlotIdx);

    // start combining adjacent/containing events with regards to current event
    while (currentSlot.overlaps(adjSlot)) {
      int curSlotStart = currentSlot.start();
      int adjSlotEnd = adjSlot.end();
      // Combine events if it is a chained event.
      if (!currentSlot.contains(adjSlotEnd)) {
        TimeRange combinedSlot = TimeRange.fromStartEnd(
          curSlotStart, adjSlotEnd, false);
          queue.set(currentSlotIdx, combinedSlot);
      }
      // Get next slot in line if available
      queue.remove(adjSlotIdx);
      if(adjSlotIdx == queue.size()) break;
      else adjSlot = queue.get(adjSlotIdx);
    }
  }


  /**
   * Creates a vacant slot based on the provided index of the unavailable timeslot.
   * 
   * // |OE1|   <vacantEvent here>   |OE2|
   * @param occupiedQueue Combined list of TimeRange with TimeRange that is unavailable
   * @param slotIdx Index of TimeRange Object in front of the VacantSlot in {@occupiedQueue}
   */
  private TimeRange createVacantSlot(List<TimeRange> occupiedQueue, int slotIdx) {
    TimeRange occupiedSlot1 = occupiedQueue.get(slotIdx);
    TimeRange occupiedSlot2 = occupiedQueue.get(slotIdx + 1);
    return TimeRange.fromStartEnd(occupiedSlot1.end(), occupiedSlot2.start(), false);
  }

  public Collection<TimeRange> query(Collection<Event> eventsSource, MeetingRequest request) {

    if (hasNoAttendees(request)) {
      return Collections.singletonList(TimeRange.WHOLE_DAY);
    }
    if (isDurationInvalid(request)) {
      return Collections.emptyList();
    }

    List<TimeRange> vacantSlots= new ArrayList<TimeRange>();
    List<TimeRange> occupiedQueue = createInitializedOccupiedQueue();
    // Duplicate events list to support removal of events
    List<Event> events = new ArrayList<>(eventsSource);

    events.removeIf(event -> hasNoCommonAttendees(event, request));

    // Create entry timeslots for the events
    for (Event event: events) {
      occupiedQueue.add(event.getWhen());
    }

    sortTimeRangeByStart(occupiedQueue);

    // Combine unavailable timeslots so that no two timeslots are connecting
    for (int i = 0; i < occupiedQueue.size() - 1; i++) {
      combineTimeRange(occupiedQueue, i);
    }
  
    // Create vacant timeslots according to gaps in unavailable timeslots
    for (int i = 0; i < occupiedQueue.size() - 1; i++) {
      vacantSlots.add(createVacantSlot(occupiedQueue, i));
    }

    vacantSlots.removeIf(timeSlot -> isSlotInsufficient(timeSlot, request));
    return vacantSlots;
  }
}
