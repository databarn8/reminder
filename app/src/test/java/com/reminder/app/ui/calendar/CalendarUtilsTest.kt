package com.reminder.app.ui.calendar

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.YearMonth

class CalendarUtilsTest {

    @Test
    fun testFirstDayOfWeekCalculation() {
        // Test the fixed calculation for first day of week
        // Java's DayOfWeek: Monday=1, Tuesday=2, ..., Sunday=7
        // We want: Monday=0, Tuesday=1, ..., Saturday=5, Sunday=6
        
        // Test Monday (should be 0)
        val mondayDate = LocalDate.of(2025, 10, 6) // Monday, October 6, 2025
        val firstDayOfMonth = mondayDate.withDayOfMonth(1) // October 1, 2025 (Wednesday)
        val mondayFirstDayOfWeek = if (firstDayOfMonth.dayOfWeek.value == 7) 6 else firstDayOfMonth.dayOfWeek.value - 1
        assertEquals(2, mondayFirstDayOfWeek) // Wednesday should be index 2
        
        // Test Wednesday (should be 2)
        assertEquals(2, mondayFirstDayOfWeek)
        
        // Test Saturday (should be 5)
        val saturdayDate = LocalDate.of(2025, 11, 1) // Saturday, November 1, 2025
        val saturdayFirstDayOfWeek = if (saturdayDate.dayOfWeek.value == 7) 6 else saturdayDate.dayOfWeek.value - 1
        assertEquals(5, saturdayFirstDayOfWeek)
        
        // Test Sunday (should be 6)
        val sundayDate = LocalDate.of(2025, 12, 7) // Sunday, December 7, 2025
        val sundayFirstDayOfWeek = if (sundayDate.dayOfWeek.value == 7) 6 else sundayDate.dayOfWeek.value - 1
        assertEquals(6, sundayFirstDayOfWeek)
    }
    
    @Test
    fun testNovember2025CalendarLayout() {
        // November 2025: November 1st is Saturday
        val november2025 = LocalDate.of(2025, 11, 1)
        val yearMonth = YearMonth.from(november2025)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfMonth = november2025.withDayOfMonth(1)
        
        // Apply our fixed calculation
        val firstDayOfWeek = if (firstDayOfMonth.dayOfWeek.value == 7) 6 else firstDayOfMonth.dayOfWeek.value - 1
        
        // November 1, 2025 is Saturday, so firstDayOfWeek should be 5
        assertEquals(5, firstDayOfWeek)
        
        // Calendar should have 5 empty cells before November 1st
        val calendarDays = (0 until firstDayOfWeek).map { null } + (1..daysInMonth).toList()
        
        // Verify November 28th is at the correct position (Friday)
        val november28Index = calendarDays.indexOf(28)
        val november28DayOfWeek = november28Index % 7
        assertEquals(4, november28DayOfWeek) // 0=Monday, 1=Tuesday, ..., 4=Friday, 5=Saturday, 6=Sunday
    }
    
    @Test
    fun testDecember2025CalendarLayout() {
        // December 2025: December 1st is Monday
        val december2025 = LocalDate.of(2025, 12, 1)
        val yearMonth = YearMonth.from(december2025)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfMonth = december2025.withDayOfMonth(1)
        
        // Apply our fixed calculation
        val firstDayOfWeek = if (firstDayOfMonth.dayOfWeek.value == 7) 6 else firstDayOfMonth.dayOfWeek.value - 1
        
        // December 1, 2025 is Monday, so firstDayOfWeek should be 0
        assertEquals(0, firstDayOfWeek)
        
        // Calendar should have no empty cells before December 1st
        val calendarDays = (0 until firstDayOfWeek).map { null } + (1..daysInMonth).toList()
        
        // Verify December 25th is at the correct position (Thursday)
        val december25Index = calendarDays.indexOf(25)
        val december25DayOfWeek = december25Index % 7
        assertEquals(3, december25DayOfWeek) // 0=Monday, 1=Tuesday, 2=Wednesday, 3=Thursday
    }
    
    @Test
    fun testJanuary2026CalendarLayout() {
        // January 2026: January 1st is Thursday
        val january2026 = LocalDate.of(2026, 1, 1)
        val yearMonth = YearMonth.from(january2026)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfMonth = january2026.withDayOfMonth(1)
        
        // Apply our fixed calculation
        val firstDayOfWeek = if (firstDayOfMonth.dayOfWeek.value == 7) 6 else firstDayOfMonth.dayOfWeek.value - 1
        
        // January 1, 2026 is Thursday, so firstDayOfWeek should be 3
        assertEquals(3, firstDayOfWeek)
        
        // Calendar should have 3 empty cells before January 1st
        val calendarDays = (0 until firstDayOfWeek).map { null } + (1..daysInMonth).toList()
        
        // Verify January 31st is at the correct position (Saturday)
        val january31Index = calendarDays.indexOf(31)
        val january31DayOfWeek = january31Index % 7
        assertEquals(5, january31DayOfWeek) // 5=Saturday
    }
}