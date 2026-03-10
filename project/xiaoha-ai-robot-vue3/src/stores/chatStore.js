import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {

    // 模型列表
    const models = ref([
        { id: 1, name: 'deepseek-v3', icon: 'deepseek-logo', description: '更流畅', selected: false},
        { id: 2, name: 'deepseek-r1', icon: 'deepseek-logo', description: '深度思考', selected: false},
        { id: 3, name: 'qwen3.5-plus', icon: 'qwen-logo', description: '超级模型', selected: false}
    ])

    // 选中的模型
    const selectedModel = ref(models.value[0]) // 默认选择第一个模型

    // 联网搜索状态，默认为否
    const isNetworkSearchSelected = ref(false)

    // 外部库状态， 默认为否
    const isRagSelected = ref(false)

    // 更新选中的模型
    function updateSelectedModel(model) {
        
        // 所有模型的selected置为false
        models.value.forEach(m => m.selected = false)
        
        // 选中模型selected置为true
        model.selected = true
        
        // 更新selectedModel
        selectedModel.value = model
    }

    // 更新联网搜索状态
    function updateNetworkSearchStatus(status) {
        isNetworkSearchSelected.value = status
    }

    // 更新外部库状态
    function updateRagStatus(status) {
        isRagSelected.value = status
    }

    return {models, selectedModel, isNetworkSearchSelected, isRagSelected, updateSelectedModel, updateNetworkSearchStatus, updateRagStatus};
},
{
    // 开启持久化
    persist: true
})