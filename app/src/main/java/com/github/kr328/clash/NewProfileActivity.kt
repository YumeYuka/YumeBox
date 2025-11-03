package com.github.kr328.clash

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kr328.clash.common.constants.Authorities
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.design.NewProfileDesign
import com.github.kr328.clash.design.model.File as ProfileFile
import com.github.kr328.clash.design.model.ProfileProvider
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.remote.IFetchObserver
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.util.copyContentTo
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.design.util.showExceptionToast
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R as DesignR

class NewProfileActivity : BaseActivity<NewProfileDesign>() {
    private val self: NewProfileActivity
        get() = this

    private var editingProfile: Profile? = null
    private var selectedImportUri: Uri? = null

    override suspend fun main() {
        val design = NewProfileDesign(this)

        val providers = queryProfileProviders()
        design.patchProviders(providers)

        setContentDesign(design)

        prepareInitialState(design, providers)

        while (isActive) {
            select {
                design.requests.onReceive { request ->
                    when (request) {
                        NewProfileDesign.Request.Cancel -> finish()
                        NewProfileDesign.Request.PopStack -> onBackPressedDispatcher.onBackPressed()
                        NewProfileDesign.Request.CreateEmptyFile -> Unit
                        NewProfileDesign.Request.CreateEmptyUrl -> Unit
                        is NewProfileDesign.Request.ExternalOpen -> handleExternalProvider(design, request.provider)
                        is NewProfileDesign.Request.ImportFile -> handleImportFile(design)
                        is NewProfileDesign.Request.SaveFile -> saveFileProfile(design, request)
                        is NewProfileDesign.Request.SaveUrl -> saveUrlProfile(design, request)
                    }
                }
            }
        }
    }

    private suspend fun handleCommitFailure(design: NewProfileDesign, uuid: UUID) {
        val profile = withProfile { queryByUUID(uuid) }
        if (profile != null) {
            editingProfile = profile
            withContext(Dispatchers.Main) {
                design.isEditMode = true
            }
        }
    }

    private suspend fun prepareInitialState(design: NewProfileDesign, providers: List<ProfileProvider>) {
        val target = intent.uuid ?: return
        val profile = withProfile { queryByUUID(target) }

        if (profile == null) {
            finish()
            return
        }

        editingProfile = profile

        withContext(Dispatchers.Main) {
            design.isEditMode = true
            design.providerIndex = when (profile.type) {
                Profile.Type.File -> providers.indexOfFirst { it is ProfileProvider.File }.takeIf { it >= 0 } ?: 0
                Profile.Type.Url -> providers.indexOfFirst { it is ProfileProvider.Url }.takeIf { it >= 0 } ?: 0
                Profile.Type.External -> providers.indexOfFirst { it is ProfileProvider.External }.takeIf { it >= 0 }
                    ?: 0
            }

            when (profile.type) {
                Profile.Type.File -> {
                    design.fileName = profile.name
                }

                Profile.Type.Url -> {
                    design.urlName = profile.name
                    design.url = profile.source
                    design.urlHoursIndex = resolveHourIndex(profile.interval)
                }

                Profile.Type.External -> {
                    design.urlName = profile.name
                }
            }
        }
    }

    private suspend fun handleExternalProvider(design: NewProfileDesign, provider: ProfileProvider.External) {
        val name = getString(DesignR.string.new_profile)
        val data = provider.get() ?: return
        val (uri, initialName) = data
        val displayName = initialName?.takeIf { it.isNotBlank() } ?: name
        val uuid = withProfile {
            create(Profile.Type.External, displayName, uri.toString())
        }
        try {
            commitWithProgress(design, uuid)
            setResult(Activity.RESULT_OK)
            finish()
        } catch (e: Exception) {
            handleCommitFailure(design, uuid)
        }
    }

    private suspend fun handleImportFile(design: NewProfileDesign) {
        val uri = startActivityForResult(
            ActivityResultContracts.GetContent(),
            "*/*"
        ) ?: return

        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val meta = resolveDocumentMeta(uri)
        selectedImportUri = uri
        updateImportSelection(design, meta)

        withContext(Dispatchers.Main) {
            if (design.fileName.isBlank()) {
                design.fileName = meta.name
            }
        }
    }

