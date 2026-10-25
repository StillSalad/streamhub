package com.lollomarta.streamhub.integrations

import com.lollomarta.streamhub.domain.model.MediaType

/** Cosa sa il dispositivo. Implementato su Android da AndroidDeviceEnvironment. */
interface DeviceEnvironment {
    val platform: Platform
    fun isPackageLaunchable(packageName: String): Boolean
    /** Cerca tra le app avviabili quella la cui etichetta soddisfa [matcher]. */
    fun findLaunchablePackage(matcher: (label: String) -> Boolean): String?
    fun hasStore(): Boolean
}

data class ContentRequest(
    val type: MediaType,
    val tmdbId: Int,
    val title: String,
    val youtubeKey: String? = null,
)

sealed interface LaunchTarget {
    data class App(val packageName: String) : LaunchTarget
    data class Uri(val uri: String, val packageName: String?) : LaunchTarget
    data class Store(val packageName: String, val platform: Platform) : LaunchTarget
}

enum class InstallState(val label: String) { INSTALLED("App installata"), NOT_DETECTED("App non rilevata") }

/** Avvia fisicamente un target. Restituisce false se l'attività non esiste o il lancio fallisce. */
fun interface TargetStarter {
    fun start(target: LaunchTarget): Boolean
}

sealed interface LaunchPlan {
    data class Attempts(val service: StreamingService, val targets: List<LaunchTarget>, val firstIsContent: Boolean) : LaunchPlan
    data class NotInstalled(val service: StreamingService, val store: LaunchTarget.Store?) : LaunchPlan
}

sealed interface LaunchOutcome {
    data class Launched(val service: StreamingService, val target: LaunchTarget, val directContent: Boolean) : LaunchOutcome
    data class NotInstalled(val service: StreamingService, val store: LaunchTarget.Store?) : LaunchOutcome
    data class Failed(val service: StreamingService?, val reason: String) : LaunchOutcome
}

interface StreamingServiceAdapter {
    val service: StreamingService
    fun installedPackage(env: DeviceEnvironment): String?
    fun installState(env: DeviceEnvironment): InstallState =
        if (installedPackage(env) != null) InstallState.INSTALLED else InstallState.NOT_DETECTED
    fun appTarget(installedPackage: String?): LaunchTarget? = installedPackage?.let { LaunchTarget.App(it) }
    /** Link al singolo contenuto: null se il servizio non ne offre uno verificato. */
    fun contentTarget(request: ContentRequest, installedPackage: String?): LaunchTarget? = null
    fun fallback(env: DeviceEnvironment): LaunchTarget.Store?
}

open class DefaultStreamingServiceAdapter(final override val service: StreamingService) : StreamingServiceAdapter {
    override fun installedPackage(env: DeviceEnvironment): String? {
        service.candidatesFor(env.platform).firstOrNull { env.isPackageLaunchable(it.packageName) }
            ?.let { return it.packageName }
        if (service.launcherLabels.isEmpty()) return null
        return env.findLaunchablePackage { label -> ServiceMatcher.labelMatches(service, label) }
    }

    override fun fallback(env: DeviceEnvironment): LaunchTarget.Store? {
        if (!env.hasStore()) return null
        // Proponiamo lo store solo per package pubblicati negli store, mai per quelli non verificati.
        val candidate = service.candidatesFor(env.platform)
            .firstOrNull { it.verification != Verification.UNVERIFIED } ?: return null
        return LaunchTarget.Store(candidate.packageName, env.platform)
    }
}

class YouTubeAdapter(service: StreamingService) : DefaultStreamingServiceAdapter(service) {
    override fun contentTarget(request: ContentRequest, installedPackage: String?): LaunchTarget? {
        val key = request.youtubeKey?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) } ?: return null
        return LaunchTarget.Uri("https://www.youtube.com/watch?v=$key", installedPackage)
    }
}

interface StreamingLaunchManager {
    val adapters: List<StreamingServiceAdapter>
    fun adapter(serviceId: String): StreamingServiceAdapter?
    fun installState(serviceId: String): InstallState
    fun plan(serviceId: String, request: ContentRequest?): LaunchPlan?
    fun launch(serviceId: String, request: ContentRequest?, starter: TargetStarter): LaunchOutcome
}

class DefaultStreamingLaunchManager(
    private val env: DeviceEnvironment,
    services: List<StreamingService> = StreamingServices.ALL,
) : StreamingLaunchManager {

    override val adapters: List<StreamingServiceAdapter> = services.map {
        if (it.contentLinkSupport == ContentLinkSupport.YOUTUBE_VIDEO) YouTubeAdapter(it) else DefaultStreamingServiceAdapter(it)
    }

    override fun adapter(serviceId: String) = adapters.firstOrNull { it.service.id == serviceId }

    override fun installState(serviceId: String): InstallState =
        adapter(serviceId)?.installState(env) ?: InstallState.NOT_DETECTED

    override fun plan(serviceId: String, request: ContentRequest?): LaunchPlan? {
        val adapter = adapter(serviceId) ?: return null
        val installed = adapter.installedPackage(env)
        val content = request?.let { adapter.contentTarget(it, installed) }
        val app = adapter.appTarget(installed)
        val targets = listOfNotNull(content, app)
        return if (targets.isEmpty()) LaunchPlan.NotInstalled(adapter.service, adapter.fallback(env))
        else LaunchPlan.Attempts(adapter.service, targets, firstIsContent = content != null)
    }

    override fun launch(serviceId: String, request: ContentRequest?, starter: TargetStarter): LaunchOutcome {
        val plan = plan(serviceId, request) ?: return LaunchOutcome.Failed(null, "Servizio sconosciuto: $serviceId")
        return when (plan) {
            is LaunchPlan.NotInstalled -> LaunchOutcome.NotInstalled(plan.service, plan.store)
            is LaunchPlan.Attempts -> {
                plan.targets.forEachIndexed { index, target ->
                    val ok = try { starter.start(target) } catch (e: Exception) { false }
                    if (ok) return LaunchOutcome.Launched(plan.service, target, directContent = plan.firstIsContent && index == 0)
                }
                val installedApp = plan.targets.any { it is LaunchTarget.App }
                if (!installedApp) LaunchOutcome.NotInstalled(plan.service, adapter(serviceId)?.fallback(env))
                else LaunchOutcome.Failed(plan.service, "Impossibile avviare ${plan.service.displayName} su questo dispositivo.")
            }
        }
    }
}
