package com.example.e430.account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.e430.R
import com.example.e430.account.model.Account
import com.example.e430.core.network.E621Site
import com.example.e430.core.ui.RemotePreviewImage
import com.example.e430.core.ui.theme.E430Gold

@Composable
fun LoginDialog(
    site: E621Site,
    isLoggingIn: Boolean,
    loginFailed: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onGetApiKey: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        title = { Text(stringResource(R.string.sign_in_to_site, site.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.sign_in_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    enabled = !isLoggingIn,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLoggingIn,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loginFailed) {
                    Text(
                        text = stringResource(R.string.sign_in_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(username, apiKey) },
                enabled = username.isNotBlank() && apiKey.isNotBlank() && !isLoggingIn,
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(stringResource(R.string.sign_in))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onGetApiKey, enabled = !isLoggingIn) {
                Text(stringResource(R.string.get_api_key))
            }
        },
    )
}

@Composable
fun AccountScreen(
    account: Account,
    isSavingBlacklist: Boolean,
    blacklistSaveFailed: Boolean,
    blacklistSaved: Boolean,
    onSaveBlacklist: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var blacklist by remember(account.blacklistedTags) { mutableStateOf(account.blacklistedTags) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        AccountAvatar(account = account, size = 96)
        Text(
            text = account.username,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        AccountInfoRow(stringResource(R.string.account_id), account.id.toString())
        AccountInfoRow(stringResource(R.string.login_site), account.site.displayName)
        AccountInfoRow(stringResource(R.string.account_level), account.level)
        AccountInfoRow(stringResource(R.string.favorite_count), account.favoriteCount.toString())
        AccountInfoRow(stringResource(R.string.upload_count), account.uploadCount.toString())
        if (account.createdAt.isNotBlank()) {
            AccountInfoRow(stringResource(R.string.member_since), account.createdAt.take(10))
        }
        Text(
            text = stringResource(R.string.blacklist),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        )
        Text(
            text = stringResource(R.string.blacklist_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        OutlinedTextField(
            value = blacklist,
            onValueChange = { blacklist = it },
            label = { Text(stringResource(R.string.blacklisted_tags)) },
            minLines = 5,
            enabled = !isSavingBlacklist,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        if (blacklistSaveFailed) {
            Text(
                text = stringResource(R.string.blacklist_save_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        } else if (blacklistSaved) {
            Text(
                text = stringResource(R.string.blacklist_saved),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        Button(
            onClick = { onSaveBlacklist(blacklist) },
            enabled = !isSavingBlacklist && blacklist != account.blacklistedTags,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            if (isSavingBlacklist) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(stringResource(R.string.save_blacklist))
            }
        }
        Button(
            onClick = onLogout,
            modifier = Modifier.padding(top = 28.dp),
        ) {
            Text(stringResource(R.string.sign_out))
        }
    }
}

@Composable
fun AccountAvatar(account: Account?, size: Int, modifier: Modifier = Modifier) {
    var imageFailed by remember(account?.avatarUrl) { mutableStateOf(false) }
    val avatarModifier = modifier
        .size(size.dp)
        .clip(CircleShape)
    if (account?.avatarUrl != null && !imageFailed) {
        RemotePreviewImage(
            url = account.avatarUrl,
            contentDescription = stringResource(R.string.user_avatar),
            onLoadFailed = { imageFailed = true },
            modifier = avatarModifier,
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = avatarModifier.background(E430Gold),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = stringResource(R.string.user_avatar),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface)
    }
}
