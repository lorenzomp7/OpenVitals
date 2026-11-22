package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Active calories that count the steps. */
class StepEnergyTest {

    private val perStep = activeKcalPerStep(weightKg = 70.0, heightCm = 180.0)

    @Test fun `a step costs about four hundredths of a kilocalorie for an adult`() {
        // 0.415 x 1.80 m = 0.747 m a step; 0.5 kcal/kg/km x 70 kg x 0.000747 km.
        assertEquals(0.0261, perStep, 0.0005)
    }

    @Test fun `a day of walking without recorded energy is not zero`() {
        val buckets = List(4) { ActivityEnergyBucket(steps = 2_000, activeKcal = 0.0) }
        assertEquals(8_000 * perStep, activeCaloriesWithSteps(buckets, perStep)!!, 0.01)
    }

    @Test fun `a workout's recorded energy is not added to its own steps`() {
        val run = ActivityEnergyBucket(steps = 2_500, activeKcal = 180.0)
        val walk = ActivityEnergyBucket(steps = 1_000, activeKcal = 0.0)
        assertEquals(180.0 + 1_000 * perStep, activeCaloriesWithSteps(listOf(run, walk), perStep)!!, 0.01)
    }

    @Test fun `nothing recorded is no value`() {
        assertNull(activeCaloriesWithSteps(listOf(ActivityEnergyBucket(0, 0.0)), perStep))
    }

    @Test fun `an unset profile falls back to an average adult`() {
        assertEquals(activeKcalPerStep(70.0, 175.0), activeKcalPerStep(null, null), 1e-9)
    }
}
