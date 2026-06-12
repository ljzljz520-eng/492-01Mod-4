<template>
  <div class="arbitration-list-page">
    <el-card class="mb-4 shadow-sm rounded-lg border-0" :body-style="{ padding: '24px' }">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-xl font-bold text-gray-800">仲裁管理</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="mb-6">
        <el-form-item label="仲裁结果">
          <el-select v-model="searchForm.arbitrationResult" placeholder="请选择" clearable style="width: 180px" class="rounded-lg">
            <el-option label="通过" value="approved" />
            <el-option label="部分支持" value="partial" />
            <el-option label="驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="rounded-lg">查询</el-button>
          <el-button @click="handleReset" class="rounded-lg">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border class="rounded-lg overflow-hidden" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="disputeId" label="争议单ID" width="120" />
        <el-table-column prop="arbitratorName" label="仲裁人" width="120" />
        <el-table-column prop="arbitrationResult" label="仲裁结果" width="120">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.arbitrationResult)" class="rounded-full px-3 py-1">
              {{ getResultText(row.arbitrationResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="裁定工时/金额" width="200">
          <template #default="{ row }">
            <div>{{ row.approvedHours }} 小时</div>
            <div class="font-bold text-orange-600">¥{{ row.approvedAmount }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="arbitrationOpinion" label="仲裁意见" min-width="250" show-overflow-tooltip />
        <el-table-column prop="arbitrationTime" label="仲裁时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看争议</el-button>
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

      <el-divider />

      <h3 class="text-lg font-bold mb-4 text-gray-800">待仲裁的争议单</h3>
      <el-table :data="pendingDisputes" v-loading="loadingPending" border class="rounded-lg overflow-hidden" :header-cell-style="{ background: '#fff7e6', color: '#606266' }">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="disputeNo" label="争议单号" min-width="200" />
        <el-table-column prop="disputeType" label="类型" width="120">
          <template #default="{ row }">
            {{ row.disputeType === 'supervisor_deduction' ? '主管扣时' : '工人申诉' }}
          </template>
        </el-table-column>
        <el-table-column label="金额对比" width="220">
          <template #default="{ row }">
            <span>原始：¥{{ row.originalAmount }}</span>
            <span class="mx-2">→</span>
            <span>主张：¥{{ row.claimedAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="disputeReason" label="争议原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag type="primary" class="rounded-full px-3 py-1">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" size="small" link @click="goToArbitration(row)">立即仲裁</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { arbitrationApi } from '@/api/arbitration'
import { disputeApi } from '@/api/dispute'

const router = useRouter()
const loading = ref(false)
const loadingPending = ref(false)
const tableData = ref([])
const pendingDisputes = ref([])

const searchForm = reactive({
  arbitrationResult: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size
    }
    if (searchForm.arbitrationResult) params.arbitrationResult = searchForm.arbitrationResult
    const res = await arbitrationApi.page(params)
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

const loadPendingDisputes = async () => {
  loadingPending.value = true
  try {
    const res = await disputeApi.page({ current: 1, size: 20, status: 'arbitrating' })
    if (res.code === 200) {
      pendingDisputes.value = res.data.records
    }
  } catch (error) {
    console.error(error)
  } finally {
    loadingPending.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, { arbitrationResult: '' })
  handleSearch()
}

const handleView = (row) => {
  router.push(`/pc/dispute/${row.disputeId}`)
}

const goToArbitration = (row) => {
  router.push(`/pc/arbitration/${row.id}`)
}

const getResultType = (result) => {
  const types = { approved: 'success', rejected: 'danger', partial: 'warning' }
  return types[result] || 'info'
}

const getResultText = (result) => {
  const texts = { approved: '通过', rejected: '驳回', partial: '部分支持' }
  return texts[result] || result
}

const getStatusText = (status) => {
  const texts = {
    pending: '待处理',
    worker_submitted: '工人已提交',
    supervisor_submitted: '主管已提交',
    arbitrating: '仲裁中',
    approved: '仲裁通过',
    rejected: '仲裁驳回',
    closed: '已关闭'
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
  loadPendingDisputes()
})
</script>

<style scoped>
.arbitration-list-page {
  max-width: 1600px;
  margin: 0 auto;
}
</style>
