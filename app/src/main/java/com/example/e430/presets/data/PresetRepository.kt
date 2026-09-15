package com.example.e430.presets.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import com.example.e430.presets.model.PresetFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PresetRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("preset_storage", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun hasStorageAccess(): Boolean {
        val treeUri = storedTreeUri() ?: return false
        return resolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission && it.isWritePermission }
    }

    fun takeStorageAccess(treeUri: Uri): Boolean = runCatching {
        val selectedPath = DocumentsContract.getTreeDocumentId(treeUri).substringAfter(':').trim('/')
        require(selectedPath.isEmpty() || selectedPath.equals("Documents", ignoreCase = true))
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(treeUri, flags)
        preferences.edit { putString(TREE_URI, treeUri.toString()) }
        true
    }.getOrDefault(false)

    suspend fun load(owner: String): PresetFile = withContext(Dispatchers.IO) {
        val treeUri = requireNotNull(storedTreeUri()) { "Preset storage access is required" }
        val directory = ensurePresetDirectory(treeUri)
        val fileName = fileName(owner)
        val fileUri = findChild(treeUri, directory, fileName)
            ?: requireNotNull(
                DocumentsContract.createDocument(resolver, directory, JSON_MIME, fileName),
            ) { "Could not create preset file" }
        val text = resolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (text.isBlank()) {
            return@withContext PresetFile(owner = owner).also { write(fileUri, it) }
        }
        json.decodeFromString<PresetFile>(text).copy(owner = owner)
    }

    suspend fun save(owner: String, file: PresetFile) = withContext(Dispatchers.IO) {
        val treeUri = requireNotNull(storedTreeUri()) { "Preset storage access is required" }
        val directory = ensurePresetDirectory(treeUri)
        val name = fileName(owner)
        val fileUri = findChild(treeUri, directory, name)
            ?: requireNotNull(DocumentsContract.createDocument(resolver, directory, JSON_MIME, name))
        write(fileUri, file.copy(owner = owner))
    }

    private fun ensurePresetDirectory(treeUri: Uri): Uri {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val documents = if (rootId.substringAfter(':').trim('/').equals("Documents", ignoreCase = true)) {
            root
        } else {
            findChild(treeUri, root, "Documents")
                ?: requireNotNull(
                    DocumentsContract.createDocument(
                        resolver,
                        root,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        "Documents",
                    ),
                )
        }
        return findChild(treeUri, documents, "E430")
            ?: requireNotNull(
                DocumentsContract.createDocument(
                    resolver,
                    documents,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    "E430",
                ),
            )
    }

    private fun findChild(treeUri: Uri, parent: Uri, name: String): Uri? {
        val parentId = DocumentsContract.getDocumentId(parent)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn).equals(name, ignoreCase = true)) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idColumn))
                }
            }
        }
        return null
    }

    private fun write(uri: Uri, file: PresetFile) {
        val content = json.encodeToString(PresetFile.serializer(), file)
        requireNotNull(resolver.openOutputStream(uri, "wt"))
            .bufferedWriter()
            .use { it.write(content) }
    }

    private fun storedTreeUri(): Uri? = preferences.getString(TREE_URI, null)?.let(Uri::parse)

    private fun fileName(owner: String): String {
        val safeOwner = owner.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "anonymous" }
        return "$safeOwner.json"
    }

    private companion object {
        const val TREE_URI = "tree_uri"
        const val JSON_MIME = "application/json"
    }
}
