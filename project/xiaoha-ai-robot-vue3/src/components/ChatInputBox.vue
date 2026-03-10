<template>
  <div :class ="containerClass">
    <div class="bg-gray-100 rounded-3xl px-4 py-3 mx-4 border border-gray-200 flex flex-col transition-all duration-300 focus-within:bg-white focus-within:ring-4 focus-within:ring-amber-500/20 focus-within:border-amber-400 focus-within:shadow-lg">
        <textarea :placeholder= "!isRagSelected ? '给助手发送消息' : '检索本地知识库'"
            class="bg-transparent border-none outline-none w-full text-sm resize-none min-h-[24px]" rows="2"
            v-model="userMessage"
            @input="autoResize"
            @keydown.enter="handleEnter"
            ref="textareaRef">
        </textarea>

        <!-- 下方容器 -->
        <div class="flex mt-3">
            <div v-if="!isRagSelected"
              class="flex gap-2 relative" 
              ref="leftContainerRef">
                <!-- 模型下拉框 -->
                <div class="border border-gray-300 px-2 py-1 rounded-3xl flex items-center justify-center hover:bg-gray-200 cursor-pointer transition-colors duration-300"
                  ref="selectRef"
                  @click="toggleModelDropdown">                    
                    <SvgIcon :name="currSelectedModel.icon" customCss="w-5 h-5 mr-1.5" />
                    <span class="text-gray-800 text-xs">{{ currSelectedModel.name }}</span>
                    <SvgIcon name="down-arrow" customCss="w-5 h-5 ml-1 text-gray-800 transform transition-transform duration-300"
                    :class="isModelDropdownOpen ? 'rotate-180' : ''" />
                </div>
                <!-- 下拉框菜单 -->
                <div v-if="isModelDropdownOpen" 
                ref="dropdownRef"
                :class="['absolute', 'left-0', 'w-48', 'bg-white', 'rounded-lg', 'shadow-lg', 'border', 'border-gray-200', 'z-10', 'overflow-hidden', dropdownPosition]"
                >
                    <div v-for="model in models" :key="model.id" 
                    class="px-3 py-2 hover:bg-gray-100 cursor-pointer flex items-center justify-between"
                    @click="selectModel(model)">
                            <div class="flex items-center">
                            <SvgIcon :name="model.icon" customCss="w-5 h-5 mr-2" />
                            <div class="flex flex-col text-xs">
                                <div class="text-gray-800">{{ model.name }}</div>
                                <div class="text-gray-500">{{ model.description }}</div>
                            </div>
                        </div>
                        <!-- 右侧对号 -->
                        <SvgIcon v-if="model.selected" name="check" customCss="w-3 h-3 text-gray-600" />
                    </div>
                </div>
                <!-- 联网搜索 -->
                <div class="ml-1 border px-2 py-1 rounded-3xl flex items-center justify-center cursor-pointer transition-colors duration-300"
                :class="isNetworkSearchSelected ? 'border-[#ceddee] bg-[#DBEAFE] hover:bg-[#C3DAF8]' : 'border-gray-300 hover:bg-gray-200'" 
                @click="toggleNetworkSearch">
                    <SvgIcon name="network" customCss="w-5 h-5 mr-1.5" :class="isNetworkSearchSelected ? 'text-[#4D6BFE]' : 'text-gray-500'" />
                    <span class="text-xs mr-1" :class="isNetworkSearchSelected ? 'text-[#4D6BFE]' : 'text-gray-800'">联网搜索</span>
                </div>
                
            </div>
            <div class="grow"></div>

            <!-- 文件按钮 -->
            <div class="relative mr-2" ref="fileButtonRef">
              <!-- 浮动菜单容器（始终渲染，通过高度过渡动画展开/收起） -->
              <div class="absolute bottom-0 left-0 w-8 rounded-full flex flex-col items-center overflow-hidden transition-all duration-300 ease-in-out"
                :class="isFileDropdownOpen
                  ? 'bg-gray-200 border-none shadow-sm z-10'
                  : 'pointer-events-none'"
                :style="{ height: isFileDropdownOpen ? '128px' : '32px' }">
                <!-- 启用RAG -->
                <div class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-300 cursor-pointer transition-all duration-300"
                  :class="isFileDropdownOpen ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
                  :style="{ transitionDelay: isFileDropdownOpen ? '180ms' : '0ms' }"
                  title="启用知识库"
                  @click.stop ="toggleRagStatus">
                  <SvgIcon name="rag" customCss="w-5 h-5" />
                </div>
                <!-- 添加文件 -->
                <div class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-300 cursor-pointer transition-all duration-300"
                  :class="isFileDropdownOpen ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
                  :style="{ transitionDelay: isFileDropdownOpen ? '120ms' : '0ms' }"
                  title="添加文件"
                  @click.stop>
                  <SvgIcon name="add-file" customCss="w-5 h-5" />
                </div>
                <!-- 管理文件 -->
                <div class="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-300 cursor-pointer transition-all duration-300"
                  :class="isFileDropdownOpen ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'"
                  :style="{ transitionDelay: isFileDropdownOpen ? '60ms' : '0ms' }"
                  title="管理文件"
                  @click.stop>
                  <SvgIcon name="manage-file" customCss="w-5 h-5" />
                </div>
              </div>
              <!-- 文件按钮（始终在文档流中，固定大小，z-20 保证在菜单之上） -->
              <div class="relative z-20 w-8 h-8 rounded-full flex items-center justify-center cursor-pointer hover:bg-gray-200 transition-colors duration-300"
                @click.stop="toggleFileDropdown"
                >
                <SvgIcon :name="isRagSelected ? 'file-selected' : 'file'" customCss="w-5 h-5" />
              </div>
            </div>

            <!-- 发送按钮-->
            <a-tooltip placement="top">
              <!-- tooltip 提示文字 -->
              <template #title>
                <span>请输入你的问题</span>
              </template>
              <!-- 发送按钮 -->
              <button class="flex items-center justify-center bg-amber-500 rounded-full w-8 h-8 border border-amber-500 hover:bg-amber-600 transition-colors
                    disabled:opacity-50
                    disabled:cursor-not-allowed"
                    :disabled="!userMessage.trim()"
                    @click="handleSendMessage"
                    >
                <SvgIcon name="up-arrow" customCss="w-5 h-5 text-white"></SvgIcon>
              </button>
            </a-tooltip>
        </div>
    </div>
  </div>
