<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="h-screen flex flex-col overflow-y-auto" ref="chatContainer">
        <!-- 聊天记录区域 -->
        <div class="flex-1 max-w-4xl mx-auto pb-24 pt-4 px-4 w-full">
          <!-- 遍历聊天记录 -->
          <template v-for="(chat, index) in chatList" :key="index">
            <!-- 用户提问消息（靠右） -->
            <div v-if="chat.role === 'user'" class="flex justify-end mb-4">
              <div class="quesiton-container">
                <p>{{ chat.content }}</p>
              </div>
            </div>

            <!-- 大模型回复消息（靠左） -->
            <div v-else class="flex mb-4">
              <!-- 头像 -->
              <div class="flex-shrink-0 mr-3">
                <div class="w-8 h-8 rounded-full flex items-center justify-center border border-gray-200">
                  <SvgIcon name="firework" customCss="w-8 h-8"></SvgIcon>
                </div>
              </div>
              <!-- 回复的内容 -->
              <div class="p-1 mb-2 max-w-[90%]">
                <LoadingDots v-if="chat.loading" />
                <StreamMarkdownRender :content="chat.content" />
              </div>
            </div>
          </template>
        </div>

        <!-- 提问输入框 -->
        <ChatInputBox v-model="message" containerClass="sticky max-w-4xl mx-auto bg-white bottom-8 left-0 w-full"
          @sendMessage="sendMessage" />
      </div>
    </template>
  </Layout>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch} from 'vue';
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import Layout from '@/layouts/Layout.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { useRoute } from 'vue-router'
// 导入Pinia store
import { useChatStore } from '@/stores/chatStore'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import LoadingDots from '@/components/LoadingDots.vue';
import { findChatMessagePageList } from '@/api/chat.js';

// 获取 chat store
const chatStore = useChatStore()

console.log('首页传递过来的消息: ', history.state?.firstMessage)

const route = useRoute()

// 输入的消息
const message = ref(history.state?.firstMessage || '')
// 聊天容器引用
const chatContainer = ref(null)
// 聊天记录 
const chatList = ref([])
// 分页状态
const current = ref(1)
const size = ref(10)
const hasMore = ref(true)
const isLoadingMore = ref(false)

// 监听路由参数变化
watch(() => route.params.chatId, (newChatId) => {
  if (newChatId) {
    chatId.value = newChatId
    // 重置聊天记录和分页状态
    chatList.value = []
    current.value = 1
    hasMore.value = true
    // 加载新的聊天记录
    loadHistoryMessages()
  }
})

// 加载历史对话信息
const loadHistoryMessages = async () => {
  findChatMessagePageList(current.value, size.value, chatId.value)
    .then((res) => {
      isLoadingMore.value = false

      if (res && res.data.success) {
        const historyMessages = res.data.data
        // 判断是否还有下一页
        hasMore.value = res.data.pages > current.value
        if (historyMessages && historyMessages.length > 0) {
          // 历史消息添加到聊天列表顶部
          chatList.value = [...historyMessages, ...chatList.value];
          // 首次加载完成后，滚动到底部
          if (current.value === 1) {
            scrollToBottom()
          }
        }
      }
    }).catch((error) => {
      console.error('加载历史消息失败: ', error)
      isLoadingMore.value = false
    })
}

// 监听滚动事件
const handleScroll = () => {
  if (chatContainer.value) {
    // 到滚动区域顶部的距离
    const scrollTop = chatContainer.value.scrollTop
    // 滚动区域完整高度
    const scrollHeight = chatContainer.value.scrollHeight
    
    // 打印滚动过程中的详细日志
    console.log('=== 滚动事件日志 ===')
    console.log('scrollTop:', scrollTop)
    console.log('scrollHeight:', scrollHeight)
    console.log('isLoadingMore:', isLoadingMore.value)
    console.log('hasMore:', hasMore.value)

    if (scrollTop < 50 && hasMore.value && !isLoadingMore.value) {
      console.log('需要加载更多历史消息...')
      loadMoreHistoryMessages()
    }
  }
}

// 加载更多历史消息
const loadMoreHistoryMessages = async () => {
  console.log('开始加载更多历史消息...')
  // 双重检查
  if (!hasMore.value) {
    console.log('没有更多历史消息可加载。')
    return
  }
  if (isLoadingMore.value) {
    console.log('正在加载中，请稍后...')
    return
  }
  // 正在加载中
  isLoadingMore.value = true

  // 下一页页码 与 当前页码备份
  const nextPageNo = current.value + 1
  const prevPageNo = current.value
  current.value = nextPageNo
  try {
    loadHistoryMessages()
  } catch (error) {
    // 出错时，回退页码
    current.value = prevPageNo
  }
}

