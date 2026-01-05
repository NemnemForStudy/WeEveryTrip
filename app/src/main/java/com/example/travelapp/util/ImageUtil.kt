package com.example.travelapp.util

import com.example.travelapp.ui.write.PostImage

object ImageUtil {
    // 특정 날짜 이미지 리스트에서 선택한 이미지 제거
    fun removeImageFromGrouped(
        currentMap: Map<Int, List<PostImage>>,
        day: Int,
        imageToRemove: PostImage
    ): Map<Int, List<PostImage>> {
        val mutableMap = currentMap.toMutableMap()
        val dayList = mutableMap[day]?.toMutableList() ?: return currentMap

        // 이미지 제거
        dayList.remove(imageToRemove)

        if(dayList.isEmpty()) {
            mutableMap.remove(day) // 사진이 하나도 없으면 해당 Day 키 자체 삭제
        } else {
            mutableMap[day] = dayList
        }

        return mutableMap
    }
}