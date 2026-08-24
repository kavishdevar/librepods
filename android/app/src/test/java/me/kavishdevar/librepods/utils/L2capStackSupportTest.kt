package me.kavishdevar.librepods.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class L2capStackSupportTest {

    @Test
    fun samsungOneUi9OnSdk36IsAllowed() {
        assertTrue(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "samsung",
                androidRelease = "16",
                buildId = "BP4A.251205.006",
                oneUiVersion = 90_000,
                semPlatformInt = 180_000
            )
        )
    }

    @Test
    fun samsungOneUi85OnSdk36IsDeniedEvenIfSemLooksLike9() {
        assertFalse(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "samsung",
                androidRelease = "16",
                buildId = "BP4A.251205.006",
                oneUiVersion = 85_000,
                semPlatformInt = 180_000
            )
        )
    }

    @Test
    fun samsungUnknownOneUiUsesSem180000() {
        assertTrue(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "samsung",
                androidRelease = "16",
                buildId = "BP4A.251205.006",
                oneUiVersion = null,
                semPlatformInt = 180_000
            )
        )
    }

    @Test
    fun samsungUnknownOneUiLowSemIsDenied() {
        assertFalse(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "samsung",
                androidRelease = "16",
                buildId = "BP4A.251205.006",
                oneUiVersion = null,
                semPlatformInt = 170_500
            )
        )
    }

    @Test
    fun pixelQpr3IsAllowed() {
        assertTrue(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "Google",
                androidRelease = "16",
                buildId = "CP1A.260305.018",
                oneUiVersion = null,
                semPlatformInt = null
            )
        )
    }

    @Test
    fun oppoOxygenOs16IsAllowed() {
        assertTrue(
            hasFixedL2capStack(
                sdkInt = 36,
                manufacturer = "OnePlus",
                androidRelease = "16",
                buildId = "CP1A.foo",
                oneUiVersion = null,
                semPlatformInt = null
            )
        )
    }

    @Test
    fun sdk37IsAlwaysAllowed() {
        assertTrue(
            hasFixedL2capStack(
                sdkInt = 37,
                manufacturer = "samsung",
                androidRelease = "17",
                buildId = "anything",
                oneUiVersion = 85_000,
                semPlatformInt = 170_000
            )
        )
    }

    @Test
    fun oneUiPropertyWinsOverSem() {
        assertFalse(isSamsungOneUi9OrNewer(oneUiVersion = 85_000, semPlatformInt = 180_000))
        assertTrue(isSamsungOneUi9OrNewer(oneUiVersion = 90_000, semPlatformInt = 170_000))
        assertTrue(isSamsungOneUi9OrNewer(oneUiVersion = null, semPlatformInt = 180_000))
        assertFalse(isSamsungOneUi9OrNewer(oneUiVersion = null, semPlatformInt = null))
    }
}
