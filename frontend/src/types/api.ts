/** 后端统一响应包装 */
export interface Result<T> {
  code: number
  message: string
  data: T
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ─── Dashboard ───

export interface DashboardOverviewVO {
  totalOrders: number
  totalItems: number
  totalWorkHours: number
  monthlyFee: number
  dailyAvgFee: number
  laborEfficiency: number
  avgCostPerOrder: number
  avgCostPerItem: number
  avgHeadcount: number
  workDays: number
  itemsPerOrder: number
}

export interface TrendDataVO {
  date: string
  warehouseCode: string
  warehouseName: string
  value: number
  type: string
}

// ─── Baseline ───

export interface MonthlyBaselineVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number
  totalFee: number
  totalOrders: number
  totalItems: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
  totalWorkHours: number
  weightedUnitPrice: number
  itemsPerOrder: number
  dailyAvgOrders: number
  fixedTempRatio: number
  laborEfficiency: number
  warehouseType: string
}

export interface WarehouseDetailVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number
  totalFee: number
  totalOrders: number
  totalItems: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
  totalWorkHours: number
  weightedUnitPrice: number
}

export interface CompareResultVO {
  date: string
  year: number
  month: number
  warehouseCode: string
  warehouseName: string
  totalFee: number
  totalOrders: number
  avgHeadcount: number
  dailyAvgFee: number
  costPerOrder: number
  costPerItem: number
  laborEfficiency: number
  totalWorkHours: number
}

export interface DailyDetailVO {
  date: string
  warehouseCode: string
  warehouseName: string
  obOrders: number
  obItems: number
  itemOrderRatio: number
  headcount: number
  workHours: number
  dailyFee: number
}

export interface OperationFeeDetailVO {
  dimension: 'day' | 'month'
  date?: string
  year: number
  month: number
  monthLabel: string
  warehouseCode: string
  warehouseName: string
  operationCategory: string
  operationCount: number
  feeRatio: number
  estimatedFee: number
}

// ─── Impact ───

export interface FactorRankVO {
  rank: number
  factorName: string
  correlation: number
  description: string
  tier: number
  tierLabel: string
  applicationTip: string
}

export interface CorrelationMatrixVO {
  factors: string[]
  matrix: number[][]
}

export interface ScatterPointVO {
  date: string
  factorValue: number
  workHours: number
}

// ─── Estimate ───

export interface EstimateRequest {
  dailyOrders: number
  itemsPerOrder: number
  workDays: number
  laborEfficiency: number
  fixedLaborPrice: number
  tempLaborPrice: number
  forkliftLaborPrice: number
  fixedLaborRatio: number
  tempLaborRatio: number
  forkliftLaborRatio: number
  hoursPerDay: number
  itemsPerOrderBaseline: number
  enableItemCorrection: boolean
  taxRate: number
}

export interface EstimateResultVO {
  estimatedHeadcount: number
  estimatedTotalHours: number
  weightedUnitPrice: number
  monthlyFee: number
  dailyFee: number
  costPerOrder: number
  costPerItem: number
  itemCorrectionFactor: number
  baselineDailyFee: number
  baselineDeviation: number
}

// ─── Report ───

export interface ReportGenerateRequest {
  warehouseCode: string
  startMonth: string
  endMonth: string
}

export interface ReportVO {
  id: string
  title: string
  warehouseCode: string
  warehouseName: string
  startMonth: string
  endMonth: string
  createdAt: string
  content?: string
}

// ─── Warehouse ───

export interface WarehouseVO {
  warehouseCode: string
  warehouseName: string
}
