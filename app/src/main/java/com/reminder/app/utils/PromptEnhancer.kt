package com.reminder.app.utils

import android.util.Log
import java.util.regex.Pattern

/**
 * Prompt Enhancer - Improves user prompts by providing additional context, clarification, or rephrasing
 * Helps users create more effective and detailed reminders
 */
class PromptEnhancer {
    
    companion object {
        private const val TAG = "PromptEnhancer"
        
        // Common vague terms that need clarification
        private val VAGUE_TERMS = listOf(
            "later", "soon", "sometime", "eventually", "at some point", 
            "in a bit", "in a while", "next time", "soonish"
        )
        
        // Common incomplete task descriptions
        private val INCOMPLETE_TASKS = listOf(
            "call", "email", "text", "meet", "buy", "go", "do", "finish", "start"
        )
        
        // Context enhancers based on time of day
        private val TIME_CONTEXT_ENHANCERS = mapOf(
            "morning" to listOf("before work", "after breakfast", "during morning routine"),
            "afternoon" to listOf("after lunch", "during work hours", "mid-day break"),
            "evening" to listOf("after work", "before dinner", "evening routine"),
            "night" to listOf("before bed", "after dinner", "wind down time")
        )
        
        // Priority enhancers
        private val PRIORITY_ENHANCERS = mapOf(
            "urgent" to listOf("as soon as possible", "first thing", "immediately"),
            "important" to listOf("high priority", "don't forget", "make time for"),
            "casual" to listOf("when convenient", "if time permits", "no rush")
        )
    }
    
    data class EnhancedPrompt(
        val originalPrompt: String,
        val enhancedPrompt: String,
        val enhancementType: EnhancementType,
        val explanation: String,
        val confidence: Float
    )
    
    enum class EnhancementType {
        TIME_SPECIFIC,      // Added specific time
        TASK_COMPLETION,     // Added task details
        CONTEXT_ADDED,       // Added context
        PRIORITY_CLARIFIED,  // Clarified priority
        REPURPOSED,         // Rephrased for clarity
        MULTIPLE_IMPROVEMENTS // Multiple enhancements
    }
    
    /**
     * Enhance a user prompt with additional context and clarification
     */
    fun enhancePrompt(originalPrompt: String): List<EnhancedPrompt> {
        Log.d(TAG, "Enhancing prompt: '$originalPrompt'")
        
        val enhancements = mutableListOf<EnhancedPrompt>()
        val lowerPrompt = originalPrompt.lowercase().trim()
        
        // Check for vague time references
        VAGUE_TERMS.forEach { vagueTerm ->
            if (lowerPrompt.contains(vagueTerm)) {
                enhancements.addAll(createTimeSpecificEnhancements(originalPrompt, vagueTerm))
            }
        }
        
        // Check for incomplete tasks
        INCOMPLETE_TASKS.forEach { incompleteTask ->
            if (lowerPrompt.startsWith(incompleteTask) || lowerPrompt.contains(" $incompleteTask ")) {
                enhancements.addAll(createTaskCompletionEnhancements(originalPrompt, incompleteTask))
            }
        }
        
        // Check for time context opportunities
        TIME_CONTEXT_ENHANCERS.forEach { (timeContext, enhancers) ->
            if (lowerPrompt.contains(timeContext)) {
                enhancements.addAll(createContextEnhancements(originalPrompt, timeContext, enhancers))
            }
        }
        
        // Check for priority clarification opportunities
        PRIORITY_ENHANCERS.forEach { (priority, enhancers) ->
            if (lowerPrompt.contains(priority)) {
                enhancements.addAll(createPriorityEnhancements(originalPrompt, priority, enhancers))
            }
        }
        
        // Create a general improvement if no specific enhancements were found
        if (enhancements.isEmpty()) {
            enhancements.add(createGeneralImprovement(originalPrompt))
        }
        
        // Create a comprehensive enhancement that combines multiple improvements
        if (enhancements.size > 1) {
            enhancements.add(createComprehensiveEnhancement(originalPrompt, enhancements))
        }
        
        Log.d(TAG, "Generated ${enhancements.size} enhancements")
        return enhancements.take(5) // Limit to top 5 enhancements
    }
    
