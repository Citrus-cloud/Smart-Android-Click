package com.clickflow.android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.ui.ClickFlowApp
import com.clickflow.android.ui.ClickFlowTheme

private const val ONBOARDING_PREFS = "clickflow_onboarding"
private const val KEY_DONE = "done"

class MainActivity : ComponentActivity() {

    private val viewModel: ClickFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                FirstRunGate(viewModel)
            }
        }
    }
}

@Composable
private fun FirstRunGate(viewModel: ClickFlowViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE) }
    var done by remember { mutableStateOf(prefs.getBoolean(KEY_DONE, false)) }

    if (done) {
        ClickFlowApp(viewModel)
    } else {
        OnboardingScreen(
            onFinish = {
                prefs.edit().putBoolean(KEY_DONE, true).apply()
                done = true
            },
        )
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(34.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF56514A))))
                    .padding(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ClickFlow", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Умный автокликер без лишней путаницы", color = Color.White.copy(alpha = 0.82f))
                    Text("Метки · Картинки · OCR · Сценарии", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            GuideCard(
                title = "1. Включи Accessibility",
                body = "Это системное разрешение Android нужно, чтобы ClickFlow мог делать реальные тапы. Без него приложение может только показывать интерфейс.",
            )
            GuideCard(
                title = "2. Включи «Поверх других окон»",
                body = "Это нужно для плавающих меток. Метку можно поставить поверх другого приложения и запускать тапы прямо оттуда.",
            )
            GuideCard(
                title = "3. Поставь метки и нажми Запустить",
                body = "На главном экране можно добавить до 5 меток, настроить таймер, повторы, бесконечный режим и задержку старта.",
            )
            GuideCard(
                title = "4. Расширенные функции",
                body = "В разделе «Расширенные» есть клик по картинке, клик по тексту через OCR и простой сценарий по overlay-меткам.",
            )
            GuideCard(
                title = "5. Как остановить",
                body = "На главном экране есть «Стоп» и «Аварийная остановка». В режимах картинки/текста есть отдельные кнопки остановки.",
            )

            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Text("Начать")
            }
            OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Text("Пропустить")
            }
        }
    }
}

@Composable
private fun GuideCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
