<template>
  <div ref="chatContainer">
    <!-- 聊天记录 -->
    <div>
      <template v-for="(chat, index) in chatList" :key="index">
        <!-- 用户消息 -->
        <div v-if="chat.role === 'user'">
          <p>{{ chat.content }}</p>
        </div>
        <!-- AI 回复 -->
        <div v-else>
          <p>{{ chat.content }}</p>
        </div>
      </template>
    </div>

    <!-- 输入区域 -->
    <div>
      <textarea
        v-model="message"
        placeholder="输入消息"
        @input="autoResize"
        @keydown.enter.prevent="sendMessage"
        ref="textareaRef"
      ></textarea>
      <button @click="sendMessage" :disabled="!message.trim()">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'

// 引用
const textareaRef = ref(null)
const chatContainer = ref(null)
const message = ref('')
const chatList = ref([
  { role: 'assistant', content: '你好，我是 AI 助手！' }
])

let eventSource = null

// 自动调整 textarea 高度
const autoResize = () => {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    const newHeight = Math.min(textarea.scrollHeight, 300)
    textarea.style.height = newHeight + 'px'
    textarea.style.overflowY = textarea.scrollHeight > 300 ? 'auto' : 'hidden'
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 发送消息
const sendMessage = () => {
  if (!message.value.trim()) return

  const userMessage = message.value.trim()
  chatList.value.push({ role: 'user', content: userMessage })
  message.value = ''
  if (textareaRef.value) textareaRef.value.style.height = 'auto'
  chatList.value.push({ role: 'assistant', content: '' })

  try {
    eventSource = new EventSource(
      `http://localhost:8080/mcp/ai/generateStream?message=${encodeURIComponent(userMessage)}&chatId=1`
    )

    let responseText = ''
    eventSource.onmessage = (event) => {
      if (event.data) {
        let response = JSON.parse(event.data)
        responseText += response.v
        chatList.value[chatList.value.length - 1].content = responseText
        scrollToBottom()
      }
    }

    eventSource.onerror = (error) => {
      if (error.eventPhase === EventSource.CLOSED) {
        console.log('SSE 正常关闭')
      } else {
        chatList.value[chatList.value.length - 1].content = '请求出错，请稍后重试'
      }
      closeSSE()
      scrollToBottom()
    }
  } catch (error) {
    console.error('发送消息错误: ', error)
    chatList.value[chatList.value.length - 1].content = '请求出错，请稍后重试'
    scrollToBottom()
  }
}

// 关闭 SSE
const closeSSE = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

onBeforeUnmount(() => {
  closeSSE()
})
</script>

<style scoped>
textarea {
  width: 100%;
  min-height: 24px;
  resize: none;
}
</style>
