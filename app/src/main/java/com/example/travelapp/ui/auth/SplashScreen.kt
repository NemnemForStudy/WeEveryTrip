package com.example.travelapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.theme.TravelAppTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000L) // 3초 대기
        onTimeout()
    }

    // Surface를 사용해 테마에 지정된 배경색(Beige)을 화면에 실제로 그려줍니다.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 화면 전체를 차지하고, 내용물을 정중앙에 배치하기 위한 바깥쪽 Column 입니다.
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "우리", "모두의", "여행" 텍스트 블록의 내부 정렬을 위한 안쪽 Column 입니다.
            Column {
                Text(
                    text = "우리",
                    style = MaterialTheme.typography.headlineLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                Text(
                    text = "모두의",
                    style = MaterialTheme.typography.headlineLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF212121)
                )

                // 여행(공간과 함께 가로로 배치)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 의도적인 빈 공간을 만들기 위해 Spacer
                    Spacer(modifier = Modifier.width(30.dp))

                    Text(
                        text = "여행",
                        style = MaterialTheme.typography.headlineLarge,
                        color = androidx.compose.ui.graphics.Color(0xFF212121)
                    )
                }
            }
        }
    }
}

// @Preview 기능을 사용하면 앱 전체 실행하지 않고도 원하는 화면만 즉시 확인할 수 있다.
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    TravelAppTheme {
        SplashScreen(onTimeout = {})
    }
}
