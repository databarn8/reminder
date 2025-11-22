# Repeat Pattern and Alert Timing Implementation Plan

## Problem Statement
The user reports that despite previous changes, they still don't see:
1. Repeat pattern options (minutes, hours, days, week, month)
2. End condition options (how many times)
3. Alert timing options (e.g., 30 minutes before, 5 hours before)

The user wants a simple design with good selections based on online calendar design best practices.

## Research Findings
Based on calendar app design best practices:
1. **Simplicity is key** - Users should be able to set repeat patterns with minimal taps
2. **Smart defaults** - Pre-select common options based on context
3. **Visual clarity** - Clear labels and intuitive groupings
4. **Progressive disclosure** - Show simple options first, advanced options on demand

## Implementation Plan

### Phase 1: Analyze Current Implementation
1. Check if repeat pattern and alert timing components are properly integrated in InputScreen.kt
2. Verify that data structures (Reminder.kt) support the required fields
3. Identify why the features aren't visible to the user

### Phase 2: Design Simple Repeat Pattern Selector
1. Create a new simplified repeat pattern selector with:
   - **Quick Options**: 5 min, 15 min, 30 min, 1 hour, 1 day, 1 week, 1 month
   - **Custom Option**: "Custom..." for advanced users
   - **End Condition**: After X times (3, 5, 10, 20) or End on specific date
2. Use a card-based layout with clear labels
3. Implement smart defaults based on reminder type (e.g., medication reminders default to daily)

### Phase 3: Design Alert Timing Selector
1. Create a simple alert timing selector with:
   - **Quick Options**: On time, 5 min before, 15 min before, 30 min before, 1 hour before
   - **Custom Option**: "Custom..." for specific timing
2. Default to "15 min before" for most reminder types
3. Use a dropdown or segmented control for easy selection

### Phase 4: Integration
1. Update InputScreen.kt to use the new components
2. Ensure data persistence works correctly
3. Test with various reminder types and repeat patterns

### Phase 5: Testing and Refinement
1. Test the complete flow from creating a reminder to saving it
2. Verify that repeat patterns and alert timings work as expected
3. Make any necessary adjustments based on testing

## Technical Considerations
1. **Data Model**: Ensure Reminder.kt has fields for:
   - repeatInterval (minutes)
   - repeatEndCondition (type: times or date)
   - repeatEndValue (number of times or date)
   - alertTimingOffset (minutes before event)

2. **UI Components**:
   - Use Material 3 components for consistency
   - Keep touch targets large enough (48dp minimum)
   - Use proper spacing and typography

3. **State Management**:
   - Use mutableStateOf for reactive UI
   - Ensure state is properly restored on configuration changes

## Implementation Steps
1. Create new SimplifiedRepeatPatternSelector.kt component
2. Create new SimplifiedAlertTimingSelector.kt component
3. Update InputScreen.kt to integrate these components
4. Update Reminder.kt data model if needed
5. Test the complete implementation
6. Build and deploy for user testing

## Success Criteria
1. User can easily select repeat patterns (minutes, hours, days, week, month)
2. User can set end conditions (after X times or on specific date)
3. User can set alert timing (on time, X minutes/hours before)
4. All options have smart defaults based on reminder context
5. UI is simple and intuitive with minimal taps required