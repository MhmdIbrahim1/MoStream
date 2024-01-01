package com.lagradost.cloudstream3.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.AcraApplication.Companion.getActivity
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.plugins.PLUGINS_KEY
import com.lagradost.cloudstream3.plugins.PLUGINS_KEY_LOCAL
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Companion.ANILIST_CACHED_LIST
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Companion.ANILIST_TOKEN_KEY
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Companion.ANILIST_UNIXTIME_KEY
import com.lagradost.cloudstream3.syncproviders.providers.AniListApi.Companion.ANILIST_USER_KEY
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_CACHED_LIST
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_REFRESH_TOKEN_KEY
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_TOKEN_KEY
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_UNIXTIME_KEY
import com.lagradost.cloudstream3.syncproviders.providers.MALApi.Companion.MAL_USER_KEY
import com.lagradost.cloudstream3.syncproviders.providers.OpenSubtitlesApi.Companion.OPEN_SUBTITLES_USER_KEY
import com.lagradost.cloudstream3.ui.result.txt
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.DataStore.getDefaultSharedPrefs
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import com.lagradost.cloudstream3.utils.DataStore.mapper
import com.lagradost.cloudstream3.utils.DataStore.setKeyRaw
import com.lagradost.cloudstream3.utils.UIHelper.checkWrite
import com.lagradost.cloudstream3.utils.UIHelper.requestRW
import kotlinx.coroutines.tasks.await
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.ListenerRegistration


object BackupUtils {


    //
    private const val FIRESTORE_USERS_COLLECTION_NAME = "users"
    private const val FIRESTORE_BACKUP_COLLECTION_NAME = "backups"
    private val auth = FirebaseAuth.getInstance()
    private val firestoreUsers: CollectionReference by lazy {
        Firebase.firestore.collection(FIRESTORE_USERS_COLLECTION_NAME)
    }
    private val firestore: CollectionReference by lazy {
        firestoreUsers.document(auth.uid!!).collection(FIRESTORE_BACKUP_COLLECTION_NAME)
    }

    /**
     * No sensitive or breaking data in the backup
     * */
    private val nonTransferableKeys = listOf(
        // When sharing backup we do not want to transfer what is essentially the password
        ANILIST_TOKEN_KEY,
        ANILIST_CACHED_LIST,
        ANILIST_UNIXTIME_KEY,
        ANILIST_USER_KEY,
        MAL_TOKEN_KEY,
        MAL_REFRESH_TOKEN_KEY,
        MAL_CACHED_LIST,
        MAL_UNIXTIME_KEY,
        MAL_USER_KEY,

        // The plugins themselves are not backed up
        PLUGINS_KEY,
        PLUGINS_KEY_LOCAL,

        OPEN_SUBTITLES_USER_KEY,
        "nginx_user", // Nginx user key
    )

    /** false if blacklisted key */
    private fun String.isTransferable(): Boolean {
        return !nonTransferableKeys.contains(this)
    }

    private var restoreFileSelector: ActivityResultLauncher<Array<String>>? = null

    // Kinda hack, but I couldn't think of a better way
    data class BackupVars(
        @JsonProperty("_Bool") val _Bool: Map<String, Boolean>?,
        @JsonProperty("_Int") val _Int: Map<String, Int>?,
        @JsonProperty("_String") val _String: Map<String, String>?,
        @JsonProperty("_Float") val _Float: Map<String, Float>?,
        @JsonProperty("_Long") val _Long: Map<String, Long>?,
        @JsonProperty("_StringSet") val _StringSet: Map<String, Set<String>?>?,
    ){
        constructor() : this(null, null, null, null, null, null)
    }

