import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const currentWarehouse = ref<string | null>(null)

  return { currentWarehouse }
})
