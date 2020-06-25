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
import java.io.*;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // sort by time
    List<TimeRange> occupiedQueue = new ArrayList<TimeRange>();
    for (Event event: events) {
      occupiedQueue.add(event.getWhen());
    }
    Collections.sort(occupiedQueue, TimeRange.ORDER_BY_START);
    // combine events to make bigger chunks
    for (int currentEventIdx = 0;
         currentEventIdx < occupiedQueue.size();
        currentEventIdx++) {
      TimeRange currentSlot = occupiedQueue.get(currentEventIdx);
      int adjEventIdx = currentEventIdx + 1;

      // start combining adjacent/containing events with regards to current event
      while (adjEventIdx < occupiedQueue.size()) {
        TimeRange adjSlot = occupiedQueue.get(adjEventIdx);

        // If both events are overlapping, combine and remove adj_event from event list
        if (currentSlot.overlaps(adjSlot)) {
          int curSlotStart = currentSlot.start();
          int adjSlotEnd = adjSlot.end();
          if (currentSlot.contains(adjSlotEnd)) {
            occupiedQueue.remove(adjEventIdx);
          } else {
            // Combine events
            TimeRange combinedSlot = TimeRange.fromStartEnd(
              curSlotStart, adjSlotEnd, true);
            occupiedQueue.set(currentEventIdx, combinedSlot);
            // current_event.setWhen(
            //   TimeRange.fromStartDuration(
            //     cur_event_start, adj_event_end - cur_event_start));
            occupiedQueue.remove(adjEventIdx);
          }
        } else {
        //  Both events are entirely separate events, stop checking current events for
        //  overlaps
          break;
        }
      }
    }
    // create events to fill in the gap
    List<TimeRange> vacantEvents= new ArrayList<TimeRange>();
    TimeRange vacantSlot = null;
    for (int currentEventIdx = 0;
         currentEventIdx < occupiedQueue.size() - 1;
         currentEventIdx++) {
      // |OE1|   <vacantEvent here>   |OE2|
      TimeRange occupiedSlot1 = occupiedQueue.get(currentEventIdx);
      TimeRange occupiedSlot2 = occupiedQueue.get(currentEventIdx + 1);
      if (currentEventIdx == 0) {
        vacantSlot = TimeRange.fromStartEnd(
          TimeRange.START_OF_DAY, occupiedSlot1.start(), false);
      } else {
        vacantSlot = TimeRange.fromStartEnd(
          occupiedSlot1.end(), occupiedSlot2.start(), false);
      }
      vacantEvents.add(vacantSlot);
    }
    vacantEvents.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, false));
    return (Collection<TimeRange>)vacantEvents;
  }
}
