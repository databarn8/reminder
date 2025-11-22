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
    val sound: SoundConfig = SoundConfig(),
    val series: AlertSeries = AlertSeries()
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
                    intensity = VibrationIntensity.LIGHT,
                    seriesCount = 1,
                    seriesInterval = 1000
                ),
                sound = SoundConfig(
                    enabled = false,
                    type = SoundType.GENTLE,
                    volume = 0.3f,
                    seriesCount = 1,
                    seriesInterval = 2000
                ),
                series = AlertSeries(
                    enabled = false,
                    maxAttempts = 1,
                    intervalMinutes = 0,
                    escalationEnabled = false,
                    stopOnAcknowledge = true
                )
            )
        }
        
        fun getMediumLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.NOTIFICATION_VIBRATION,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.DOUBLE,
                    intensity = VibrationIntensity.MEDIUM,
                    seriesCount = 3,
                    seriesInterval = 5000
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.CHIME,
                    volume = 0.8f,
                    seriesCount = 3,
                    seriesInterval = 5000
                ),
                series = AlertSeries(
                    enabled = true,
                    maxAttempts = 3,
                    intervalMinutes = 5,
                    escalationEnabled = false,
                    stopOnAcknowledge = true
                )
            )
        }
        
        fun getHighLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.NOTIFICATION_SOUND,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.TRIPLE,
                    intensity = VibrationIntensity.STRONG,
                    seriesCount = 5,
                    seriesInterval = 2000
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.ALARM,
                    volume = 1.0f,
                    seriesCount = 5,
                    seriesInterval = 2000
                ),
                series = AlertSeries(
                    enabled = true,
                    maxAttempts = 5,
                    intervalMinutes = 2,
                    escalationEnabled = true,
                    stopOnAcknowledge = true
                )
            )
        }
        
        fun getUrgentLevelDefaults(): AlertConfig {
            return AlertConfig(
                alertType = AlertType.FULL_ALERT,
                vibration = VibrationConfig(
                    enabled = true,
                    pattern = VibrationPattern.PULSE,
                    intensity = VibrationIntensity.STRONG,
                    seriesCount = 99, // Effectively unlimited
                    seriesInterval = 60000 // Every minute
                ),
                sound = SoundConfig(
                    enabled = true,
                    type = SoundType.ALARM,
                    volume = 1.0f,
                    seriesCount = 99, // Effectively unlimited
                    seriesInterval = 60000 // Every minute
                ),
                series = AlertSeries(
                    enabled = true,
                    maxAttempts = 99, // Effectively unlimited
                    intervalMinutes = 1,
                    escalationEnabled = true,
                    stopOnAcknowledge = true
                )
            )
        }
    }
}

// Alert level system for simplified user experience
@Serializable
data class AlertLevelConfig(
    val lowLevel: AlertConfig = AlertConfig.getLowLevelDefaults(),
    val mediumLevel: AlertConfig = AlertConfig.getMediumLevelDefaults(),
    val highLevel: AlertConfig = AlertConfig.getHighLevelDefaults(),
    val urgentLevel: AlertConfig = AlertConfig.getUrgentLevelDefaults(),
    val customProfiles: Map<String, AlertConfig> = emptyMap()
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
    LOW, MEDIUM, HIGH, URGENT, CUSTOM
}

// Enhanced alert level option for dropdowns
@Serializable
data class AlertLevelOption(
    val displayName: String,
    val level: AlertLevel,
    val customProfileName: String? = null,
    val config: AlertConfig? = null
) {
    companion object {
        fun getBuiltInOptions(): List<AlertLevelOption> {
            return listOf(
                AlertLevelOption("Low", AlertLevel.LOW),
                AlertLevelOption("Medium", AlertLevel.MEDIUM),
                AlertLevelOption("High", AlertLevel.HIGH),
                AlertLevelOption("Urgent", AlertLevel.URGENT)
            )
        }
        
        fun getCustomOptions(customProfiles: Map<String, AlertConfig>): List<AlertLevelOption> {
            return customProfiles.map { (name, config) ->
                AlertLevelOption(name, AlertLevel.CUSTOM, name, config)
            }
        }
        
        fun getAllOptions(customProfiles: Map<String, AlertConfig>): List<AlertLevelOption> {
            return getBuiltInOptions() + getCustomOptions(customProfiles)
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
    val intensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val seriesCount: Int = 1,
    val seriesInterval: Int = 1000 // ms between series
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
    val seriesCount: Int = 1,
    val seriesInterval: Int = 2000, // ms between series
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

@Serializable
data class AlertSeries(
    val enabled: Boolean = false,
    val maxAttempts: Int = 3,
    val intervalMinutes: Int = 5,
    val escalationEnabled: Boolean = true,
    val stopOnAcknowledge: Boolean = true
)

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