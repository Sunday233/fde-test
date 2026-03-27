import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'baseline',
          name: 'Baseline',
          component: () => import('@/views/BaselineView.vue'),
        },
        {
          path: 'impact',
          name: 'Impact',
          component: () => import('@/views/ImpactView.vue'),
        },
        {
          path: 'estimate',
          name: 'Estimate',
          component: () => import('@/views/EstimateView.vue'),
        },
        {
          path: 'report',
          name: 'Report',
          component: () => import('@/views/ReportView.vue'),
        },
      ],
    },
  ],
})

export default router