onMounted(() => {
  // 加载历史消息
  loadHistoryMessages()

  // 为聊天容器添加滚动事件监听以及点击监听
  if (chatContainer.value) {
    chatContainer.value.addEventListener('scroll', handleScroll)
    chatContainer.value.addEventListener('click', handleGlobalClick)
  }

  const firstMessage = history.state?.firstMessage
  // 检查跳转路由时，是否有初始消息
  if (firstMessage) {
    message.value = firstMessage
    // 发送消息
    sendMessage({
      selectedModel: chatStore.selectedModel,
      isNetworkSearch: chatStore.isNetworkSearchSelected,
      isRagSelected: chatStore.isRagSelected
    })
    
    // 发送完后，清除初始消息，避免重复发送
    if (history.replaceState) {
      const newState = { ...history.state }
      delete newState.firstMessage
      history.replaceState(newState, document.title)
    }
  }
})

// AbortController 用于取消请求
let controller = null;

// 对话 ID
const chatId = ref(route.params.chatId || null)

// 发送消息
const sendMessage = async (payload) => {
  // 校验发送的消息不能为空
  if (!message.value.trim()) return

  console.log('选中的模型:', payload.selectedModel)
  console.log('是否联网:', payload.isNetworkSearch)
  console.log('是否开启RAG', payload.isRagSelected)

  const isRag = payload.isRagSelected

  // 将用户发送的消息添加到 chatList 聊天列表中
  const userMessage = message.value.trim()
  chatList.value.push({ role: 'user', content: userMessage })

  // 点击发送按钮后，清空输入框
  message.value = ''

  // 添加一个占位的回复消息
  chatList.value.push({ role: 'assistant', content: '', loading:true})

  const url = isRag ? 'http://localhost:8080/customer-service/chat/completion' :
                      'http://localhost:8080/chat/completion' 

  const requestBody = {
    message: userMessage,
    chatId: chatId.value
  } 
  
  if (!isRag) {
    // 非 RAG 
    requestBody.modelName = payload.selectedModel?.name
    requestBody.networkSearch = payload.isNetworkSearch
  }

  // 响应的回答
  let responseText = ''
  // 获取最后一条消息
  const lastMessage = chatList.value[chatList.value.length - 1]

  controller = new AbortController()
                    
  try {

    await fetchEventSource(url, {
      method: 'POST',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody),
      openWhenHidden: true, 
      onmessage(msg) {
        if (msg.event === '') {
          //收到第一条消息后设置loading为false
          if (lastMessage.loading) {
            lastMessage.loading = false
          }
          // 解析 JSON
          let parseJson = JSON.parse(msg.data)
          // 持续追加流式回答
          responseText += parseJson.v

          // 更新最后一条消息
          chatList.value[chatList.value.length - 1].content = responseText
          // 滚动到底部
          scrollToBottom()
        }
        else if (msg.event === 'close') {
          console.log('-- sse close')
          controller.abort();
        }
      },
      onerror(err) {
        throw err;    // 必须 throw 才能停止 
      }
    })


  } catch (error) {
    console.error('发送消息错误: ', error)
    // 提示用户 “请求出错”
    const lastMessage = chatList.value[chatList.value.length - 1]
    lastMessage.loading = false
    lastMessage.content = '抱歉，请求出错了，请稍后重试。'
    // 滚动到底部
    scrollToBottom()
  }

}

// 发送消息（旧）
// const sendMessage = async (payload) => {
//   // 校验发送的消息不能为空
//   if (!message.value.trim()) return

//   console.log('选中的模型:', payload.selectedModel)
//   console.log('是否联网:', payload.isNetworkSearch)
//   console.log('是否开启RAG', payload.isRagSelected)

//   // 将用户发送的消息添加到 chatList 聊天列表中
//   const userMessage = message.value.trim()
//   chatList.value.push({ role: 'user', content: userMessage })

//   // 点击发送按钮后，清空输入框
//   message.value = ''

//   // 添加一个占位的回复消息
//   chatList.value.push({ role: 'assistant', content: '', loading:true})
                    