    data class BackupFile(
        @JsonProperty("datastore") val datastore: BackupVars,
        @JsonProperty("settings") val settings: BackupVars
    ){
        constructor() : this(BackupVars(null, null, null, null, null, null), BackupVars(null, null, null, null, null, null))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBackup(context: Context?): BackupFile? {
        if (context == null) return null

        val allData = context.getSharedPrefs().all.filter { it.key.isTransferable() }
        val allSettings = context.getDefaultSharedPrefs().all.filter { it.key.isTransferable() }

        val allDataSorted = BackupVars(
            allData.filter { it.value is Boolean } as? Map<String, Boolean>,
            allData.filter { it.value is Int } as? Map<String, Int>,
            allData.filter { it.value is String } as? Map<String, String>,
            allData.filter { it.value is Float } as? Map<String, Float>,
            allData.filter { it.value is Long } as? Map<String, Long>,
            allData.filter { it.value as? Set<String> != null } as? Map<String, Set<String>>
        )

        val allSettingsSorted = BackupVars(
            allSettings.filter { it.value is Boolean } as? Map<String, Boolean>,
            allSettings.filter { it.value is Int } as? Map<String, Int>,
            allSettings.filter { it.value is String } as? Map<String, String>,
            allSettings.filter { it.value is Float } as? Map<String, Float>,
            allSettings.filter { it.value is Long } as? Map<String, Long>,
            allSettings.filter { it.value as? Set<String> != null } as? Map<String, Set<String>>
        )

        return BackupFile(
            allDataSorted,
            allSettingsSorted
        )
    }

    @WorkerThread
    fun restore(
        context: Context?,
        backupFile: BackupFile,
        restoreSettings: Boolean,
        restoreDataStore: Boolean
    ) {
        if (context == null) return
        if (restoreSettings) {
            context.restoreMap(backupFile.settings._Bool, true)
            context.restoreMap(backupFile.settings._Int, true)
            context.restoreMap(backupFile.settings._String, true)
            context.restoreMap(backupFile.settings._Float, true)
            context.restoreMap(backupFile.settings._Long, true)
            context.restoreMap(backupFile.settings._StringSet, true)
        }

        if (restoreDataStore) {
            context.restoreMap(backupFile.datastore._Bool)
            context.restoreMap(backupFile.datastore._Int)
            context.restoreMap(backupFile.datastore._String)
            context.restoreMap(backupFile.datastore._Float)
            context.restoreMap(backupFile.datastore._Long)
            context.restoreMap(backupFile.datastore._StringSet)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun backup(context: Context?) = ioSafe {
        if (context == null) return@ioSafe

//        var fileStream: OutputStream? = null
//        var printStream: PrintWriter? = null
        try {
            if (!context.checkWrite()) {
                showToast(R.string.backup_failed, Toast.LENGTH_LONG)
                context.getActivity()?.requestRW()
                return@ioSafe
            }

            val date = SimpleDateFormat("yyyy_MM_dd_HH_mm").format(Date(currentTimeMillis()))
            val ext = "txt"
            val displayName = "CS3_Backup_${date}"
            val backupFile = getBackup(context)
//            // Save to local storage
//            val stream = setupStream(context, displayName, null, ext, false)
//            fileStream = stream.openNew()
//            printStream = PrintWriter(fileStream)
//            printStream.print(mapper.writeValueAsString(backupFile))

            // Save to Firestore
            if (backupFile != null) {
                saveToFirestore(backupFile)
            }
            showToast(
                R.string.backup_success,
                Toast.LENGTH_LONG
            )
        } catch (e: Exception) {
            logError(e)
            try {
                showToast(
                    txt(R.string.backup_failed_error_format, e.toString()),
                    Toast.LENGTH_LONG
                )
            } catch (e: Exception) {
                logError(e)
            }
        } finally {
//            printStream?.closeQuietly()
//            fileStream?.closeQuietly()
        }
    }

    fun FragmentActivity.setUpBackup() {
        try {
            restoreFileSelector =
                registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri == null) return@registerForActivityResult
                    val activity = this
                    ioSafe {
                        try {
                            val input = activity.contentResolver.openInputStream(uri)
                                ?: return@ioSafe

                            val restoredValue =
                                mapper.readValue<BackupFile>(input)

                            restore(
                                activity,
                                restoredValue,
                                restoreSettings = true,
                                restoreDataStore = true
                            )
                            activity.runOnUiThread { activity.recreate() }
                        } catch (e: Exception) {
                            logError(e)
                            main { // smth can fail in .format
                                showToast(
                                    getString(R.string.restore_failed_format).format(e.toString())
                                )
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun FragmentActivity.restorePrompt() {
        runOnUiThread {
            // Try local restore first
            try {
                restoreFileSelector?.launch(
                    arrayOf(
                        "text/plain",
                        "text/str",
                        "text/x-unknown",
                        "application/json",
                        "unknown/unknown",
                        "content/unknown",
                        "application/octet-stream",
                    )
                )
            } catch (e: Exception) {
                showToast(e.message)
                logError(e)
            }
        }
    }

    private fun <T> Context.restoreMap(
        map: Map<String, T>?,
        isEditingAppSettings: Boolean = false
    ) {
        map?.filter { it.key.isTransferable() }?.forEach {
            setKeyRaw(it.key, it.value, isEditingAppSettings)
        }
    }

    @WorkerThread
    fun saveToFirestore(backupFile: BackupFile) = ioSafe {
        try {
            // Add the backup file as a document in the Firestore collection
            firestore.add(backupFile).await()
            showToast(R.string.backup_success, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            logError(e)
            showToast(txt(R.string.backup_failed_error_format, e.toString()), Toast.LENGTH_LONG)
        }
    }


    fun FragmentActivity.restoreFromFirestore(context: Context?): ListenerRegistration? {
        if (context == null) return null

        return firestore.addSnapshotListener { snapshot, e ->
            if (e != null) {
                logError(e)
                runOnUiThread {
                   // showToast(txt(R.string.restore_failed_error_format, e.toString()), Toast.LENGTH_LONG)
                }
                return@addSnapshotListener
            }

            // check if there is a collection
            if (snapshot == null || snapshot.isEmpty) {
                runOnUiThread {
                    showToast(R.string.restore_failed, Toast.LENGTH_LONG)
                }
                return@addSnapshotListener
            }
            if (!snapshot.isEmpty) {
                val latestBackup = snapshot.documents.first()
                val backupFile = latestBackup.toObject(BackupFile::class.java)
                val activity = this

                if (backupFile != null) {
                    runOnUiThread {
                       ioSafe {
                            restore(
                                 activity,
                                 backupFile,
                                 restoreSettings = true,
                                 restoreDataStore = true
                            )
                           activity.runOnUiThread { activity.recreate() }
                       }
                    }
                } else {
                    runOnUiThread {
                        showToast(R.string.restore_failed, Toast.LENGTH_LONG)
                    }
                }
            } else {
                runOnUiThread {
                    showToast(R.string.restore_failed, Toast.LENGTH_LONG)
                }
            }
        }
    }

    suspend fun FragmentActivity.copyBackupDataBetweenUsers(sourceUserId: String, destinationUserId: String, sourceBackupDocId: String, destinationBackupDocId: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Reference to the source user's backup document
        val sourceDocumentReference = firestore.collection("users").document(sourceUserId)
            .collection("backups").document(sourceBackupDocId)

        // Reference to the destination user's backup document
        val destinationDocumentReference = firestore.collection("users").document(destinationUserId)
            .collection("backups").document(destinationBackupDocId)

        try {
            // Get the data from the source backup document
            val snapshot = sourceDocumentReference.get().await()
            if (snapshot.exists()) {
                // Copy the data to the destination backup document
                val data = snapshot.data
                if (data != null) {
                    destinationDocumentReference.set(data).await()
                    println("Data copied successfully!")
                } else {
                    println("Source backup document has no data.")
                }
            } else {
                println("Source backup document does not exist.")
            }
        } catch (e: Exception) {
            println("Error copying data: ${e.message}")
        }
    }

}