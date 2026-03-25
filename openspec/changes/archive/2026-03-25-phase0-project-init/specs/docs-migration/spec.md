## ADDED Requirements

### Requirement: 迁移现有文档和脚本到 docs 目录

系统 SHALL 将现有的数据分析报告和 Python 脚本复制到 `wh-op-platform/docs/` 目录下的对应子目录中。

所属服务：项目根目录（docs/ 目录）

#### Scenario: 迁移数据 Profiling 报告
- **WHEN** 执行迁移
- **THEN** `data_profiling_reports/` 下的所有 19 份 Markdown 报告 SHALL 被复制到 `docs/data_profiling_reports/`
- **THEN** 文件内容 SHALL 保持不变

#### Scenario: 迁移数据分析报告
- **WHEN** 执行迁移
- **THEN** `data_analysis_reports/` 下的分析报告 SHALL 被复制到 `docs/data_analysis_reports/`
- **THEN** 文件内容 SHALL 保持不变

#### Scenario: 迁移 Python 分析脚本
- **WHEN** 执行迁移
- **THEN** `cost_analysis.py`、`warehouse_type_analysis.py`、`db_profile_script.py` SHALL 被复制到 `docs/scripts/`
- **THEN** 文件内容 SHALL 保持不变

#### Scenario: 保留原始文件
- **WHEN** 迁移完成
- **THEN** 原始位置的文件 SHALL 保持不变（复制而非移动）
- **THEN** 后续清理在 Monorepo 完全搭建后再执行
