<template>
  <div class="payment-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-xl font-bold text-gray-800">打款批次管理（财务端）</span>
          <el-button type="primary" @click="openCreateBatch" class="rounded-lg">
            <el-icon><Plus /></el-icon>
            创建打款批次
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="mb-6">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 180px" class="rounded-lg">
            <el-option label="待打款" value="pending" />
            <el-option label="打款中" value="processing" />
            <el-option label="已完成" value="completed" />
            <el-option label="打款失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="打款日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="rounded-lg">查询</el-button>
          <el-button @click="handleReset" class="rounded-lg">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border class="rounded-lg overflow-hidden" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="batchNo" label="批次号" min-width="200" />
        <el-table-column prop="batchName" label="批次名称" min-width="180" />
        <el-table-column prop="totalCount" label="笔数" width="100" />
        <el-table-column prop="totalAmount" label="总金额" width="140">
          <template #default="{ row }">
            <span class="font-bold text-orange-600">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentDate" label="预计打款日" width="120" />
        <el-table-column prop="actualPaymentDate" label="实际打款日" width="120" />
        <el-table-column prop="paymentChannel" label="打款渠道" width="100">
          <template #default="{ row }">{{ getChannelText(row.paymentChannel) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getBatchStatusType(row.status)" class="rounded-full px-3 py-1">
              {{ getBatchStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewBatchDetail(row)">查看明细</el-button>
            <el-button v-if="row.status === 'pending'" type="success" size="small" link @click="markBatchPaid(row)">标记已打款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-6 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="rounded-lg"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="createVisible" title="创建打款批次" width="900px" class="rounded-lg" :close-on-click-modal="false">
      <el-form :model="batchForm" :rules="batchRules" ref="batchFormRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="批次名称" prop="batchName">
              <el-input v-model="batchForm.batchName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="打款渠道" prop="paymentChannel">
              <el-select v-model="batchForm.paymentChannel" class="w-full">
                <el-option label="银行转账" value="bank" />
                <el-option label="支付宝" value="alipay" />
                <el-option label="微信" value="wechat" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计打款日" prop="paymentDate">
              <el-date-picker
                v-model="batchForm.paymentDate"
                type="date"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人">
              <el-input v-model="batchForm.operatorName" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="batchForm.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="选择结算单" prop="settlementIds">
          <div class="w-full border rounded-lg p-4 bg-gray-50">
            <div class="flex justify-between items-center mb-3">
              <div class="text-sm text-gray-600">
                已选 <span class="font-bold text-blue-600">{{ selectedSettlements.length }}</span> 笔，
                合计金额：<span class="font-bold text-orange-600">¥{{ selectedTotalAmount.toFixed(2) }}</span>
              </div>
              <el-button type="primary" size="small" @click="refreshAvailableSettlements" class="rounded-lg">
                刷新可选结算单
              </el-button>
            </div>
            <el-table
              :data="availableSettlements"
              border
              max-height="300"
              class="text-sm"
              @selection-change="handleSettlementSelection"
              :header-cell-style="{ background: '#eef2ff', color: '#606266' }"
            >
              <el-table-column type="selection" width="50" :reserve-selection="true" />
              <el-table-column prop="settlement.settlementNo" label="结算单号" width="180" />
              <el-table-column label="工人" width="140">
                <template #default="{ row }">{{ row.worker?.workerName }} ({{ row.worker?.workerNo }})</template>
              </el-table-column>
              <el-table-column prop="settlement.workDate" label="工作日期" width="110" />
              <el-table-column label="结算工时" width="100">
                <template #default="{ row }">{{ row.settlement.actualHours }}h</template>
              </el-table-column>
              <el-table-column label="应发金额" width="120">
                <template #default="{ row }">
                  <span class="font-bold text-orange-600">¥{{ row.settlement.totalAmount }}</span>
                </template>
              </el-table-column>
              <el-table-column label="争议原因" min-width="200">
                <template #default="{ row }">
                  <el-tag v-if="row.dispute" type="warning" effect="light" size="small">有争议</el-tag>
                  <span v-if="row.dispute" class="ml-2 text-sm text-red-600">{{ row.dispute.disputeReason }}</span>
                  <span v-else class="text-gray-400 text-sm">正常结算</span>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="150">
                <template #default="{ row }">
                  <span class="text-gray-500 text-sm">{{ row.settlement.remark || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false" class="rounded-lg">取消</el-button>
        <el-button type="primary" @click="submitCreateBatch" class="rounded-lg" :loading="submitting">创建批次</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="打款批次明细" width="1000px" class="rounded-lg">
      <div v-if="batchDetail">
        <el-descriptions :column="3" border class="mb-4">
          <el-descriptions-item label="批次号">{{ batchDetail.batch.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="批次名称">{{ batchDetail.batch.batchName }}</el-descriptions-item>
          <el-descriptions-item label="打款渠道">{{ getChannelText(batchDetail.batch.paymentChannel) }}</el-descriptions-item>
          <el-descriptions-item label="总笔数">{{ batchDetail.batch.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span class="font-bold text-orange-600">¥{{ batchDetail.batch.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getBatchStatusType(batchDetail.batch.status)">
              {{ getBatchStatusText(batchDetail.batch.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预计打款日">{{ batchDetail.batch.paymentDate }}</el-descriptions-item>
          <el-descriptions-item label="实际打款日">{{ batchDetail.batch.actualPaymentDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ batchDetail.batch.operatorName }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="font-bold text-lg mb-3">批次内结算单明细（含争议原因）</h4>
        <el-table :data="batchDetail.settlements" border max-height="400" class="rounded-lg" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
          <el-table-column prop="settlement.settlementNo" label="结算单号" width="180" />
          <el-table-column label="工人信息" width="180">
            <template #default="{ row }">
              <div class="font-medium">{{ row.worker?.workerName }}</div>
              <div class="text-xs text-gray-500">工号：{{ row.worker?.workerNo }}</div>
              <div class="text-xs text-gray-500">银行卡：{{ row.worker?.bankCard }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="settlement.workDate" label="工作日期" width="110" />
          <el-table-column label="工时" width="120">
            <template #default="{ row }">
              <div>原始：{{ row.settlement.originalHours }}h</div>
              <div class="text-blue-600">结算：{{ row.settlement.actualHours }}h</div>
            </template>
          </el-table-column>
          <el-table-column label="金额明细" width="180">
            <template #default="{ row }">
              <div>基础：¥{{ row.settlement.baseAmount }}</div>
              <div class="text-green-600">补贴：¥{{ row.settlement.tempSubsidy }}</div>
              <div class="text-red-600">扣款：¥{{ row.settlement.deductionAmount }}</div>
              <div class="font-bold text-orange-600 pt-1 border-t mt-1">应发：¥{{ row.settlement.totalAmount }}</div>
            </template>
          </el-table-column>
          <el-table-column label="争议信息" min-width="220">
            <template #default="{ row }">
              <div v-if="row.dispute">
                <el-tag type="warning" size="small" class="mb-1">有争议</el-tag>
                <div class="text-xs font-medium text-red-600">争议原因：{{ row.dispute.disputeReason }}</div>
                <div class="text-xs text-gray-500">类型：{{ row.dispute.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}</div>
                <div class="text-xs text-gray-500">争议金额：¥{{ row.dispute.originalAmount }} → ¥{{ row.dispute.claimedAmount }}</div>
                <el-tag v-if="row.settlement.remark" type="info" size="small" class="mt-1">
                  {{ row.settlement.remark }}
                </el-tag>
              </div>
              <el-tag v-else type="success" size="small">正常结算</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="settlement.status" label="结算状态" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getSettlementStatusText(row.settlement.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button
          v-if="batchDetail?.batch?.status === 'pending'"
          type="success"
          @click="markCurrentBatchPaid"
          class="rounded-lg"
        >
          标记已打款
        </el-button>
        <el-button @click="detailVisible = false" class="rounded-lg">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { paymentBatchApi } from '@/api/paymentBatch'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const batchDetail = ref(null)
const availableSettlements = ref([])
const selectedSettlements = ref([])
const batchFormRef = ref(null)

const searchForm = reactive({
  status: '',
  dateRange: []
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const batchForm = reactive({
  batchName: `日结工资打款-${dayjs().format('YYYYMMDD')}`,
  paymentChannel: 'bank',
  paymentDate: dayjs().format('YYYY-MM-DD'),
  operatorId: 1,
  operatorName: '财务',
  remark: '',
  settlementIds: []
})

const batchRules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  paymentChannel: [{ required: true, message: '请选择打款渠道', trigger: 'change' }],
  paymentDate: [{ required: true, message: '请选择打款日期', trigger: 'change' }],
  settlementIds: [{ required: true, message: '请选择结算单', trigger: 'change' }]
}

const selectedTotalAmount = computed(() => {
  return selectedSettlements.value.reduce((sum, item) => {
    return sum + (item.settlement?.totalAmount || 0)
  }, 0)
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.paymentDateStart = searchForm.dateRange[0]
      params.paymentDateEnd = searchForm.dateRange[1]
    }
    const res = await paymentBatchApi.page(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, { status: '', dateRange: [] })
  handleSearch()
}

const openCreateBatch = async () => {
  Object.assign(batchForm, {
    batchName: `日结工资打款-${dayjs().format('YYYYMMDD')}`,
    paymentChannel: 'bank',
    paymentDate: dayjs().format('YYYY-MM-DD'),
    operatorId: 1,
    operatorName: '财务',
    remark: '',
    settlementIds: []
  })
  selectedSettlements.value = []
  createVisible.value = true
  await refreshAvailableSettlements()
}

const refreshAvailableSettlements = async () => {
  try {
    const res = await paymentBatchApi.getAvailableSettlements({})
    if (res.code === 200) {
      availableSettlements.value = res.data
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载可打款结算单失败')
  }
}

const handleSettlementSelection = (selection) => {
  selectedSettlements.value = selection
  batchForm.settlementIds = selection.map(s => s.settlement.id)
}

const submitCreateBatch = async () => {
  if (!batchFormRef.value) return
  await batchFormRef.value.validate(async (valid) => {
    if (valid) {
      if (batchForm.settlementIds.length === 0) {
        ElMessage.warning('请选择至少一笔结算单')
        return
      }
      submitting.value = true
      try {
        const res = await paymentBatchApi.create({
          batchName: batchForm.batchName,
          paymentDate: batchForm.paymentDate,
          paymentChannel: batchForm.paymentChannel,
          remark: batchForm.remark,
          operatorId: batchForm.operatorId,
          operatorName: batchForm.operatorName,
          settlementIds: batchForm.settlementIds
        })
        if (res.code === 200) {
          ElMessage.success('创建打款批次成功')
          createVisible.value = false
          loadData()
        }
      } catch (error) {
        console.error(error)
        ElMessage.error('创建失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const viewBatchDetail = async (row) => {
  try {
    const res = await paymentBatchApi.getById(row.id)
    if (res.code === 200) {
      batchDetail.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取明细失败')
  }
}

const markBatchPaid = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认批次【${row.batchName}】已完成打款吗？\n共 ${row.totalCount} 笔，合计 ¥${row.totalAmount}`,
      '确认打款',
      { type: 'warning' }
    )
    const res = await paymentBatchApi.markPaid(row.id, {})
    if (res.code === 200) {
      ElMessage.success('标记打款成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('标记失败')
    }
  }
}

const markCurrentBatchPaid = () => {
  if (batchDetail.value?.batch) {
    markBatchPaid(batchDetail.value.batch)
    viewBatchDetail(batchDetail.value.batch)
  }
}

const getChannelText = (channel) => {
  const texts = { bank: '银行转账', alipay: '支付宝', wechat: '微信' }
  return texts[channel] || channel
}

const getBatchStatusType = (status) => {
  const types = {
    pending: 'warning',
    processing: 'primary',
    completed: 'success',
    failed: 'danger'
  }
  return types[status] || 'info'
}

const getBatchStatusText = (status) => {
  const texts = {
    pending: '待打款',
    processing: '打款中',
    completed: '已完成',
    failed: '打款失败'
  }
  return texts[status] || status
}

const getSettlementStatusText = (status) => {
  const texts = {
    pending: '待确认',
    confirmed: '已确认',
    disputed: '有争议',
    paid: '已打款',
    cancelled: '已取消'
  }
  return texts[status] || status
}

const handleSizeChange = (size) => {
  pagination.size = size
  loadData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.payment-page {
  max-width: 1600px;
  margin: 0 auto;
}
</style>
