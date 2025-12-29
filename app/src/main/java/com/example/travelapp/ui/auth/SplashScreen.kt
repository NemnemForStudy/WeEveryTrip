package com.example.travelapp.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.Beige
// 📍 정의한 색상 변수 사용 (DRY 원칙)
import com.example.travelapp.ui.theme.PointRed
import com.example.travelapp.ui.theme.TextMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val anim1 = remember { Animatable(0f) }
    val anim2 = remember { Animatable(0f) }
    val anim3 = remember { Animatable(0f) }
    val pinAnim = remember { Animatable(-60f) }

    LaunchedEffect(Unit) {
        anim1.animateTo(1f, animationSpec = tween(500))
        delay(300)
        anim2.animateTo(1f, animationSpec = tween(500))
        delay(300)

        launch {
            pinAnim.animateTo(0f, animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ))
        }
        anim3.animateTo(1f, animationSpec = tween(500))

        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Beige),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // "우리"
            AnimatedTextItem(text = "우리", animValue = anim1.value, fontWeight = FontWeight.Light)

            // "모두의"
            AnimatedTextItem(text = "모두의", animValue = anim2.value, fontWeight = FontWeight.Medium)

            // "[빨간핀]ㅕ행"
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .offset(x = 24.dp)
                    .alpha(anim3.value)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PointRed,
                    modifier = Modifier
                        .size(50.dp)
                        .offset(y = pinAnim.value.dp)
                )

                Text(
                    text = "ㅕ행",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    modifier = Modifier.offset(x = (-12).dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedTextItem(text: String, animValue: Float, fontWeight: FontWeight) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = fontWeight,
        color = TextMain,
        modifier = Modifier
            .alpha(animValue)
            .offset(y = (10 * (1 - animValue)).dp)
            .padding(bottom = 0.dp)
    )
}