    private fun createTimeSpecificEnhancements(originalPrompt: String, vagueTerm: String): List<EnhancedPrompt> {
        val enhancements = mutableListOf<EnhancedPrompt>()
        
        // Replace vague term with specific times
        val specificTimes = listOf(
            "tomorrow at 9:00 AM",
            "today at 2:00 PM", 
            "this evening at 6:00 PM",
            "next Monday at 10:00 AM"
        )
        
        specificTimes.forEach { specificTime ->
            val enhancedPrompt = originalPrompt.replace(Regex("(?i)$vagueTerm"), specificTime)
            enhancements.add(
                EnhancedPrompt(
                    originalPrompt = originalPrompt,
                    enhancedPrompt = enhancedPrompt,
                    enhancementType = EnhancementType.TIME_SPECIFIC,
                    explanation = "Replaced '$vagueTerm' with specific time: $specificTime",
                    confidence = 0.8f
                )
            )
        }
        
        return enhancements
    }
    
    private fun createTaskCompletionEnhancements(originalPrompt: String, incompleteTask: String): List<EnhancedPrompt> {
        val enhancements = mutableListOf<EnhancedPrompt>()
        
        // Common completions for incomplete tasks
        val taskCompletions = when (incompleteTask) {
            "call" -> listOf("call mom", "call doctor", "call client", "call bank")
            "email" -> listOf("email boss", "email professor", "email customer service")
            "text" -> listOf("text friend", "text roommate", "text colleague")
            "meet" -> listOf("meet with team", "meet client", "meet friend for coffee")
            "buy" -> listOf("buy groceries", "buy birthday gift", "buy medicine")
            "go" -> listOf("go to gym", "go to post office", "go to bank")
            "do" -> listOf("do laundry", "do homework", "do taxes")
            "finish" -> listOf("finish report", "finish project", "finish assignment")
            "start" -> listOf("start diet", "start exercise routine", "start new book")
            else -> listOf("$incompleteTask important task")
        }
        
        taskCompletions.forEach { completion ->
            val enhancedPrompt = originalPrompt.replace(Regex("(?i)^$incompleteTask\\s*"), "$completion ")
                .replace(Regex("(?i)\\s+$incompleteTask\\s*"), " $completion ")
            enhancements.add(
                EnhancedPrompt(
                    originalPrompt = originalPrompt,
                    enhancedPrompt = enhancedPrompt,
                    enhancementType = EnhancementType.TASK_COMPLETION,
                    explanation = "Added specific task: $completion",
                    confidence = 0.7f
                )
            )
        }
        
        return enhancements
    }
    
    private fun createContextEnhancements(originalPrompt: String, timeContext: String, enhancers: List<String>): List<EnhancedPrompt> {
        val enhancements = mutableListOf<EnhancedPrompt>()
        
        enhancers.forEach { enhancer ->
            val enhancedPrompt = "$originalPrompt $enhancer"
            enhancements.add(
                EnhancedPrompt(
                    originalPrompt = originalPrompt,
                    enhancedPrompt = enhancedPrompt,
                    enhancementType = EnhancementType.CONTEXT_ADDED,
                    explanation = "Added context for '$timeContext': $enhancer",
                    confidence = 0.6f
                )
            )
        }
        
        return enhancements
    }
    
    private fun createPriorityEnhancements(originalPrompt: String, priority: String, enhancers: List<String>): List<EnhancedPrompt> {
        val enhancements = mutableListOf<EnhancedPrompt>()
        
        enhancers.forEach { enhancer ->
            val enhancedPrompt = "$enhancer: $originalPrompt"
            enhancements.add(
                EnhancedPrompt(
                    originalPrompt = originalPrompt,
                    enhancedPrompt = enhancedPrompt,
                    enhancementType = EnhancementType.PRIORITY_CLARIFIED,
                    explanation = "Clarified priority for '$priority': $enhancer",
                    confidence = 0.6f
                )
            )
        }
        
        return enhancements
    }
    
