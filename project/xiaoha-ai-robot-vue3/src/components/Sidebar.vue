<template>
    <!-- 左边栏 -->
    <div 
        :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
        class="w-64 bg-[#f7f6e9] border-r border-gray-200 fixed left-0 top-0 h-full transition-transform duration-300 ease-in-out z-10 overflow-y-auto shadow-2xl">
        <!-- 侧边栏内容区域 -->
        <div class="p-0 h-full flex flex-col">
            <!-- Logo 与应用名称 -->
            <div class="flex items-end justify-center p-4 cursor-pointer">
              <SvgIcon name="ecommerce" customCss="w-9 h-9 text-gray-700 mr-3" />
              <span class="text-2xl font-bold font-sans tracking-wide text-gray-800">电商助手</span>
            </div>
            <!-- 开启新对话按钮 -->
            <button class="mx-auto mb-[34px] my-2 px-6 py-2 text-white rounded-xl transition-colors new-chat-btn w-50 cursor-pointer"
            @click="jumpToChatPage('index')">
              <SvgIcon name="new-chat" customCss="w-5 h-5 mr-1.5 inline text-gray-600" />
              开启新对话
            </button>
            <!-- 历史对话区域 -->
            <div class="my-4 px-2 overflow-y-auto overflow-x-hidden flex-1" ref="historyChatContainerRef">
              <div class="space-y-1">
                <div class="text-xs px-3 py-1 text-gray-500 sticky top-0 z-10">对话历史</div>
                
                <div v-for="(historyChat, index) in historyChats" :key="index" 
                :class="['relative px-3 py-1 rounded-xl hover:bg-[rgb(239,246,255)] cursor-pointer transition-colors flex items-center justify-between',
                          selectedChatId === historyChat.uuid ? 'bg-[#e4edfd] text-[#4d6bfe]' : 'hover:bg-[rgb(239,246,255)]']"
                @click="jumpToChatPage(historyChat.uuid)"
                @mouseenter="showButton = historyChat.uuid" 
                @mouseleave="showButton = null"
                >
                    <p class="text-[14px] overflow-hidden whitespace-nowrap">{{ historyChat.summary }}</p>
                    <!-- 下拉菜单 -->
                    <a-dropdown>
                         <template #overlay>
                            <a-menu @click="handleMenuClick(historyChat.uuid, historyChat.id, historyChat.summary, $event)">
                              <a-menu-item key="rename">
                                <EditOutlined />
                                重命名
                              </a-menu-item>
                              <a-menu-item key="delete" danger>
                                <DeleteOutlined />
                                删除
                              </a-menu-item>
                          </a-menu>
                        </template>
                        <!-- 右边菜单按钮 -->
                        <button
                            class="z-10 rounded-lg outline-none justify-center items-center bg-white
                            w-6 h-6 flex absolute right-2 top-1/2 transform -translate-y-1/2 transition-all duration-300 hover:bg-gray-50"
                            :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }"
                            @click.stop
                        >
                            <EllipsisOutlined class="w-4 h-4 text-gray-500" />
                        </button>
                    </a-dropdown>
                </div>
              
              </div>
            </div>
        </div>
    </div>
    <!-- 侧边栏切换按钮 -->
    <a-tooltip placement="bottom">
        <!-- Tooltip 提示文字 -->
        <template #title>
          <span>{{ sidebarOpen ? '收缩边栏' : '打开边栏'}}</span>
        </template>

        <button 
          :class="sidebarOpen ? 'left-64' : 'left-0'"
          @click="toggleSidebar"
          class="fixed top-4 z-20 bg-white border border-gray-200 border-l-0 rounded-r-full shadow-md p-2 pr-3 transition-all duration-300 flex items-center justify-center">
            <!-- 图标 -->
            <SvgIcon :name="sidebarOpen ? 'arrow-left' : 'arrow-right'" :customCss="sidebarOpen ? 'w-6 h-6 text-gray-400' : 'w-7 h-7 text-gray-400'" />
        </button>
    </a-tooltip>
    <!-- 确认删除弹窗 -->
    <a-modal v-model:open="deleteChatModelOpen" width="400px" :centered="true" title="永久删除对话" ok-text="确认" ok-type="danger" cancel-text="取消" 
    @ok="handleDeleteChatModelOk()">
      <p>确定要永久删除该对话吗？删除后将无法恢复!</p>
    </a-modal>
    <!-- 重命名对话弹窗 -->
    <a-modal v-model:open="renameChatModelOpen" width="400px" :centered="true" title="重命名对话" ok-text="确认" cancel-text="取消"
    @ok="handleRenameChatModelOk()">
      <!-- 表单对象  -->
      <a-form
        ref="formRef"
        :rules="rules"
        :model="formState"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
        autocomplete="off"
        >
          <a-form-item
            label="摘要: "
            name="summary"
          >
            <a-input v-model:value="formState.summary" />
          </a-form-item>
      </a-form>
    </a-modal>

