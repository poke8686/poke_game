package com.poke86.game.ui.chat

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.poke86.game.network.ChatMessage
import com.poke86.game.network.ChatWsClient
import kotlinx.coroutines.launch

val KakaoYellow = Color(0xFFFEE500)
val KakaoBackground = Color(0xFFBACEE0)
val MyBubbleColor = Color(0xFFFFEB33)
val OtherBubbleColor = Color(0xFFFFFFFF)

val cuteEmoticons = listOf(
    "https://media.tenor.com/C9Z_RxyKk8AAAAAM/cat-dance.gif",
    "https://media.tenor.com/Z4O88ZtyN2MAAAAM/cute-cat.gif",
    "https://media.tenor.com/0FwJ6vYV5qQAAAAM/puppy-dog.gif",
    "https://media.tenor.com/JjK9-yGgN10AAAAM/peach-goma.gif"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    navController: NavController,
    roomId: String,
    roomName: String
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val chatClient = remember { ChatWsClient() }
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var showEmoticonPanel by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val myId = remember { "user_${(1..1000).random()}" }

    LaunchedEffect(Unit) {
        chatClient.connect(this)
        chatClient.connectionEvent.collect { connected ->
            if (connected) {
                chatClient.joinRoom(roomId, "사용자")
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            chatClient.messages.collect { history ->
                messages = history
            }
        }
        launch {
            chatClient.newMessage.collect { msg ->
                messages = messages + msg
                scope.launch {
                    listState.animateScrollToItem(messages.size)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            chatClient.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmoticonPanel = !showEmoticonPanel }) {
                            Icon(Icons.Default.SentimentSatisfiedAlt, "이모티콘", tint = Color.Gray)
                        }
                        TextField(
                            value = inputText,
                            onValueChange = { 
                                inputText = it
                                if (showEmoticonPanel) showEmoticonPanel = false
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("메시지를 입력하세요") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    chatClient.sendMessage(roomId, inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, "전송", tint = if (inputText.isNotBlank()) Color(0xFF3B1E1E) else Color.Gray)
                        }
                    }
                }
                AnimatedVisibility(visible = showEmoticonPanel) {
                    Surface(color = Color(0xFFF5F5F5), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            cuteEmoticons.forEach { url ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url).build(),
                                    imageLoader = imageLoader,
                                    contentDescription = "Emoticon",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable {
                                            chatClient.sendEmoticon(roomId, url)
                                            showEmoticonPanel = false
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(KakaoBackground)
                .clickable { showEmoticonPanel = false }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.isSystem) {
                        SystemMessage(message.content)
                    } else {
                        ChatBubble(
                            message = message,
                            isMine = message.senderId == myId || message.senderName == "사용자",
                            imageLoader = imageLoader,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMine: Boolean, imageLoader: ImageLoader, context: android.content.Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(message.senderName.take(1))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            if (!isMine) {
                Text(message.senderName, fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            if (message.msgType == "EMOTICON") {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(message.content).build(),
                    imageLoader = imageLoader,
                    contentDescription = "Emoticon",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    color = if (isMine) MyBubbleColor else OtherBubbleColor,
                    shape = RoundedCornerShape(
                        topStart = if (isMine) 12.dp else 0.dp,
                        topEnd = if (isMine) 0.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SystemMessage(content: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0x33000000),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}
