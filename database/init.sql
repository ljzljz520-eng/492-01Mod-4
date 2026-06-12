SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `scaffolding_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `scaffolding_db`;

-- 文件信息表
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint(20) DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `file_extension` varchar(20) DEFAULT NULL COMMENT '文件扩展名',
  `upload_user_id` bigint(20) DEFAULT NULL COMMENT '上传人ID',
  `upload_user_name` varchar(50) DEFAULT NULL COMMENT '上传人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 工作管理表
DROP TABLE IF EXISTS `work`;
CREATE TABLE `work` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `work_name` varchar(100) NOT NULL COMMENT '工作名称',
  `work_content` text COMMENT '工作内容',
  `work_status` varchar(20) DEFAULT 'pending' COMMENT '工作状态（pending-待处理，in_progress-进行中，completed-已完成，cancelled-已取消）',
  `work_time` datetime DEFAULT NULL COMMENT '工作时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `priority` varchar(20) DEFAULT 'normal' COMMENT '优先级（low-低，normal-普通，high-高，urgent-紧急）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_work_status` (`work_status`),
  KEY `idx_work_time` (`work_time`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作管理表';

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名（账号）',
  `password` varchar(100) NOT NULL COMMENT '密码（不加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认admin账号
INSERT INTO `user` (`username`, `password`, `nickname`) VALUES ('admin', '123456', '管理员');

-- 岗位表
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `position_code` varchar(50) NOT NULL COMMENT '岗位编码',
  `position_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '岗位单价（元/小时）',
  `description` varchar(500) DEFAULT NULL COMMENT '岗位描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态（1-启用，0-禁用）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_code` (`position_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- 工人表
DROP TABLE IF EXISTS `worker`;
CREATE TABLE `worker` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `worker_no` varchar(50) NOT NULL COMMENT '工人工号',
  `worker_name` varchar(50) NOT NULL COMMENT '工人姓名',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `position_id` bigint(20) DEFAULT NULL COMMENT '所属岗位ID',
  `bank_card` varchar(30) DEFAULT NULL COMMENT '银行卡号',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户银行',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态（1-在职，0-离职）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_worker_no` (`worker_no`),
  KEY `idx_position_id` (`position_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工人表';

-- 打卡记录表
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE `attendance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `worker_id` bigint(20) NOT NULL COMMENT '工人ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '打卡上班时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '打卡下班时间',
  `original_hours` decimal(5,2) DEFAULT '0.00' COMMENT '原始工时（小时）',
  `check_in_image` varchar(500) DEFAULT NULL COMMENT '上班打卡照片',
  `check_out_image` varchar(500) DEFAULT NULL COMMENT '下班打卡照片',
  `location` varchar(200) DEFAULT NULL COMMENT '打卡地点',
  `status` varchar(20) DEFAULT 'normal' COMMENT '状态（normal-正常，late-迟到，early_leave-早退，absent-缺勤）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_worker_date` (`worker_id`, `work_date`),
  KEY `idx_work_date` (`work_date`),
  KEY `idx_worker_id` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- 日结工资单表
DROP TABLE IF EXISTS `daily_settlement`;
CREATE TABLE `daily_settlement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `settlement_no` varchar(50) NOT NULL COMMENT '结算单号',
  `worker_id` bigint(20) NOT NULL COMMENT '工人ID',
  `position_id` bigint(20) NOT NULL COMMENT '岗位ID',
  `attendance_id` bigint(20) DEFAULT NULL COMMENT '打卡记录ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `original_hours` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '原始工时（小时）',
  `actual_hours` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '实际结算工时（小时）',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '岗位单价（元/小时）',
  `base_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '基础工资',
  `temp_subsidy` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '临时补贴',
  `deduction_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '扣款金额',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应发工资',
  `supervisor_id` bigint(20) DEFAULT NULL COMMENT '主管ID',
  `supervisor_name` varchar(50) DEFAULT NULL COMMENT '主管姓名',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态（pending-待确认，confirmed-已确认，disputed-有争议，paid-已打款，cancelled-已取消）',
  `dispute_id` bigint(20) DEFAULT NULL COMMENT '关联争议单ID',
  `batch_id` bigint(20) DEFAULT NULL COMMENT '打款批次ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_worker_id` (`worker_id`),
  KEY `idx_work_date` (`work_date`),
  KEY `idx_status` (`status`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日结工资单表';

-- 工资争议单表
DROP TABLE IF EXISTS `wage_dispute`;
CREATE TABLE `wage_dispute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dispute_no` varchar(50) NOT NULL COMMENT '争议单号',
  `settlement_id` bigint(20) NOT NULL COMMENT '关联结算单ID',
  `worker_id` bigint(20) NOT NULL COMMENT '工人ID',
  `dispute_type` varchar(20) NOT NULL COMMENT '争议类型（supervisor_deduction-主管扣时，worker_appeal-工人申诉）',
  `original_hours` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '原始工时',
  `claimed_hours` decimal(5,2) NOT NULL DEFAULT '0.00' COMMENT '主张工时',
  `original_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '原始金额',
  `claimed_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '主张金额',
  `dispute_reason` varchar(1000) DEFAULT NULL COMMENT '争议原因',
  `worker_remark` varchar(1000) DEFAULT NULL COMMENT '工人说明',
  `supervisor_remark` varchar(1000) DEFAULT NULL COMMENT '主管说明',
  `worker_submit_time` datetime DEFAULT NULL COMMENT '工人提交时间',
  `supervisor_submit_time` datetime DEFAULT NULL COMMENT '主管提交时间',
  `supervisor_id` bigint(20) DEFAULT NULL COMMENT '提交主管ID',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态（pending-待处理，worker_submitted-工人已提交，supervisor_submitted-主管已提交，arbitrating-仲裁中，approved-仲裁通过，rejected-仲裁驳回，closed-已关闭）',
  `arbitration_id` bigint(20) DEFAULT NULL COMMENT '仲裁记录ID',
  `final_hours` decimal(5,2) DEFAULT NULL COMMENT '最终裁定工时',
  `final_amount` decimal(12,2) DEFAULT NULL COMMENT '最终裁定金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispute_no` (`dispute_no`),
  KEY `idx_settlement_id` (`settlement_id`),
  KEY `idx_worker_id` (`worker_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资争议单表';

-- 争议证据表
DROP TABLE IF EXISTS `dispute_evidence`;
CREATE TABLE `dispute_evidence` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dispute_id` bigint(20) NOT NULL COMMENT '争议单ID',
  `evidence_type` varchar(20) NOT NULL COMMENT '证据类型（photo-照片，chat-聊天记录，other-其他）',
  `file_id` bigint(20) DEFAULT NULL COMMENT '文件ID（关联file_info）',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件访问地址',
  `submitter_type` varchar(20) NOT NULL COMMENT '提交方（worker-工人，supervisor-主管）',
  `submitter_id` bigint(20) DEFAULT NULL COMMENT '提交人ID',
  `submitter_name` varchar(50) DEFAULT NULL COMMENT '提交人姓名',
  `description` varchar(500) DEFAULT NULL COMMENT '证据说明',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_dispute_id` (`dispute_id`),
  KEY `idx_submitter_type` (`submitter_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='争议证据表';

-- 仲裁记录表
DROP TABLE IF EXISTS `arbitration_record`;
CREATE TABLE `arbitration_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dispute_id` bigint(20) NOT NULL COMMENT '争议单ID',
  `arbitrator_id` bigint(20) DEFAULT NULL COMMENT '仲裁人ID',
  `arbitrator_name` varchar(50) DEFAULT NULL COMMENT '仲裁人姓名',
  `arbitration_opinion` varchar(2000) NOT NULL COMMENT '仲裁意见',
  `arbitration_result` varchar(20) NOT NULL COMMENT '仲裁结果（approved-通过，rejected-驳回，partial-部分支持）',
  `approved_hours` decimal(5,2) DEFAULT NULL COMMENT '裁定工时',
  `approved_amount` decimal(12,2) DEFAULT NULL COMMENT '裁定金额',
  `arbitration_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '仲裁时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_dispute_id` (`dispute_id`),
  KEY `idx_arbitration_result` (`arbitration_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仲裁记录表';

-- 打款批次表
DROP TABLE IF EXISTS `payment_batch`;
CREATE TABLE `payment_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` varchar(50) NOT NULL COMMENT '批次号',
  `batch_name` varchar(100) NOT NULL COMMENT '批次名称',
  `total_count` int(11) NOT NULL DEFAULT '0' COMMENT '总笔数',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `payment_date` date DEFAULT NULL COMMENT '预计打款日期',
  `actual_payment_date` date DEFAULT NULL COMMENT '实际打款日期',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态（pending-待打款，processing-打款中，completed-已完成，failed-打款失败）',
  `payment_channel` varchar(50) DEFAULT NULL COMMENT '打款渠道（bank-银行，alipay-支付宝，wechat-微信）',
  `payment_voucher` varchar(500) DEFAULT NULL COMMENT '打款凭证',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_status` (`status`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打款批次表';

-- 初始化岗位数据
INSERT INTO `position` (`position_code`, `position_name`, `unit_price`, `description`) VALUES
('POS001', '普通操作工', 25.00, '普通生产线操作工'),
('POS002', '技术工', 40.00, '有专业技能的技术工人'),
('POS003', '搬运工', 30.00, '负责货物装卸搬运'),
('POS004', '包装工', 28.00, '负责产品包装');

-- 初始化工人数据
INSERT INTO `worker` (`worker_no`, `worker_name`, `id_card`, `phone`, `position_id`, `bank_card`, `bank_name`) VALUES
('W001', '张三', '110101199001011234', '13800138001', 1, '6222021234567890123', '中国工商银行'),
('W002', '李四', '110101199002022345', '13800138002', 2, '6222021234567890456', '中国建设银行'),
('W003', '王五', '110101199003033456', '13800138003', 1, '6222021234567890789', '中国农业银行');

-- 初始化打卡数据
INSERT INTO `attendance` (`worker_id`, `work_date`, `check_in_time`, `check_out_time`, `original_hours`, `status`) VALUES
(1, '2026-06-10', '2026-06-10 08:00:00', '2026-06-10 18:00:00', 10.00, 'normal'),
(2, '2026-06-10', '2026-06-10 08:00:00', '2026-06-10 18:00:00', 10.00, 'normal'),
(3, '2026-06-10', '2026-06-10 08:30:00', '2026-06-10 17:30:00', 9.00, 'late');

-- 初始化日结单数据
INSERT INTO `daily_settlement` (`settlement_no`, `worker_id`, `position_id`, `attendance_id`, `work_date`, `original_hours`, `actual_hours`, `unit_price`, `base_amount`, `temp_subsidy`, `deduction_amount`, `total_amount`, `status`, `remark`) VALUES
('DS20260610001', 1, 1, 1, '2026-06-10', 10.00, 10.00, 25.00, 250.00, 20.00, 0.00, 270.00, 'pending', '正常结算'),
('DS20260610002', 2, 2, 2, '2026-06-10', 10.00, 10.00, 40.00, 400.00, 0.00, 0.00, 400.00, 'confirmed', '已确认'),
('DS20260610003', 3, 1, 3, '2026-06-10', 9.00, 9.00, 25.00, 225.00, 0.00, 0.00, 225.00, 'disputed', '有争议待处理');

SET FOREIGN_KEY_CHECKS = 1;