</template>

<script setup>
import SvgIcon from '@/components/SvgIcon.vue'
import { EditOutlined, EllipsisOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { ref, onMounted, onBeforeUnmount, reactive, toRaw } from 'vue'
// 导入获取历史对话列表的API
import { findHistoryChatPageList, deleteChat, renameChat } from '@/api/chat'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'

// 路由实例
const router = useRouter()
// 路由参数
const route = useRoute()
// 当前显示右侧栏按钮的聊天 ID
const showButton = ref(null)
// 当前选中的聊天 ID
const selectedChatId = ref(null);
// 分页状态
const current = ref(1)
const size = ref(20)
// 左边栏滚动区域引用
const historyChatContainerRef = ref(null)
// 历史对话
const historyChats = ref([])
// 是否存在下一页数据
const hasMore = ref(true)
// 是否正在加载中
const isLoadingMore = ref(false)
// 删除对话弹窗是否展示
const deleteChatModelOpen = ref(false)
// 被删除对话的 UUID
const deleteChatUuid = ref(null)
// 重命名对话弹窗是否显示
const renameChatModelOpen = ref(false)
// 重命名弹窗表单引用
const formRef = ref(null)
// 校验规则
const rules = {
  summary: [
    {
      required: true,
      message: '对话摘要不能为空',
      trigger: 'change'
    }
  ]
}
// 表单对象
const formState = reactive({
  id: null,
  summary: ''
})

// 定义 props, 对外部暴露配置项
const props = defineProps({
  sidebarOpen: { type: Boolean, required: true }, // 左边栏是否展开
})
// 定义 emits
const emit = defineEmits(['toggle-sidebar'])

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  emit('toggle-sidebar')
}
// 跳转历史对话页
const jumpToChatPage = (chatId) => {
  // console.log('跳转到历史对话页, chatId: ', chatId)
  if (chatId === 'index') {
    selectedChatId.value = null
    router.push({ name: 'Index'})
    return;
  }
  selectedChatId.value = chatId
  router.push({ name: 'ChatPage', params: { chatId } })
}
// 处理滚动事件
const handleScroll = () => {
  if (historyChatContainerRef.value) {
    // 获取滚动参数
    const { scrollTop, scrollHeight, clientHeight } = historyChatContainerRef.value;
    // 距离底部位置
    const scrollBottom = scrollHeight - scrollTop - clientHeight;

    // 打印滚动位置
    // console.log('=== 滚动事件日志 ===')
    // console.log('scrollPosition:', scrollBottom)

    // 距离底部20px / 有下一页数据 / 非加载中
    if (scrollBottom <= 20 && hasMore.value && !isLoadingMore.value) {
      console.log('触发加载更多历史会话...')
      // 加载更多历史会话
      loadMoreHistoryChats()
    }
  }
}
// 加载历史会话
const loadMoreHistoryChats = async () => {
  if (!hasMore.value) {
    console.log('没有更多历史会话可加载')
    return;
  }
  if (isLoadingMore.value) {
    console.log('正在加载中，请稍后...')
    return;
  }
  console.log('开始加载历史会话...')
  isLoadingMore.value = true

  const nextPageNo = current.value + 1
  const currentTemp = current.value
  current.value = nextPageNo

  try {
    await fetchHistoryChats()
  } catch (error) {
    // 恢复页码
    current.value = currentTemp
  }
}
// 获取历史对话列表
const fetchHistoryChats = () => {
  findHistoryChatPageList(current.value, size.value).then((res) => {
    // 请求成功后更新加载状态
    isLoadingMore.value = false

    if (res.data.success) {
      const data = res.data.data
      // 判断是否还有下一页
      hasMore.value = res.data.pages > current.value
      // 追加历史对话
      if (data && data.length > 0) {
        historyChats.value = [...historyChats.value, ...data]
      }
    }
  }).catch((error) => {
    console.error('加载历史会话失败: ', error)
    isLoadingMore.value = false
  })
}
// 处理菜单点击事件
const handleMenuClick = (chatId, id, summary, e) => {
  if (e.key === 'delete') {
    // 显示对话框
    deleteChatModelOpen.value = true
    // 保留被删除对话的 UUID
    deleteChatUuid.value = chatId
  } else if (e.key === 'rename') {
    // 显示对话框
    renameChatModelOpen.value = true
    // 原对象数据
    formState.id = id
    formState.summary = summary
  }
}
// 处理删除对话弹窗确认事件
const handleDeleteChatModelOk = () => {
  deleteChat(deleteChatUuid.value).then((res) => {
    if (res.data.success) {
      message.success('对话删除成功')
      // 从历史对话列表中移除该对话
      const index = historyChats.value.findIndex(chat => chat.uuid === deleteChatUuid.value)
      if (index !== -1) {
        historyChats.value.splice(index, 1)
      }
      // 跳转首页
      if (deleteChatUuid.value === selectedChatId.value) {
        router.push({ name: 'Index'})
      }
    } else {
      message.error(res.data.message)
    }
  }).finally(
    // 隐藏弹窗
    deleteChatModelOpen.value = false
  )
}
// 处理重命名对话弹窗确认事件
const handleRenameChatModelOk = () => {
  formRef.value
    .validate()
    .then(() => { // 校验通过
      console.log('提交了')
      console.log('values', formState, toRaw(formState))
      renameChat(formState.id, formState.summary).then((res) => {
        if (!res.data.success) {
          message.error(res.data.message)
          return
        }
        message.success('对话重命名成功')
        // 更新历史对话列表中的摘要
        const chatIndex = historyChats.value.findIndex(chat => chat.id == formState.id)
        if (chatIndex !== -1) {
          historyChats.value[chatIndex].summary = formState.summary
        }
        // 关闭弹窗
        renameChatModelOpen.value = false
      })
    })
    .catch(error => {
      console.log('error:', error)
    })
}

// onMounted
onMounted(() => {
  // 查询历史对话列表
  fetchHistoryChats()
  // 检查当前路由参数中是否有 chatId
  const currentChatId = route.params.chatId
  if (currentChatId) {
    selectedChatId.value = currentChatId
  }
  // 添加滚动监听事件
  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.addEventListener('scroll', handleScroll)
  }
})
// onBeforeUnmount
onBeforeUnmount(() => {
  // 移除滚动监听事件
  if (historyChatContainerRef.value) {
    historyChatContainerRef.value.removeEventListener('scroll', handleScroll)
  }
})

</script>

<style scoped>
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent; /* 自定义滚动条颜色 */
}

.new-chat-btn {
  background-color: #e5e7eb; /* 亮银色 (Tailwind gray-200) */
  color: #374151; /* 深灰色文字 (Tailwind gray-700) */
  border: 1px solid #d1d5db; /* 浅灰边框 */
}

.new-chat-btn:hover {
  background-color: #d1d5db; /* 悬停稍微变深 */
}
</style>
