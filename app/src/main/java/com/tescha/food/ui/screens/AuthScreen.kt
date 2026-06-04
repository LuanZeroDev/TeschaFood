package com.tescha.food.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tescha.food.R
import com.tescha.food.ui.theme.*
import com.tescha.food.ui.viewmodel.AuthViewModel

enum class AuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(
    initialMode: AuthMode = AuthMode.LOGIN,
    onBack: () -> Unit = {},
    onSuccess: (com.tescha.food.data.model.User) -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    var mode by remember { mutableStateOf(initialMode) }
    val formState by authViewModel.formState.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()

    LaunchedEffect(loginSuccess) {
        loginSuccess?.let { user ->
            authViewModel.clearSuccess()
            onSuccess(user)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
        ) {
            Spacer(Modifier.height(40.dp))

            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = MetallicGold)
            }

            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_blanco),
                contentDescription = "Logo Tescha Food",
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(20.dp))

            AnimatedContent(targetState = mode, label = "auth_title") { target ->
                Column {
                    Text(
                        text = if (target == AuthMode.LOGIN) "INICIAR\nSESIÓN" else "CREAR\nCUENTA",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            lineHeight = 42.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            if (mode == AuthMode.REGISTER) {
                GhostTextField(
                    value = formState.name,
                    onValueChange = authViewModel::onNameChange,
                    label = "Nombre completo",
                )
                Spacer(Modifier.height(16.dp))
                GhostTextField(
                    value = formState.phone,
                    onValueChange = authViewModel::onPhoneChange,
                    label = "Teléfono",
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(16.dp))
            }

            GhostTextField(
                value = formState.email,
                onValueChange = authViewModel::onEmailChange,
                label = "Correo electrónico",
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(16.dp))

            GhostPasswordField(
                value = formState.password,
                onValueChange = authViewModel::onPasswordChange,
                label = "Contraseña",
            )

            if (formState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = formState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { if (mode == AuthMode.LOGIN) authViewModel.login() else authViewModel.register() },
                enabled = !formState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "ENTRAR" else "REGISTRARME",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (mode == AuthMode.LOGIN) "¿No tienes cuenta? " else "¿Ya tienes cuenta? ",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { mode = if (mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN }) {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "Regístrate" else "Inicia sesión",
                        color = MetallicGold,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun GhostTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MetallicGold,
            unfocusedBorderColor = Outline,
            focusedLabelColor = MetallicGold,
            unfocusedLabelColor = OnSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
        singleLine = true,
    )
}

@Composable
fun GhostPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Ocultar" else "Mostrar",
                    tint = Outline,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MetallicGold,
            unfocusedBorderColor = Outline,
            focusedLabelColor = MetallicGold,
            unfocusedLabelColor = OnSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
        singleLine = true,
    )
}