    private fun createGeneralImprovement(originalPrompt: String): EnhancedPrompt {
        // Add structure and clarity to general prompts
        val enhancedPrompt = when {
            originalPrompt.length < 10 -> {
                // Very short prompt - add structure
                "Remember to: $originalPrompt"
            }
            !originalPrompt.contains("remind me to", ignoreCase = true) && 
            !originalPrompt.contains("remember to", ignoreCase = true) -> {
                // Add reminder prefix
                "Remind me to $originalPrompt"
            }
            else -> {
                // Rephrase for better clarity
                originalPrompt.replace(Regex("(?i)(remind me to|remember to)"), "I need to")
            }
        }
        
        return EnhancedPrompt(
            originalPrompt = originalPrompt,
            enhancedPrompt = enhancedPrompt,
            enhancementType = EnhancementType.REPURPOSED,
            explanation = "Improved structure and clarity",
            confidence = 0.5f
        )
    }
    
    private fun createComprehensiveEnhancement(originalPrompt: String, enhancements: List<EnhancedPrompt>): EnhancedPrompt {
        // Combine the best elements from multiple enhancements
        val timeEnhancement = enhancements.find { it.enhancementType == EnhancementType.TIME_SPECIFIC }
        val taskEnhancement = enhancements.find { it.enhancementType == EnhancementType.TASK_COMPLETION }
        val contextEnhancement = enhancements.find { it.enhancementType == EnhancementType.CONTEXT_ADDED }
        
        var comprehensivePrompt = originalPrompt
        
        // Apply time enhancement first
        timeEnhancement?.let {
            comprehensivePrompt = it.enhancedPrompt
        }
        
        // Then apply task enhancement
        taskEnhancement?.let {
            comprehensivePrompt = it.enhancedPrompt
        }
        
        // Finally add context
        contextEnhancement?.let {
            comprehensivePrompt = "${it.enhancedPrompt} ${it.enhancedPrompt.substringAfter(originalPrompt)}"
        }
        
        return EnhancedPrompt(
            originalPrompt = originalPrompt,
            enhancedPrompt = comprehensivePrompt,
            enhancementType = EnhancementType.MULTIPLE_IMPROVEMENTS,
            explanation = "Combined multiple improvements for better clarity",
            confidence = 0.9f
        )
    }
    
    /**
     * Get a description of the enhancement type
     */
    fun getEnhancementTypeDescription(type: EnhancementType): String {
        return when (type) {
            EnhancementType.TIME_SPECIFIC -> "⏰ Added specific time"
            EnhancementType.TASK_COMPLETION -> "📝 Added task details"
            EnhancementType.CONTEXT_ADDED -> "📍 Added context"
            EnhancementType.PRIORITY_CLARIFIED -> "🔥 Clarified priority"
            EnhancementType.REPURPOSED -> "🔄 Improved structure"
            EnhancementType.MULTIPLE_IMPROVEMENTS -> "✨ Multiple improvements"
        }
    }
    
    /**
     * Check if a prompt needs enhancement
     */
    fun needsEnhancement(prompt: String): Boolean {
        val lowerPrompt = prompt.lowercase().trim()
        
        // Check for vague terms
        val hasVagueTime = VAGUE_TERMS.any { lowerPrompt.contains(it) }
        
        // Check for incomplete tasks
        val hasIncompleteTask = INCOMPLETE_TASKS.any { 
            lowerPrompt.startsWith(it) || lowerPrompt.contains(" $it ") 
        }
        
        // Check for very short prompts
        val isVeryShort = prompt.length < 10
        
        // Check for missing time information
        val hasNoTime = !lowerPrompt.contains(Regex("(today|tomorrow|morning|afternoon|evening|night|am|pm|\\d{1,2}:\\d{2})"))
        
        return hasVagueTime || hasIncompleteTask || isVeryShort || hasNoTime
    }
}