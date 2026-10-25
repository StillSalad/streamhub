package com.lollomarta.streamhub

import com.lollomarta.streamhub.domain.model.MediaType
import com.lollomarta.streamhub.integrations.ContentRequest
import com.lollomarta.streamhub.integrations.DefaultStreamingLaunchManager
import com.lollomarta.streamhub.integrations.DeviceEnvironment
import com.lollomarta.streamhub.integrations.InstallState
import com.lollomarta.streamhub.integrations.LaunchOutcome
import com.lollomarta.streamhub.integrations.LaunchPlan
import com.lollomarta.streamhub.integrations.LaunchTarget
import com.lollomarta.streamhub.integrations.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEnv(
    override val platform: Platform = Platform.ANDROID_TV,
    val installed: Set<String> = emptySet(),
    val labels: Map<String, String> = emptyMap(),
    val store: Boolean = true,
) : DeviceEnvironment {
    override fun isPackageLaunchable(packageName: String) = packageName in installed
    override fun findLaunchablePackage(matcher: (String) -> Boolean) = labels.entries.firstOrNull { matcher(it.value) }?.key
    override fun hasStore() = store
}

class StreamingLaunchManagerTest {
    private val request = ContentRequest(MediaType.MOVIE, 1, "Film", youtubeKey = "dQw4w9WgXcQ")

    @Test fun installedAppIsLaunched() {
        val m = DefaultStreamingLaunchManager(FakeEnv(installed = setOf("com.netflix.ninja")))
        val started = mutableListOf<LaunchTarget>()
        val out = m.launch("netflix", request) { started += it; true }
        assertTrue(out is LaunchOutcome.Launched)
        assertEquals(LaunchTarget.App("com.netflix.ninja"), started.single())
        assertEquals(false, (out as LaunchOutcome.Launched).directContent)
    }

    @Test fun notInstalledOffersStoreFallback() {
        val m = DefaultStreamingLaunchManager(FakeEnv())
        val out = m.launch("disney_plus", request) { error("non deve essere avviato") }
        assertTrue(out is LaunchOutcome.NotInstalled)
        assertEquals("com.disney.disneyplus", (out as LaunchOutcome.NotInstalled).store?.packageName)
    }

    @Test fun unverifiedPackagesAreNeverSuggestedInStore() {
        val m = DefaultStreamingLaunchManager(FakeEnv())
        val plan = m.plan("now", null) as LaunchPlan.NotInstalled
        assertNull(plan.store)
    }

    @Test fun noStoreOnDeviceMeansNoStoreFallback() {
        val m = DefaultStreamingLaunchManager(FakeEnv(store = false))
        assertNull((m.plan("netflix", null) as LaunchPlan.NotInstalled).store)
    }

    @Test fun appFoundByLauncherLabelWhenPackageUnknown() {
        val m = DefaultStreamingLaunchManager(FakeEnv(labels = mapOf("it.example.raiplay" to "RaiPlay")))
        assertEquals(InstallState.INSTALLED, m.installState("raiplay"))
        val plan = m.plan("raiplay", request) as LaunchPlan.Attempts
        assertEquals(LaunchTarget.App("it.example.raiplay"), plan.targets.single())
    }

    @Test fun fireTvUsesFireTvPackages() {
        val m = DefaultStreamingLaunchManager(FakeEnv(platform = Platform.FIRE_TV, installed = setOf("com.apple.atve.amazon.appletv")))
        val plan = m.plan("apple_tv", null) as LaunchPlan.Attempts
        assertEquals(LaunchTarget.App("com.apple.atve.amazon.appletv"), plan.targets.single())
    }

    @Test fun youtubeTrailerTriesDirectLinkThenApp() {
        val m = DefaultStreamingLaunchManager(FakeEnv(installed = setOf("com.google.android.youtube.tv")))
        val plan = m.plan("youtube", request) as LaunchPlan.Attempts
        assertTrue(plan.firstIsContent)
        assertEquals(2, plan.targets.size)
        val started = mutableListOf<LaunchTarget>()
        val out = m.launch("youtube", request) { started += it; it is LaunchTarget.App }
        assertEquals(2, started.size)
        assertEquals(false, (out as LaunchOutcome.Launched).directContent)
    }

    @Test fun invalidYoutubeKeyIsIgnored() {
        val m = DefaultStreamingLaunchManager(FakeEnv(installed = setOf("com.google.android.youtube.tv")))
        val plan = m.plan("youtube", request.copy(youtubeKey = "bad key/../")) as LaunchPlan.Attempts
        assertEquals(false, plan.firstIsContent)
    }

    @Test fun failedStartIsReportedNotCrashed() {
        val m = DefaultStreamingLaunchManager(FakeEnv(installed = setOf("com.netflix.ninja")))
        val out = m.launch("netflix", null) { throw IllegalStateException("boom") }
        assertTrue(out is LaunchOutcome.Failed)
    }

    @Test fun unknownServiceFails() {
        val m = DefaultStreamingLaunchManager(FakeEnv())
        assertTrue(m.launch("sconosciuto", null) { true } is LaunchOutcome.Failed)
    }
}
