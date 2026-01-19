package com.nemnem.travelapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TravelItemCard(
    locationName: String,
    date: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // 클릭 가능하게 만듦
        shape = RoundedCornerShape(16.dp) // 요즘 스타일: 모서리를 둥글게!
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = locationName, style = MaterialTheme.typography.titleMedium)
            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}