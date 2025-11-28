package com.reminder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalDate

// Enhanced alert configuration data structures
@Serializable
data class AlertConfig(
    val alertType: AlertType = AlertType.NOTIFICATION_ONLY,
    val vibration: VibrationConfig = VibrationConfig(),
    val sound: SoundConfig = SoundConfig()
) {
    companion object {
        fun fromJson(json: String): AlertConfig {
            return try {
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.decodeFromString(
                    AlertConfig.serializer(),
                    json
                )
            } catch (e: Exception) {
                android.util.Log.e("AlertConfig", "Error parsing JSON: ${e.message}")
                AlertConfig() // Return default if parsing fails
            }
        }
        
        fun getLowLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.NOTIFICATION_ONLY,
                vibration = VibrationConfig(
                    enabled = false,
                    pattern = VibrationPattern.SINGLE,
                    intensity = VibrationIntensity.LIGHT
                ),
                sound = SoundConfig(
                    enabled = false,
                    type = SoundType.GENTLE,
                    volume = 0.3f,
                    repeatCount = 1
                )
            )
        }
        
        fun getMediumLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.NOTIFICATION_VIBRATION,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.DOUBLE,
                    intensity = VibrationIntensity.MEDIUM
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.CHIME,
                    volume = 0.8f,
                    repeatCount = 3
                )
            )
        }
        
        fun getHighLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.NOTIFICATION_SOUND,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.TRIPLE,
                    intensity = VibrationIntensity.STRONG
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.ALARM,
                    volume = 1.0f,
                    repeatCount = 5
                )
            )
        }
        
        fun getUrgentLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.FULL_ALERT,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.PULSE,
                    intensity = VibrationIntensity.STRONG
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.ALARM,
                    volume = 1.0f,
                    repeatCount = 8
                )
            )
        }
    }
}

// Alert level system for simplified user experience
@Serializable
data class AlertLevelConfig(
    val lowLevel: AlertConfig = AlertConfig.getLowLevelDefaults(),
    val highLevel: AlertConfig = AlertConfig.getHighLevelDefaults(),
    val urgentLevel: AlertConfig = AlertConfig.getUrgentLevelDefaults()
) {
    companion object {
        fun fromJson(json: String): AlertLevelConfig {
            return try {
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.decodeFromString(
                    AlertLevelConfig.serializer(),
                    json
                )
            } catch (e: Exception) {
                android.util.Log.e("AlertLevelConfig", "Error parsing JSON: ${e.message}")
                AlertLevelConfig() // Return default if parsing fails
            }
        }
        
        fun toJson(config: AlertLevelConfig): String {
            return try {
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.encodeToString(
                    AlertLevelConfig.serializer(),
                    config
                )
            } catch (e: Exception) {
                android.util.Log.e("AlertLevelConfig", "Error encoding JSON: ${e.message}")
                // Return JSON for default config if encoding fails
                kotlinx.serialization.json.Json.encodeToString(AlertLevelConfig())
            }
        }
    }
}

@Serializable
enum class AlertLevel {
    LOW, HIGH, URGENT
}

// Enhanced alert level option for dropdowns
@Serializable
data class AlertLevelOption(
    val displayName: String,
    val level: AlertLevel,
    val config: AlertConfig? = null
) {
    companion object {
        fun getBuiltInOptions(): List<AlertLevelOption> {
            return listOf(
                AlertLevelOption("Low", AlertLevel.LOW),
                AlertLevelOption("High", AlertLevel.HIGH),
                AlertLevelOption("Urgent", AlertLevel.URGENT)
            )
        }
        
        fun getAllOptions(): List<AlertLevelOption> {
            return getBuiltInOptions()
        }
    }
}

@Serializable
enum class AlertType {
    NOTIFICATION_ONLY,
    NOTIFICATION_VIBRATION,
    NOTIFICATION_SOUND,
    FULL_ALERT
}

@Serializable
data class VibrationConfig(
    val enabled: Boolean = true,
    val pattern: VibrationPattern = VibrationPattern.SINGLE,
    val intensity: VibrationIntensity = VibrationIntensity.MEDIUM
)

@Serializable
enum class VibrationPattern {
    SINGLE,      // One short vibration
    DOUBLE,       // Two short vibrations
    TRIPLE,       // Three short vibrations
    LONG,         // One long vibration
    PULSE,        // Pulsing pattern
    CUSTOM        // User-defined pattern
}

@Serializable
enum class VibrationIntensity {
    LIGHT, MEDIUM, STRONG
}

@Serializable
data class SoundConfig(
    val enabled: Boolean = true,
    val type: SoundType = SoundType.CHIME,
    val volume: Float = 0.8f,
    val repeatCount: Int = 1, // How many times to repeat the sound (1-10)
    val customSoundUri: String? = null // Future feature
)

@Serializable
enum class SoundType {
    CHIME,        // System default notification sound
    ALARM,        // System alarm sound
    GENTLE,       // Soft notification sound
    URGENT,       // Loud attention sound
    CUSTOM         // User-selected sound (future)
}


// Repeat pattern data structure
@Serializable
data class RepeatPattern(
    val type: RepeatType = RepeatType.NONE,
    val interval: Int = 1, // Every X days/weeks/months
    val daysOfWeek: List<java.time.DayOfWeek>? = null, // For weekly repeats
    val dayOfMonth: Int? = null, // For monthly repeats
    @Serializable(with = LocalDateSerializer::class)
    val endDate: java.time.LocalDate? = null // Optional end date
) {
    companion object {
        fun fromJson(json: String): RepeatPattern {
            return kotlinx.serialization.json.Json.decodeFromString(
                RepeatPattern.serializer(),
                json
            )
        }
        
        fun toJson(pattern: RepeatPattern): String {
            return kotlinx.serialization.json.Json.encodeToString(
                RepeatPattern.serializer(),
                pattern
            )
        }
    }
}

// Helper function for RepeatPattern serialization
fun RepeatPattern.toJson(): String {
    return kotlinx.serialization.json.Json.encodeToString(
        RepeatPattern.serializer(),
        this
    )
}

@Serializable
enum class RepeatType {
    NONE, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

// LocalDate serializer for kotlinx.serialization
object LocalDateSerializer : kotlinx.serialization.KSerializer<LocalDate> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDate", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

// Helper functions for serialization
fun AlertConfig.toJson(): String {
    return try {
        kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.encodeToString(
            AlertConfig.serializer(),
            this
        )
    } catch (e: Exception) {
        android.util.Log.e("AlertConfig", "Error encoding AlertConfig to JSON: ${e.message}")
        // Return JSON for default config if encoding fails
        kotlinx.serialization.json.Json.encodeToString(AlertConfig())
    }
}