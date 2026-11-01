---
name: greedy-intervals-plan
description: Resumable study plan for Topic 17 - Greedy Algorithms and Intervals
metadata:
  type: project
---

# Study Plan — Topic 17: Greedy Algorithms and Intervals

**Status:** In Progress  
**Started:** 2026-11-01  
**Estimated time:** 1 hour  
**Language:** Kotlin (Java-compatible)  
**Target:** FAANG-level product companies  

## Checklist

- [ ] Read concept explanation (greedy algorithms, intervals, internal working, analogy)
- [ ] Review time/space complexity for every major operation
- [ ] Study common mistakes and edge cases
- [ ] Learn interview patterns with brute → better → optimal approaches
- [ ] Complete dry run of Minimum Arrows to Burst Balloons step by step
- [ ] Study code template (line-by-line explanation)
- [ ] Solve 2 Easy problems (Non-overlapping Intervals, Merge Intervals)
- [ ] Solve 2 Medium problems (Min Arrows to Burst Balloons, Meeting Rooms II)
- [ ] Solve 2 Hard problems (Job Scheduling, Partition Labels)
- [ ] Decide: more practice / detailed solution / quiz / next topic

## Files Created

- `greedy_intervals.md` — Full teaching content (concept, patterns, dry run, code template, practice problems)
- `PROGRESS.md` — This resumable plan
- `solutions/Medium_NonOverlappingIntervals.kt` — LeetCode 435 (sort by end time)
- `solutions/Medium_MergeIntervals.kt` — LeetCode 56 (sort by start time, merge)
- `solutions/Medium_MinArrowsToBurst.kt` — LeetCode 452 (sort by end, greedy shoot)
- `solutions/Medium_MeetingRoomsII.kt` — LeetCode 253 (two pointers on sorted arrays)
- `solutions/Hard_JobScheduling.kt` — LeetCode 1235 (weighted interval scheduling with DP + binary search)
- `solutions/Medium_PartitionLabels.kt` — LeetCode 763 (last occurrence tracking)

## How to Resume

If interrupted, come back to `PROGRESS.md`, check off the next unchecked item, and continue from there. All code lives under:

```
data_structure/src/main/java/greedy_intervals/
```

Next topic after this: **Topic 18: Mixed Revision and Mock Interview Problems**

## Study Schedule (1 hour/day)

| Day | Focus | Time |
|-----|-------|------|
| 1 | Concept explanation + dry run + code template | 1 hour |
| 2 | Easy problems (2) | 1 hour |
| 3 | Medium problems (2) | 1 hour |
| 4 | Hard problems (2) | 1 hour |
| 5 | Review + decide next step | 1 hour |