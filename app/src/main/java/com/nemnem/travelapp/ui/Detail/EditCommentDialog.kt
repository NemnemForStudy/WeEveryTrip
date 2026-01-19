package com.nemnem.travelapp.ui.Detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nemnem.travelapp.data.model.comment.Comment

@Composable
fun EditCommentDialog(
    comment: Comment,               // 수정 대상 댓글 데이터
    onDismiss: () -> Unit,          // 닫기 요청
    onConfirm: (newContent: String) -> Unit // 수정 확정 및 새 내용 전달
) {
    // 1. 현재 댓글 내용으로 초기화
    var newContent by remember { mutableStateOf(comment.content) }

    AlertDialog(
        onDismissRequest = onDismiss, // 다이얼로그 바깥 클릭 시 닫기
        title = {
            Text("댓글 수정")
        },
        text = {
            Column {
                Text("작성자: ${comment.nickname}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("새 댓글 내용") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp), // 높이 제한
                    singleLine = false,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newContent.isNotBlank() && newContent != comment.content) {
                        onConfirm(newContent) // 뷰모델 호출 (2단계에서 연결)
                    } else {
                        onDismiss() // 내용 변화 없으면 그냥 닫기
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                // 내용이 없거나 원본과 같으면 버튼 비활성화
                enabled = newContent.isNotBlank() && newContent != comment.content
            ) {
                Text("수정 완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}