    private suspend fun saveFileProfile(
        design: NewProfileDesign,
        request: NewProfileDesign.Request.SaveFile
    ) {
        withSaving(design) {
            val name = request.name.trim().ifBlank { getString(DesignR.string.new_profile) }
            val existing = editingProfile

            if (existing == null) {
                val importUri = selectedImportUri
                if (importUri == null) {
                    design.showToast(DesignR.string.should_not_be_blank, ToastDuration.Short)
                    return@withSaving
                }

                val uuid = withProfile { create(Profile.Type.File, name) }
                copyConfigFromUri(uuid, importUri)
                try {
                    commitWithProgress(design, uuid)
                    clearImportSelection(design)
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    handleCommitFailure(design, uuid)
                }
            } else {
                withProfile { patch(existing.uuid, name, existing.source, existing.interval) }

                val needsCommit = !existing.imported || selectedImportUri != null

                selectedImportUri?.let { uri ->
                    copyConfigFromUri(existing.uuid, uri)
                }

                if (needsCommit) {
                    try {
                        commitWithProgress(design, existing.uuid)
                        clearImportSelection(design)
                        setResult(Activity.RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        return@withSaving
                    }
                } else {
                    clearImportSelection(design)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private suspend fun saveUrlProfile(
        design: NewProfileDesign,
        request: NewProfileDesign.Request.SaveUrl
    ) {
        withSaving(design) {
            val name = request.name.trim().ifBlank { getString(DesignR.string.new_profile) }
            val url = request.url.trim()

            if (url.isBlank()) {
                design.showToast(DesignR.string.should_not_be_blank, ToastDuration.Short)
                return@withSaving
            }

            val interval = if (request.intervalHours <= 0) 0L else TimeUnit.HOURS.toMillis(request.intervalHours)
            val existing = editingProfile

            if (existing == null) {
                val uuid = withProfile { create(Profile.Type.Url, name, url) }
                if (interval > 0) {
                    withProfile { patch(uuid, name, url, interval) }
                }
                try {
                    commitWithProgress(design, uuid)
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    handleCommitFailure(design, uuid)
                }
            } else {
                withProfile { patch(existing.uuid, name, url, interval) }

                if (!existing.imported || existing.source != url) {
                    try {
                        commitWithProgress(design, existing.uuid)
                        setResult(Activity.RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        return@withSaving
                    }
                } else {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private suspend fun withSaving(
        design: NewProfileDesign,
        block: suspend () -> Unit
    ) {
        withContext(Dispatchers.Main) { design.isSaving = true }
        try {
            block()
        } finally {
            withContext(Dispatchers.Main) { design.isSaving = false }
        }
    }

    private suspend fun updateImportSelection(design: NewProfileDesign, meta: ProfileFile) {
        withContext(Dispatchers.Main) {
            design.swapFiles(listOf(meta))
        }
    }

    private suspend fun clearImportSelection(design: NewProfileDesign) {
        selectedImportUri = null
        withContext(Dispatchers.Main) {
            design.swapFiles(emptyList())
        }
    }

    private suspend fun copyConfigFromUri(uuid: UUID, source: Uri) {
        val documentId = "$uuid/$CONFIG_DOCUMENT_ID"
        val target = DocumentsContract.buildDocumentUri(Authorities.FILES_PROVIDER, documentId)
        contentResolver.copyContentTo(source, target)
    }

    private suspend fun resolveDocumentMeta(uri: Uri): ProfileFile = withContext(Dispatchers.IO) {
        var name = uri.fileName ?: "config.yaml"
        var size = 0L

        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        name = cursor.getString(nameIdx)
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }

        ProfileFile(
            id = uri.toString(),
            name = name,
            size = size,
            lastModified = System.currentTimeMillis(),
            isDirectory = false
        )
    }

    private suspend fun commitWithProgress(design: NewProfileDesign, uuid: UUID) {
        val observer = IFetchObserver { status ->
            val title = when (status.action) {
                FetchStatus.Action.FetchConfiguration -> getString(
                    DesignR.string.format_fetching_configuration,
                    status.args.getOrNull(0) ?: ""
                )

                FetchStatus.Action.FetchProviders -> getString(
                    DesignR.string.format_fetching_provider,
                    status.args.getOrNull(0) ?: ""
                )

                FetchStatus.Action.Verifying -> getString(DesignR.string.verifying)
            }

            launch(Dispatchers.Main) {
                design.updateDownload(title, status.progress, status.max)
            }
        }

        withContext(Dispatchers.Main) {
            design.showDownload(MLang.loading, 0, 100)
        }

        try {
            withProfile { commit(uuid, observer) }
        } catch (e: Exception) {
            design.showExceptionToast(e)
            throw e
        } finally {
            withContext(Dispatchers.Main) {
                design.hideDownload()
            }
        }
    }

    private fun resolveHourIndex(interval: Long): Int {
        val hours = TimeUnit.MILLISECONDS.toHours(interval)
        return HOUR_OPTIONS.indexOfFirst { it == hours }.takeIf { it >= 0 } ?: 0
    }

    private fun launchAppDetailed(provider: ProfileProvider.External) {
        val data = Uri.fromParts(
            "package",
            provider.intent.component?.packageName ?: return,
            null
        )

        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(data))
    }

    private suspend fun ProfileProvider.External.get(): Pair<Uri, String?>? {
        val result = startActivityForResult(
            ActivityResultContracts.StartActivityForResult(),
            intent
        )

        if (result.resultCode != RESULT_OK)
            return null

        val uri = result.data?.data
        val name = result.data?.getStringExtra(Intents.EXTRA_NAME)

        if (uri != null) {
            return uri to name
        }

        return null
    }

    private suspend fun queryProfileProviders(): List<ProfileProvider> {
        return withContext(Dispatchers.IO) {
            val providers = packageManager.queryIntentActivities(
                Intent(Intents.ACTION_PROVIDE_URL),
                0
            ).map {
                val activity = it.activityInfo

                val name = activity.applicationInfo.loadLabel(packageManager)
                val summary = activity.loadLabel(packageManager)
                val icon = activity.loadIcon(packageManager)
                val intent = Intent(Intents.ACTION_PROVIDE_URL)
                    .setComponent(
                        ComponentName(
                            activity.packageName,
                            activity.name
                        )
                    )

                ProfileProvider.External(name.toString(), summary.toString(), icon, intent)
            }

            listOf(ProfileProvider.File(self), ProfileProvider.Url(self)) + providers
        }
    }

    private companion object {
        private const val CONFIG_DOCUMENT_ID = "config.yaml"
        private val HOUR_OPTIONS = listOf(0L, 1L, 3L, 6L, 12L, 24L)
    }
}