//   try {
//     // 构建请求体
//     const requestBody = {
//       message: userMessage,
//       chatId: chatId.value,
//       modelName: payload.selectedModel?.name,
//       networkSearch: payload.isNetworkSearch
//     }

//     // 响应的回答
//     let responseText = ''
//     // 获取最后一条消息
//     const lastMessage = chatList.value[chatList.value.length - 1]

//     controller = new AbortController()
//     const signal = controller.signal

//     fetchEventSource('http://localhost:8080/chat/completion', {
//       method: 'POST',
//       signal: signal,
//       headers: {
//         'Content-Type': 'application/json',
//       },
//       body: JSON.stringify(requestBody),
//       openWhenHidden: true, 
//       onmessage(msg) {
//         if (msg.event === '') {
//           //收到第一条消息后设置loading为false
//           if (lastMessage.loading) {
//             lastMessage.loading = false
//           }
//           // 解析 JSON
//           let parseJson = JSON.parse(msg.data)
//           // 持续追加流式回答
//           responseText += parseJson.v

//           // 更新最后一条消息
//           chatList.value[chatList.value.length - 1].content = responseText
//           // 滚动到底部
//           scrollToBottom()
//         }
//         else if (msg.event === 'close') {
//           console.log('-- sse close')
//           controller.abort();
//         }
//       },
//       onerror(err) {
//         throw err;    // 必须 throw 才能停止 
//       }
//     })


//   } catch (error) {
//     console.error('发送消息错误: ', error)
//     // 提示用户 “请求出错”
//     const lastMessage = chatList.value[chatList.value.length - 1]
//     lastMessage.loading = false
//     lastMessage.content = '抱歉，请求出错了，请稍后重试。'
//     // 滚动到底部
//     scrollToBottom()
//   }

// }

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick() // 等待 Vue.js 完成 DOM 更新
  if (chatContainer.value) { // 若容器存在
    // 将容器的滚动条位置设置到最底部
    const container = chatContainer.value;
    container.scrollTop = container.scrollHeight;
  }
}

// ---------------- 点击粒子效果实现 ----------------
const handleGlobalClick = (e) => {
  createParticles(e.clientX, e.clientY)
}

const createParticles = (x, y) => {
  const particleCount = 15 // 粒子数量
  for (let i = 0; i < particleCount; i++) {
    const particle = document.createElement('div')
    document.body.appendChild(particle)

    // 随机大小和颜色
    const size = Math.random() * 6 + 4
    particle.style.width = `${size}px`
    particle.style.height = `${size}px`
    particle.style.background = `hsl(${Math.random() * 360}, 80%, 60%)`
    particle.style.position = 'fixed'
    particle.style.left = `${x}px`
    particle.style.top = `${y}px`
    particle.style.borderRadius = '50%'
    particle.style.pointerEvents = 'none'
    particle.style.zIndex = '9999'

    // 随机散开距离
    const angle = Math.random() * Math.PI * 2
    const velocity = 30 + Math.random() * 60
    const tx = Math.cos(angle) * velocity
    const ty = Math.sin(angle) * velocity

    // Web Animations API 动画
    const animation = particle.animate([
      { transform: 'translate(-50%, -50%) scale(1)', opacity: 1 },
      { transform: `translate(calc(-50% + ${tx}px), calc(-50% + ${ty}px)) scale(0)`, opacity: 0 }
    ], {
      duration: 500 + Math.random() * 500,
      easing: 'cubic-bezier(0, .9, .57, 1)'
    })

    animation.onfinish = () => particle.remove()
  }
}

// 关闭 SSE 连接
const closeSSE = () => {
  if (controller) {
    controller.abort()
    controller = null
  }
}

// 组件卸载时自动关闭连接
onBeforeUnmount(() => {
  closeSSE()

  // 移除事件监听
  if (chatContainer.value) {
    chatContainer.value.removeEventListener('scroll', handleScroll);
    chatContainer.value.removeEventListener('click', handleGlobalClick);
  }
})
</script>

<style scoped>
.quesiton-container {
  font-size: 16px;
  line-height: 28px;
  color: #262626;
  padding: calc((44px - 28px) / 2) 20px;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: #eff6ff;
  border-radius: 14px;
  max-width: calc(100% - 48px);
  position: relative;
}

/* 聊天内容区域样式 */
.overflow-y-auto {
  scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
  /* 自定义滚动条颜色 */
}

</style>