</template>

<script setup>
import SvgIcon from '@/components/SvgIcon.vue'
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'          
import { useChatStore } from '@/stores/chatStore'

// props and emits

const props = defineProps({ // 接收父组件传递的属性
  // textarea 中用户输入的用户消息
  modelValue: {
    type: String,
    required: true
  },
  // 自定义根容器样式
  containerClass: { 
    type: String,
    default: ''
  },
})
// 定义 emits
const emit = defineEmits(['update:modelValue', 'sendMessage'])

// store

// 获取 chat store
const chatStore = useChatStore()

// computed

// 模型列表
const models = computed(() => chatStore.models)
// 当前选择的模型，使用 store 中的选中模型
const currSelectedModel = computed(() => chatStore.selectedModel)
// 是否启用联网搜索，使用 store 中的状态
const isNetworkSearchSelected = computed(() => chatStore.isNetworkSearchSelected)
// 是否使用 RAG
const isRagSelected = computed(() => chatStore.isRagSelected)
// 计算属性，用于 v-model 的双向绑定
const userMessage = computed({
  get() {
    return props.modelValue;
  },
  set(value) {
    emit('update:modelValue', value);
  }
})

// ref

// 下拉框容器引用
const selectRef = ref(null)
// 下拉菜单状态
const isModelDropdownOpen = ref(false)
// 文本域引用
const textareaRef = ref(null)
// 下拉菜单父容器引用
const leftContainerRef = ref(null)
// 下拉菜单引用
const dropdownRef = ref(null)
// 下拉菜单位置
const dropdownPosition = ref('top-8')
// 文件上传按钮容器引用
const fileButtonRef = ref(null)
// 下拉文件状态
const isFileDropdownOpen = ref(false)

// handlers

// 下拉菜单显示/隐藏
const toggleModelDropdown = () => {
  isModelDropdownOpen.value = !isModelDropdownOpen.value

  // 如果是显示，则计算位置
  if (isModelDropdownOpen.value) {
    nextTick(() => {
      if (leftContainerRef.value && dropdownRef.value) {
        // 元素相对于视图窗口的位置和尺寸信息
        const buttonRect = leftContainerRef.value.getBoundingClientRect();
        // 下拉菜单实际高度
        const dropdownHeight = dropdownRef.value.offsetHeight;
        // 检查空间是否足够
        // 可用空间 = window.innerHeight - buttonRect.bottom
        // 所需空间 = dropdownHeight
        // 如果可用空间 < 所需空间，说明下方空间不足
        if (window.innerHeight - buttonRect.bottom < dropdownHeight) {
          dropdownPosition.value = 'bottom-8';
        } else {
          dropdownPosition.value = 'top-8';
        }
      }
    })
  }
}
// 点击外部区域关闭下拉菜单
const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isModelDropdownOpen.value = false
  }
  if (fileButtonRef.value && !fileButtonRef.value.contains(event.target)) {
    isFileDropdownOpen.value = false
  }
}
// 切换文件上传下拉菜单
const toggleFileDropdown = () => {
  isFileDropdownOpen.value = !isFileDropdownOpen.value
}
// 切换 RAG 状态
const toggleRagStatus = () => {
  chatStore.updateRagStatus(!isRagSelected.value)
}
// 选择模型
const selectModel = (model) => {
  // 更新 store 中的选中模型
  chatStore.updateSelectedModel(model)
  // 关闭下拉菜单
  isModelDropdownOpen.value = false;
}
// 切换联网搜索选中状态
const toggleNetworkSearch = () => {
    // 更新 store 中的联网搜索状态
  chatStore.updateNetworkSearchStatus(!chatStore.isNetworkSearchSelected)
}
// 处理发送消息
const handleSendMessage = () => {
  // 若消息为空
  if (!userMessage.value.trim()) {
    message.warning('消息不能为空');
    return
  }

  emit('sendMessage', {
    selectedModel: chatStore.selectedModel,
    isNetworkSearch: chatStore.isNetworkSearchSelected,
    isRagSelected: chatStore.isRagSelected
  });
  // 清空输入框
  userMessage.value = '';
}
// 自动调整文本域高度
const autoResize = () => {
  const textarea = textareaRef.value;
  if (textarea) {
    // 重置高度以获取正确的滚动高度
    textarea.style.height = 'auto'
    
    // 计算新高度，但最大不超过 300px
    const newHeight = Math.min(textarea.scrollHeight, 300);
    textarea.style.height = newHeight + 'px';
    
    // 如果内容超出 300px，则启用滚动
    textarea.style.overflowY = textarea.scrollHeight > 300 ? 'auto' : 'hidden';
  }
}
// 监听回车事件
const handleEnter = (event) => {
  console.log('按下回车键', event.target.value)
  // 主动调用发送消息方法
  handleSendMessage()
  autoResize()
}
// lifescycle

// 挂载时添加事件监听器
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

// 卸载时移除事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

</script>

<style scoped>

</style>
