package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.*
import org.junit.Test

class ActivityTypeMetricsTest {
    @Test fun strengthAndYogaDoNotExposeGpsDriftAsDistanceOrSpeed() {
        assertFalse(activityUsesDistance(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING))
        assertFalse(activityUsesDistance(ExerciseSessionRecord.EXERCISE_TYPE_YOGA))
    }
    @Test fun cyclingRunningAndTheirIndoorVariantsKeepDistanceMetrics() {
        assertTrue(activityUsesDistance(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertTrue(activityUsesDistance(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
        assertTrue(activityUsesDistance(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
    }
}
