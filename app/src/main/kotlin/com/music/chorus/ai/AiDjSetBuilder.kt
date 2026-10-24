package pushkar.chorus.music.ai

import kotlin.math.abs

object AiDjSetBuilder {

    data class CamelotKey(val number: Int, val isMinor: Boolean) {
        val code: String get() = "$number${if (isMinor) "A" else "B"}"

        fun compatibilityWith(nextKey: CamelotKey): Float {
            if (this == nextKey) return 1.0f

            if (this.number == nextKey.number && this.isMinor != nextKey.isMinor) {
                return 0.85f
            }
            if (this.isMinor == nextKey.isMinor) {
                val diff = abs(this.number - nextKey.number)
                if (diff == 1 || diff == 11) {
                    return 0.90f
                }

                if ((this.number + 7 - 1) % 12 + 1 == nextKey.number) {
                    return 0.75f
                }
            }

            return 0.40f
        }

        companion object {
            private val KeyToCamelot = mapOf(
                "Abm" to CamelotKey(1, true), "B" to CamelotKey(1, false),
                "Ebm" to CamelotKey(2, true), "F#" to CamelotKey(2, false), "Gb" to CamelotKey(2, false),
                "Bbm" to CamelotKey(3, true), "Db" to CamelotKey(3, false), "C#" to CamelotKey(3, false),
                "Fm" to CamelotKey(4, true), "Ab" to CamelotKey(4, false),
                "Cm" to CamelotKey(5, true), "Eb" to CamelotKey(5, false),
                "Gm" to CamelotKey(6, true), "Bb" to CamelotKey(6, false),
                "Dm" to CamelotKey(7, true), "F" to CamelotKey(7, false),
                "Am" to CamelotKey(8, true), "C" to CamelotKey(8, false),
                "Em" to CamelotKey(9, true), "G" to CamelotKey(9, false),
                "Bm" to CamelotKey(10, true), "D" to CamelotKey(10, false),
                "F#m" to CamelotKey(11, true), "A" to CamelotKey(11, false),
                "C#m" to CamelotKey(12, true), "E" to CamelotKey(12, false),
            )

            fun fromCode(code: String): CamelotKey? {
                val trimmed = code.trim().uppercase()
                if (trimmed.length < 2) return null
                val letter = trimmed.last()
                val isMinor = when (letter) {
                    'A' -> true
                    'B' -> false
                    else -> return null
                }
                val num = trimmed.dropLast(1).toIntOrNull() ?: return null
                if (num in 1..12) return CamelotKey(num, isMinor)
                return null
            }

            fun parsePitchAndMode(pitchName: String, isMinor: Boolean): CamelotKey? {
                val keyStr = if (isMinor) "${pitchName}m" else pitchName
                return KeyToCamelot[keyStr]
            }
        }
    }

    data class TrackMetadata(
        val id: String,
        val title: String,
        val artist: String,
        val bpm: Float = 120f,
        val camelotKey: CamelotKey? = null,
        val energy: Float = 0.5f
    )

    enum class DjSetCurve {
        RAMP_UP,
        PEAK_DROP,
        ROLLERCOASTER,
        STEADY_GROOVE
    }

    data class TransitionScore(
        val fromTrack: TrackMetadata,
        val toTrack: TrackMetadata,
        val harmonicScore: Float,
        val tempoScore: Float,
        val energyScore: Float,
        val totalScore: Float
    )

    fun evaluateTransition(
        trackA: TrackMetadata,
        trackB: TrackMetadata,
        targetEnergyDelta: Float = 0f
    ): TransitionScore {
        val harmonicScore = if (trackA.camelotKey != null && trackB.camelotKey != null) {
            trackA.camelotKey.compatibilityWith(trackB.camelotKey)
        } else {
            0.6f
        }

        val bpmDiff = abs(trackA.bpm - trackB.bpm)
        val tempoScore = when {
            bpmDiff <= 3f -> 1.0f
            bpmDiff <= 6f -> 0.85f
            bpmDiff <= 10f -> 0.70f
            bpmDiff <= 15f -> 0.50f
            else -> 0.20f
        }

        val actualDelta = trackB.energy - trackA.energy
        val energyDiff = abs(actualDelta - targetEnergyDelta)
        val energyScore = (1.0f - energyDiff).coerceIn(0f, 1f)
        val totalScore = (harmonicScore * 0.45f) + (tempoScore * 0.35f) + (energyScore * 0.20f)

        return TransitionScore(
            fromTrack = trackA,
            toTrack = trackB,
            harmonicScore = harmonicScore,
            tempoScore = tempoScore,
            energyScore = energyScore,
            totalScore = totalScore
        )
    }

    fun sequenceDjSet(
        tracks: List<TrackMetadata>,
        curve: DjSetCurve = DjSetCurve.RAMP_UP
    ): List<TrackMetadata> {
        if (tracks.size <= 2) return tracks

        val remaining = tracks.toMutableList()
        val result = mutableListOf<TrackMetadata>()
        val startTrack = when (curve) {
            DjSetCurve.RAMP_UP -> remaining.minByOrNull { it.energy } ?: remaining.first()
            DjSetCurve.PEAK_DROP -> remaining.maxByOrNull { it.energy } ?: remaining.first()
            else -> remaining.first()
        }
        result.add(startTrack)
        remaining.remove(startTrack)

        while (remaining.isNotEmpty()) {
            val current = result.last()
            val progressFraction = result.size.toFloat() / tracks.size.toFloat()

            val desiredEnergyDelta = when (curve) {
                DjSetCurve.RAMP_UP -> 0.15f
                DjSetCurve.PEAK_DROP -> -0.15f
                DjSetCurve.ROLLERCOASTER -> if ((progressFraction * 4).toInt() % 2 == 0) 0.15f else -0.15f
                DjSetCurve.STEADY_GROOVE -> 0f
            }

            val bestNext = remaining.maxByOrNull { candidate ->
                evaluateTransition(current, candidate, desiredEnergyDelta).totalScore
            } ?: remaining.first()

            result.add(bestNext)
            remaining.remove(bestNext)
        }

        return result
    }
}
