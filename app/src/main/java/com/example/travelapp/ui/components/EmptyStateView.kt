package com.example.travelapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.PointRed
import com.example.travelapp.ui.theme.TextMain
import com.example.travelapp.ui.theme.TextSub

@Composable
fun EmptyTravelState(
    title: String = "아직 기록이 없어요",
    description: String = "첫 번째 여행 사진을 올려서\n나만의 지도를 채워보세요!",
    buttonText: String = "여행 기록하러 가기",
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 📍 여행 가방 아이콘 (시그니처 레드 컬러 활용)
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = PointRed.copy(alpha = 0.2f) // 은은하게 표현
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSub, // 훨씬 선명해짐
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 📍 수익형 앱의 필수 요소: CTA(Call To Action) 버튼
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(containerColor = PointRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(54.dp).fillMaxWidth(0.7f)
        ) {
            Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}