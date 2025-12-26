package com.example.travelapp.data.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareUtil {
    /**
     * 이미지(지도 캡쳐)와 텍스트를 함께 SNS에 공유.
     * @param context 안드로이드 시스템 기능을 사용하기 위한 맥락 정보
     * @param bitmap 네이버 지도에서 캡처한 비트맵 이미지 데이터
     * @param shareText 링크와 닉네임이 포함된 홍보 문구
     */

    fun sharePost(context: Context, bitmap: Bitmap, shareText: String) {
        // 메모리에 있는 이미지를 실제 파일로 저장하고 그 주소 가져옴
        val imageUrl = saveBitmapToCache(context, bitmap)

        if(imageUrl != null) {
            // Intent 생성. 인텐트는 다른 앱에게 전송할 메시지 박스
            val intent = Intent(Intent.ACTION_SEND).apply {
                // 이미지와 텍스트를 동시에 보낼 때 사용하는 표준 MIME
                type = "image/*"

                // 공유창에 함께 들어갈 텍스트 담음.
                putExtra(Intent.EXTRA_TEXT, shareText)

                // 공유할 이미지 파일의 주소 담음.
                putExtra(Intent.EXTRA_STREAM, imageUrl)

                // 파일을 받는 앱에게 "이 파일을 읽어도 좋다" 는 임시 권한 부여
                // 이 설정 없으면 상대방 앱에서 보안 에러가 나며 이미지 보이지 않음.
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 시스템 공유창 띄움
            context.startActivity(Intent.createChooser(intent, "ModuTrip 여행 공유"))
        }
    }

    /**
     * 비트맵 이미지를 앱 내부 임시 폴더에 저장
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            // 앱 전용 캐시 디렉토리에 'shared_images' 라는 폴더 지정
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs() // 폴더 없으면 생성

            // 폴더 안에 파일명 생성
            val file = File(cachePath, "temp_shared_map.jpg")
            val stream = FileOutputStream(file)

            // 비트맵 데이터 JPEG 형식으로 압축해 파일에 기록
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()

            // FileProvider 통해 파일을 가리키는 안전한 Content Uri 생성
            // 두 번째 인자인 authrities는 메니페스트에 적은 '${applicationId}.fileprovider' 와 정확히 일치해야함.
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}