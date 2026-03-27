import axios from 'axios'
import { message } from 'ant-design-vue'
import type {
  Result,
  PageResult,
  DashboardOverviewVO,
  TrendDataVO,
  MonthlyBaselineVO,
  WarehouseDetailVO,
  CompareResultVO,
  DailyDetailVO,
  OperationFeeDetailVO,
  FactorRankVO,
  CorrelationMatrixVO,
  ScatterPointVO,
  EstimateRequest,
  EstimateResultVO,
  ReportGenerateRequest,
  ReportVO,
  WarehouseVO,
} from '@/types/api'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 响应拦截器：统一处理 Result<T> 解包
http.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code === 200) {
      return result.data as never
    }
    message.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  },
  (error) => {
    if (axios.isCancel(error)) {
      // 请求被 AbortController 取消，静默处理
      return Promise.reject(error)
    }
    if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请检查网络连接')
    } else if (error.response && error.response.status >= 500) {
      message.error('服务器繁忙，请稍后重试')
    } else {
      message.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  },
)

// ─── Dashboard ───

export function getOverview(warehouseCode: string, month: string, signal?: AbortSignal) {
  return http.get<never, DashboardOverviewVO>('/dashboard/overview', {
    params: { warehouseCode, month },
    signal,
  })
}

export function getTrend(warehouseCode: string, startMonth: string, endMonth: string, type: string, signal?: AbortSignal) {
  return http.get<never, TrendDataVO[]>('/dashboard/trend', {
    params: { warehouseCode, startMonth, endMonth, type },
    signal,
  })
}

// ─── Baseline ───

export function getMonthlyBaseline(warehouseCode?: string, startMonth?: string, endMonth?: string, page?: number, size?: number, signal?: AbortSignal) {
  return http.get<never, MonthlyBaselineVO[] | PageResult<MonthlyBaselineVO>>('/baseline/monthly', {
    params: { warehouseCode, startMonth, endMonth, page, size },
    signal,
  })
}

export function getLatestBaselineMonth(signal?: AbortSignal) {
  return http.get<never, string>('/baseline/latestMonth', { signal })
}

export function getWarehouseDetail(warehouseCode: string, year?: number, month?: number) {
  return http.get<never, WarehouseDetailVO>(`/baseline/warehouse/${warehouseCode}`, {
    params: { year, month },
  })
}

export function compareWarehouses(codes: string[], startMonth?: string, endMonth?: string, granularity: 'day' | 'month' = 'month') {
  return http.get<never, CompareResultVO[]>('/baseline/compare', {
    params: { codes: codes.join(','), startMonth, endMonth, granularity },
  })
}

export function getDailyDetail(warehouseCode?: string, startMonth?: string, endMonth?: string, signal?: AbortSignal) {
  return http.get<never, DailyDetailVO[]>('/baseline/daily-detail', {
    params: { warehouseCode, startMonth, endMonth },
    signal,
  })
}

export function getOperationFeeDetail(warehouseCode?: string, startMonth?: string, endMonth?: string, dimension: 'day' | 'month' = 'month', signal?: AbortSignal) {
  return http.get<never, OperationFeeDetailVO[]>('/baseline/operation-fee-detail', {
    params: { warehouseCode, startMonth, endMonth, dimension },
    signal,
  })
}

// ─── Impact ───

export function getFactors(warehouseCode: string, signal?: AbortSignal) {
  return http.get<never, FactorRankVO[]>('/impact/factors', {
    params: { warehouseCode },
    signal,
  })
}

export function getCorrelation(warehouseCode: string, signal?: AbortSignal) {
  return http.get<never, CorrelationMatrixVO>('/impact/correlation', {
    params: { warehouseCode },
    signal,
  })
}

export function getScatter(warehouseCode: string, factor: string, signal?: AbortSignal) {
  return http.get<never, ScatterPointVO[]>('/impact/scatter', {
    params: { warehouseCode, factor },
    signal,
  })
}

// ─── Estimate ───

export function calculate(request: EstimateRequest) {
  return http.post<never, EstimateResultVO>('/estimate/calculate', request)
}

export function getEstimateDefaults(warehouseCode: string) {
  return http.get<never, EstimateRequest>(`/estimate/defaults/${warehouseCode}`)
}

// ─── Report ───

export function generateReport(request: ReportGenerateRequest) {
  return http.post<never, ReportVO>('/report/generate', request)
}

export function getReportList(page?: number, size?: number, signal?: AbortSignal) {
  return http.get<never, ReportVO[] | PageResult<ReportVO>>('/report/list', {
    params: { page, size },
    signal,
  })
}

export function getReportDetail(id: string) {
  return http.get<never, ReportVO>(`/report/${id}`)
}

// ─── Warehouse ───

export function getWarehouses() {
  return http.get<never, WarehouseVO[]>('/warehouses')
}

export function getAvailableMonths() {
  return http.get<never, string[]>('/available-months')
}

// ─── AbortController 辅助 ───

export function useAbortController() {
  let controller = new AbortController()

  function getSignal() {
    controller = new AbortController()
    return controller.signal
  }

  function abort() {
    controller.abort()
  }

  return { getSignal, abort }
}
