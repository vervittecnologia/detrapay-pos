package com.detrapay.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.R
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.theme.DetrapayColors
import com.detrapay.ui.theme.DetrapayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DetrapayTheme {
                val formState by viewModel.loginFormState.observeAsState()
                val loginResult by viewModel.loginResult.observeAsState()
                var cnpj by rememberSaveable {
                    mutableStateOf(formatCnpj(viewModel.getLastLoggedCnpj().orEmpty()))
                }
                var password by rememberSaveable { mutableStateOf("") }
                var isLoading by rememberSaveable { mutableStateOf(false) }
                var errorMessage by rememberSaveable { mutableStateOf<Int?>(null) }

                LaunchedEffect(loginResult) {
                    val result = loginResult ?: return@LaunchedEffect
                    isLoading = false
                    errorMessage = result.error
                    if (result.success != null) {
                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    }
                }

                LoginScreen(
                    cnpj = cnpj,
                    password = password,
                    cnpjError = formState?.cnpjError,
                    passwordError = formState?.passwordError,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    canSubmit = formState?.isDataValid == true,
                    onCnpjChange = { raw ->
                        cnpj = formatCnpj(raw)
                        errorMessage = null
                        viewModel.loginDataChanged(cnpj, password)
                    },
                    onPasswordChange = { value ->
                        password = value
                        errorMessage = null
                        viewModel.loginDataChanged(cnpj, password)
                    },
                    onSubmit = {
                        if (!isLoading && formState?.isDataValid == true) {
                            isLoading = true
                            errorMessage = null
                            viewModel.login(cnpj, password)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    cnpj: String,
    password: String,
    cnpjError: Int?,
    passwordError: Int?,
    errorMessage: Int?,
    isLoading: Boolean,
    canSubmit: Boolean,
    onCnpjChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetrapayColors.PrimarySoft)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.login_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(start = 32.dp, top = 28.dp, end = 32.dp)
                    .fillMaxWidth()
                    .height(72.dp),
            )
            Image(
                painter = painterResource(R.drawable.login_illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(132.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = cnpj,
                        onValueChange = onCnpjChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        label = { Text(stringResource(R.string.prompt_cnpj), fontSize = 16.sp) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = cnpjError != null,
                        supportingText = cnpjError?.let { error ->
                            { Text(stringResource(error)) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        label = { Text(stringResource(R.string.prompt_password), fontSize = 16.sp) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = passwordError != null,
                        supportingText = passwordError?.let { error ->
                            { Text(stringResource(error)) }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSubmit()
                            },
                        ),
                    )
                    errorMessage?.let { error ->
                        Text(
                            text = stringResource(error),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = canSubmit && !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.action_login))
                        }
                    }
                }
            }
        }
    }
}

private fun formatCnpj(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(14)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 5) append('.')
            if (index == 8) append('/')
            if (index == 12) append('-')
            append(char)
        }
    }
}
