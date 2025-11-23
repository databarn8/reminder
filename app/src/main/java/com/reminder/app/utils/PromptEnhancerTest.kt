package com.reminder.app.utils

/**
 * Test class for PromptEnhancer functionality
 * Demonstrates how the enhancement feature works with various examples
 */
object PromptEnhancerTest {
    
    /**
     * Test the PromptEnhancer with various example prompts
     */
    fun runTests() {
        val enhancer = PromptEnhancer()
        
        // Test cases for different types of prompts that need enhancement
        val testPrompts = listOf(
            // Vague time references
            "Call mom later",
            "Buy groceries sometime",
            "Meeting tomorrow",
            
            // Incomplete tasks
            "Call",
            "Email",
            "Buy",
            
            // Very short prompts
            "Doctor",
            "Gym",
            "Work",
            
            // Missing time information
            "Call dentist",
            "Team meeting",
            "Submit report",
            
            // Complex prompts that could be improved
            "Remind me to call mom about birthday",
            "Remember to buy milk and bread",
            "Don't forget to finish the project"
        )
        
        println("=== Prompt Enhancement Test Results ===\n")
        
        testPrompts.forEachIndexed { index, prompt ->
            println("Test ${index + 1}: \"$prompt\"")
            
            // Check if enhancement is needed
            val needsEnhancement = enhancer.needsEnhancement(prompt)
            println("  Needs enhancement: $needsEnhancement")
            
            if (needsEnhancement) {
                // Get enhancements
                val enhancements = enhancer.enhancePrompt(prompt)
                println("  Generated ${enhancements.size} enhancements:")
                
                enhancements.forEach { enhancement ->
                    println("    - Type: ${enhancement.enhancementType}")
                    println("      Enhanced: \"${enhancement.enhancedPrompt}\"")
                    println("      Explanation: ${enhancement.explanation}")
                    println("      Confidence: ${(enhancement.confidence * 100).toInt()}%")
                    println()
                }
            } else {
                println("  No enhancement needed")
            }
            
            println("---")
        }
        
        println("\n=== Test Complete ===")
    }
    
    /**
     * Test specific enhancement types
     */
    fun testSpecificEnhancements() {
        val enhancer = PromptEnhancer()
        
        println("=== Specific Enhancement Tests ===\n")
        
        // Test time-specific enhancements
        println("1. Time-specific enhancements:")
        val timePrompts = listOf("Call later", "Meet sometime", "Buy soon")
        timePrompts.forEach { prompt ->
            val enhancements = enhancer.enhancePrompt(prompt)
            val timeEnhancements = enhancements.filter { it.enhancementType == PromptEnhancer.EnhancementType.TIME_SPECIFIC }
            println("  \"$prompt\" -> ${timeEnhancements.size} time enhancements")
            timeEnhancements.take(2).forEach { enhancement ->
                println("    \"${enhancement.enhancedPrompt}\"")
            }
        }
        
        // Test task completion enhancements
        println("\n2. Task completion enhancements:")
        val taskPrompts = listOf("Call", "Email", "Buy")
        taskPrompts.forEach { prompt ->
            val enhancements = enhancer.enhancePrompt(prompt)
            val taskEnhancements = enhancements.filter { it.enhancementType == PromptEnhancer.EnhancementType.TASK_COMPLETION }
            println("  \"$prompt\" -> ${taskEnhancements.size} task completions")
            taskEnhancements.take(2).forEach { enhancement ->
                println("    \"${enhancement.enhancedPrompt}\"")
            }
        }
        
        // Test context enhancements
        println("\n3. Context enhancements:")
        val contextPrompts = listOf("Meeting morning", "Call afternoon", "Buy evening")
        contextPrompts.forEach { prompt ->
            val enhancements = enhancer.enhancePrompt(prompt)
            val contextEnhancements = enhancements.filter { it.enhancementType == PromptEnhancer.EnhancementType.CONTEXT_ADDED }
            println("  \"$prompt\" -> ${contextEnhancements.size} context additions")
            contextEnhancements.take(2).forEach { enhancement ->
                println("    \"${enhancement.enhancedPrompt}\"")
            }
        }
        
        println("\n=== Specific Tests Complete ===")
    }
}