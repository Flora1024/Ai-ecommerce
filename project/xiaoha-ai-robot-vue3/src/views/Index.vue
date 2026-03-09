<template>
  <Layout>
    <!-- 主内容区域 -->
    <template #main-content>
      <div class="flex items-center justify-center flex-1 relative bg-gradient-to-t from-white via-yellow-30 to-amber-100">
        <div class="max-w-3xl w-full relative">
          <!-- 悬浮的示例问题 -->
          <div class="absolute -top-20 left-0 right-0 flex justify-center gap-4">
            <span
              v-for="(text, index) in sampleQuestions"
              :key="index"
              class="px-4 py-2 bg-white/80 backdrop-blur rounded-full text-sm text-gray-600 shadow-sm border border-gray-100 cursor-pointer hover:bg-white transition-colors select-none"
              @click="copyToClipboard(text)"
            >
              {{ text }}
            </span>
          </div>
          <div class="text-center mb-10">
            <div class="flex items-end justify-center mb-3">
              <SvgIcon name="ecommerce" customCss="w-10 h-10 text-gray-700 mr-3" />
              <h2 class="text-2xl text-gray-800">您的智能电商助手 ✧*｡٩(ˊᗜˋ*)و✧*｡</h2>
            </div>
            <p class="text-gray-500">可以帮您解答各类电商场景疑问，也可以帮您分析电商订单数据，试着向我提问吧~</p>
          </div>

          <!-- 聊天输入框 -->
          <ChatInputBox 
          v-model="userMessage"
          @sendMessage="sendMessage"
          />

        </div>
      </div>
    </template>
  </Layout>
</template>

<script setup>

import Layout from '@/layouts/Layout.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import ChatInputBox from '@/components/ChatInputBox.vue'
import { ref, watch } from 'vue'
import { newChat } from '@/api/chat'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const router = useRouter()
// 用户输入的消息
const userMessage = ref('')

// 示例问题列表
const sampleQuestions = [
  '哪种洗面奶适合出油多的肤质？',
  '女生夏天适合穿什么衣服？',
  '适合男士使用的香水有哪些？'
]

// 复制文本到剪贴板
const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('复制成功')
  } catch (err) {
    console.error('复制失败:', err)
    message.error('复制失败')
  }
}

// 侦听属性，确认子组件中 textarea 中变更的消息，是否能够传递给父组件
watch(userMessage, (newText) => {
  console.log(`子组件传递的新值: ${newText}`)
})

// 发送消息 - 跳转到对话聊天页并发送消息
const sendMessage = (payload) => {
  if (!userMessage.value.trim()) return;

  console.log('选中的模型: ', payload.selectedModel);
  console.log('是否联网搜索: ', payload.isNetworkSearch);
  
  // 临时保存消息的值，因为子组件中的 userMessage 会被清空
  const userMessageTemp = userMessage.value.trim();
  console.log('用户发送的消息: ' + userMessageTemp)

  // 请求对话新建接口
  newChat(userMessageTemp).then(res => {
    if (res.data.success) {
        // 跳转到聊天对话页面
        router.push({
          name: 'ChatPage', // 必须使用命名路由来跳转
          params: {
            chatId: res.data.data.uuid // Url 中的 UUID
          },
          state: {
            firstMessage: userMessageTemp // 将用户在首页填入的消息，传递给 “聊天对话页”
          }
        })
    }
  })

}

</script>
