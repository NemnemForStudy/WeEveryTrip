package com.example.travelapp.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.travelapp.ui.theme.Beige
import com.example.travelapp.ui.write.PostImage
import com.example.travelapp.util.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostForm(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    category: String,
    onCategoryClick: () -> Unit,
    tagsInput: String,
    onTagsChange: (String) -> Unit,
    startDate: Long?,
    endDate: Long?,
    onDateClick: () -> Unit,
    tripDays: List<Long>,
    groupedImages: Map<Int, List<PostImage>>,
    existingImages: List<String> = emptyList(),
    onGalleryClick: () -> Unit,
    onSwapImages: (Int, Int, Int) -> Unit,
    onPreviewClick: (Int, List<PostImage>) -> Unit,
    navController: androidx.navigation.NavController,
    drawerState: DrawerState,
    onSubmitClick: () -> Unit,
    submitButtonText: String,
    isSubmitting: Boolean = false,
    toFullUrl: (String) -> String = { it }
) {
    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("yy년 MM월 dd일", Locale.KOREA) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(320.dp),
                        drawerContainerColor = Beige,
                        drawerContentColor = Color.Black
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("여행 일정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.Black)
                            TextButton(onClick = onSubmitClick) {
                                if (isSubmitting)
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 3.dp, color = Color(0xFF1976D2))
                                else Text(submitButtonText, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))

                        if (tripDays.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(tripDays.size) { index ->
                                    val dayNumber = index + 1
                                    val dayMillis = tripDays[index]
                                    val dayImages = groupedImages[dayNumber] ?: emptyList()
                                    var isExpanded by remember { mutableStateOf(false) }
                                    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Day $dayNumber", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                                Text(DateUtils.formatToDisplay(dayMillis), fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (dayImages.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { onPreviewClick(dayNumber, dayImages) },
                                                        modifier = Modifier.height(32.dp).padding(end = 8.dp),
                                                        shape = RoundedCornerShape(4.dp),
                                                        border = BorderStroke(1.5.dp, Color.Black)
                                                    ) { Text("미리보기", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black) }
                                                }
                                                Text("${dayImages.size}장", fontWeight = FontWeight.Bold, color = Color.Black)
                                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.rotate(rotationState), tint = Color.Black)
                                            }
                                        }
                                        AnimatedVisibility(visible = isExpanded) {
                                            ImageReorderList(dayNumber, dayImages, onSwapImages)
                                        }
                                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if(submitButtonText == "게시") "글쓰기" else "게시물 수정", fontWeight = FontWeight.Black, color = Color.Black) },
                            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.Black) } },
                            actions = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "메뉴", tint = Color.Black, modifier = Modifier.size(28.dp)) }
                                TextButton(onClick = onSubmitClick) { Text(submitButtonText, fontWeight = FontWeight.Black, color = Color(0xFF1976D2)) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Beige)
                        )
                    },
                    containerColor = Beige
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
                    ) {
                        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 8.dp).clickable { onCategoryClick() }) {
                                Text(category, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = title,
                                onValueChange = onTitleChange,
                                placeholder = { Text("글 제목", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Black
                                )
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))

                        // ✅ 날짜 텍스트 로직 수정: yy.MM.dd - yy.MM.dd
                        val dateText = when {
                            startDate != null && endDate != null -> "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
                            startDate != null -> "${sdf.format(Date(startDate))} - 종료일"
                            else -> "여행 기간 선택"
                        }

                        val newImages = remember(groupedImages) {
                            groupedImages.values.flatten()
                        }

                        ClickableRow(Icons.Default.CalendarMonth, dateText, onDateClick)
                        ClickableRow(Icons.Default.CameraAlt, "사진 첨부하기 (${groupedImages.values.flatten().size + existingImages.size})", onGalleryClick)

                        // 사진 미리보기
                        if (existingImages.isNotEmpty() || newImages.isNotEmpty()) {
                            FormLabel("첨부된 사진")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                items(existingImages) { url ->
                                    AsyncImage(model = toFullUrl(url), contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                                items(newImages) { img ->
                                    Image(painter = rememberAsyncImagePainter(img.uri), contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                }
                            }
                        }

                        FormLabel("태그")
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = onTagsChange,
                            placeholder = { Text("#제주도 #맛집탐방", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = { Icon(Icons.Default.Tag, null, tint = Color.Black) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Gray, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )

                        FormLabel("여행 이야기")
                        OutlinedTextField(
                            value = content,
                            onValueChange = onContentChange,
                            placeholder = { Text("추억을 기록해보세요...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 350.dp),
                            textStyle = TextStyle(fontWeight = FontWeight.Medium, color = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Black, unfocusedBorderColor = Color.Gray, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                        Spacer(Modifier.height(50.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FormLabel(text: String) {
    Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
fun ClickableRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
    }
    HorizontalDivider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
}

@Composable
fun ImageReorderList(dayNumber: Int, images: List<PostImage>, onSwap: (Int, Int, Int) -> Unit) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 76.dp.toPx() }

    Column {
        images.forEachIndexed { index, image ->
            var draggingIndex by remember { mutableIntStateOf(-1) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            val isDragging = index == draggingIndex

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { if (isDragging) IntOffset(0, dragOffsetY.roundToInt()) else IntOffset.Zero },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = rememberAsyncImagePainter(image.uri), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Text(
                    if(image.timestamp != null) SimpleDateFormat("a hh:mm").format(Date(image.timestamp)) else "시간 정보 없음",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.DragHandle, null, tint = Color.Black,
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index; dragOffsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val target = (index + (dragOffsetY / itemHeightPx).toInt()).coerceIn(0, images.lastIndex)
                                if (target != index) {
                                    onSwap(dayNumber, index, target)
                                    draggingIndex = target
                                    dragOffsetY = 0f
                                }
                            },
                            onDragEnd = { draggingIndex = -1 },
                            onDragCancel = { draggingIndex = -1 }
                        )
                    }
                )
            }
        }
    }
}