package edu.ismt.prabin.mealmate.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Data class representing a recipe in the application.
 */
@Serializable
data class Recipe(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val instructions: String = "",
    @SerialName("preparation_time")
    val prepTime: Int = 0,
    val servings: Int? = 1,
    @SerialName("image")
    val imageUrl: String = "",
    @SerialName("category")
    val foodType: String = "",
    @SerialName("created_at")
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Long = System.currentTimeMillis(),
    val ingredients: List<Ingredient> = emptyList()
)

object TimestampSerializer : KSerializer<Long> {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        val instant = Instant.ofEpochMilli(value)
        encoder.encodeString(formatter.format(instant))
    }

    override fun deserialize(decoder: Decoder): Long {
        return try {
            val str = decoder.decodeString()
            if (str.isEmpty()) {
                System.currentTimeMillis()
            } else {
                Instant.parse(str).toEpochMilli()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}