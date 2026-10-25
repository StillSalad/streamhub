package com.lollomarta.streamhub.integrations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Implementazione Android: usa SOLO le query dichiarate nel manifest (<queries>).
 * Le etichette del launcher vengono lette localmente e non lasciano mai il dispositivo.
 */
class AndroidDeviceEnvironment(context: Context) : DeviceEnvironment {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private var labelCache: List<Pair<String, String>>? = null

    /** Feature documentata da Amazon per riconoscere i dispositivi Fire TV. */
    override val platform: Platform =
        if (pm.hasSystemFeature("amazon.hardware.fire_tv")) Platform.FIRE_TV else Platform.ANDROID_TV

    fun launchIntentFor(packageName: String): Intent? = try {
        pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName)
    } catch (e: Exception) {
        null
    }

    override fun isPackageLaunchable(packageName: String): Boolean = launchIntentFor(packageName) != null

    override fun findLaunchablePackage(matcher: (label: String) -> Boolean): String? =
        launcherApps().firstOrNull { (_, label) -> matcher(label) }?.first

    override fun hasStore(): Boolean = when (platform) {
        Platform.FIRE_TV -> isInstalled("com.amazon.venezia")
        Platform.ANDROID_TV -> isInstalled("com.android.vending")
    }

    /** Da chiamare quando si torna nell'app: l'utente potrebbe aver installato qualcosa. */
    fun invalidate() { labelCache = null }

    private fun isInstalled(pkg: String) = try {
        pm.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun launcherApps(): List<Pair<String, String>> = labelCache ?: run {
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        categories.flatMap { cat ->
            runCatching {
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
                    .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            }.getOrDefault(emptyList())
        }.filter { it.first != appContext.packageName }
            .distinctBy { it.first }
            .also { labelCache = it }
    }
}

/** Converte i [LaunchTarget] in Intent e li avvia dal contesto dell'Activity corrente. */
class AndroidTargetStarter(
    private val activityContext: Context,
    private val env: AndroidDeviceEnvironment,
) : TargetStarter {

    override fun start(target: LaunchTarget): Boolean {
        val intent = when (target) {
            is LaunchTarget.App -> env.launchIntentFor(target.packageName) ?: return false
            is LaunchTarget.Uri -> Intent(Intent.ACTION_VIEW, Uri.parse(target.uri)).apply {
                target.packageName?.let { setPackage(it) }
            }
            is LaunchTarget.Store -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    if (target.platform == Platform.FIRE_TV) "amzn://apps/android?p=${target.packageName}"
                    else "market://details?id=${target.packageName}",
                ),
            )
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            activityContext.startActivity(intent)
            Log.d(TAG, "Avviato: ${target::class.simpleName}")
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Attività non trovata per ${target::class.simpleName}")
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Avvio non consentito")
            false
        }
    }

    private companion object { const val TAG = "StreamHubLaunch" }
}
