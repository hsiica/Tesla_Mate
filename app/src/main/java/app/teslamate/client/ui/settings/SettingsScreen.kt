package app.teslamate.client.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.teslamate.client.R
import app.teslamate.client.ui.UserError
import app.teslamate.client.ui.components.ScreenLoading
import app.teslamate.client.ui.message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onboarding: Boolean,
    embedded: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var revealToken by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (embedded) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (onboarding) R.string.onboarding_title else R.string.settings_title),
                    )
                },
            )
        },
    ) { padding ->
        if (!form.ready) {
            ScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            if (onboarding) {
                Text(
                    stringResource(R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
            OutlinedTextField(
                value = form.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.base_url_label)) },
                placeholder = { Text(stringResource(R.string.base_url_placeholder)) },
                supportingText = {
                    Text(
                        if (form.invalidUrl) {
                            stringResource(R.string.invalid_url)
                        } else {
                            stringResource(R.string.base_url_help)
                        },
                    )
                },
                isError = form.invalidUrl,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            if (form.baseUrl.trim().startsWith("http://")) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.http_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.token,
                onValueChange = viewModel::onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.api_token_label)) },
                placeholder = { Text(stringResource(R.string.api_token_placeholder)) },
                supportingText = { Text(stringResource(R.string.api_token_help)) },
                singleLine = true,
                visualTransformation = if (revealToken) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { revealToken = !revealToken }) {
                        Text(
                            stringResource(if (revealToken) R.string.hide_token else R.string.show_token),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            if (form.token.isNotBlank() && form.token.trim().length < 32) {
                Text(
                    stringResource(R.string.token_short_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::save,
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (form.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save_and_connect))
                }
            }
            form.connectedCount?.let { count ->
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.connected_cars, count),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            form.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                ErrorText(error)
            }
            if (!onboarding && (form.baseUrl.isNotBlank() || form.token.isNotBlank())) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { confirmClear = true }) {
                    Text(stringResource(R.string.clear_settings))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_settings_title)) },
            text = { Text(stringResource(R.string.clear_settings_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text(stringResource(R.string.clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ErrorText(error: UserError) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Text(
        error.message(context),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}
