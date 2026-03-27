#!/usr/bin/env python3
"""
数据探查脚本 - 对 wh_op_baseline 库中所有表进行全面的数据质量分析
生成每张表独立的 Markdown 探查报告
"""
import pymysql
import os
import sys
from collections import OrderedDict
from datetime import datetime

# ─── 数据库连接配置 ───
DB_CONFIG = {
    'host': '10.126.50.199',
    'port': 3306,
    'user': 'fdeuser',
    'password': 'FDE2026!',
    'database': 'wh_op_baseline',
    'charset': 'utf8mb4',
    'connect_timeout': 30,
    'read_timeout': 300,
}

LARGE_TABLE_THRESHOLD = 100000  # 大表阈值

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data_profiling_reports')
TOP_N = 15  # 值分布展示 Top N


def get_connection():
    conn = pymysql.connect(**DB_CONFIG)
    # Set session query timeout to 120s for safety
    with conn.cursor() as cur:
        try:
            cur.execute("SET SESSION MAX_EXECUTION_TIME=120000")
        except Exception:
            pass  # Not supported on older MySQL
    return conn


def query(conn, sql, params=None):
    with conn.cursor() as cur:
        cur.execute(sql, params)
        cols = [d[0] for d in cur.description] if cur.description else []
        rows = cur.fetchall()
    return cols, rows


def query_one(conn, sql, params=None):
    _, rows = query(conn, sql, params)
    return rows[0] if rows else None


def get_tables(conn):
    _, rows = query(conn, "SHOW TABLES")
    return [r[0] for r in rows]


def get_columns_info(conn, table):
    """获取表的列结构信息"""
    _, rows = query(conn, f"SHOW FULL COLUMNS FROM `{table}`")
    columns = []
    for r in rows:
        columns.append({
            'name': r[0],
            'type': r[1],
            'collation': r[2],
            'null': r[3],       # YES/NO
            'key': r[4],        # PRI/MUL/UNI/''
            'default': r[5],
            'extra': r[6],
            'comment': r[8] if len(r) > 8 else '',
        })
    return columns


def profile_table(conn, table):
    """对单张表进行全面的数据探查"""
    result = OrderedDict()
    result['table'] = table

    # 1. 基础信息
    row_count = query_one(conn, f"SELECT COUNT(*) FROM `{table}`")[0]
    result['row_count'] = row_count
    columns = get_columns_info(conn, table)
    result['column_count'] = len(columns)
    result['columns'] = columns

    print(f"  正在探查: {table} ({row_count:,} 行, {len(columns)} 列) ...", flush=True)

    if row_count == 0:
        print("空表，跳过详细探查")
        result['column_profiles'] = []
        return result

    # 2. 逐列探查
    is_large = row_count > LARGE_TABLE_THRESHOLD
    col_profiles = []
    for col_idx, col in enumerate(columns):
        cname = col['name']
        cp = OrderedDict()
        cp['name'] = cname
        cp['type'] = col['type']
        cp['nullable'] = col['null']
        cp['key'] = col['key']
        cp['comment'] = col['comment']

        # 2a. 空值统计 + 唯一值数量（合并为一条SQL减少往返）
        # 大表分开查询避免超时
        if is_large:
            try:
                r = query_one(conn, f"SELECT SUM(`{cname}` IS NULL), SUM(`{cname}` IS NULL OR TRIM(CAST(`{cname}` AS CHAR))='') FROM `{table}`")
                null_count = r[0] or 0
                empty_count = r[1] or 0
            except Exception:
                null_count = empty_count = 0
            try:
                r = query_one(conn, f"SELECT COUNT(DISTINCT `{cname}`) FROM `{table}`")
                distinct_count = r[0]
            except Exception:
                distinct_count = -1
            total = row_count
        else:
            try:
                r = query_one(conn, f"""
                    SELECT
                        COUNT(*) as total,
                        SUM(CASE WHEN `{cname}` IS NULL THEN 1 ELSE 0 END) as null_count,
                        SUM(CASE WHEN `{cname}` IS NULL OR TRIM(CAST(`{cname}` AS CHAR)) = '' THEN 1 ELSE 0 END) as empty_count,
                        COUNT(DISTINCT `{cname}`) as distinct_count
                    FROM `{table}`
                """)
                total, null_count, empty_count, distinct_count = r[0], r[1], r[2], r[3]
            except Exception:
                total = row_count
                null_count = empty_count = 0
                distinct_count = -1

        cp['total'] = total
        cp['null_count'] = int(null_count) if null_count else 0
        cp['null_pct'] = round(cp['null_count'] / total * 100, 2) if total > 0 else 0
        cp['empty_count'] = int(empty_count) if empty_count else 0
        cp['empty_pct'] = round(cp['empty_count'] / total * 100, 2) if total > 0 else 0
        cp['distinct_count'] = distinct_count if distinct_count >= 0 else 0
        cp['distinct_pct'] = round(cp['distinct_count'] / total * 100, 2) if total > 0 else 0
        cp['is_unique'] = (cp['distinct_count'] == total and cp['null_count'] == 0)

        if (col_idx + 1) % 5 == 0:
            print(f"    ... 列 {col_idx + 1}/{len(columns)}", flush=True)

        # 2c. 类型相关统计
        col_type_lower = col['type'].lower()
        is_numeric = any(t in col_type_lower for t in ['int', 'decimal', 'float', 'double', 'numeric', 'bigint', 'tinyint', 'smallint', 'mediumint'])
        is_date = any(t in col_type_lower for t in ['date', 'time', 'datetime', 'timestamp'])
        is_text = any(t in col_type_lower for t in ['char', 'varchar', 'text', 'longtext', 'mediumtext', 'tinytext', 'enum', 'set'])

        if is_numeric:
            try:
                r = query_one(conn, f"""
                    SELECT MIN(`{cname}`), MAX(`{cname}`),
                           AVG(CAST(`{cname}` AS DECIMAL(30,4))),
                           STDDEV(CAST(`{cname}` AS DECIMAL(30,4)))
                    FROM `{table}` WHERE `{cname}` IS NOT NULL
                """)
                cp['min'] = r[0]
                cp['max'] = r[1]
                cp['mean'] = round(float(r[2]), 4) if r[2] is not None else None
                cp['stddev'] = round(float(r[3]), 4) if r[3] is not None else None
            except Exception:
                cp['min'] = cp['max'] = cp['mean'] = cp['stddev'] = None

            # 零值统计
            try:
                r = query_one(conn, f"SELECT COUNT(*) FROM `{table}` WHERE `{cname}` = 0")
                cp['zero_count'] = r[0]
                cp['zero_pct'] = round(r[0] / total * 100, 2) if total > 0 else 0
            except Exception:
                cp['zero_count'] = 0
                cp['zero_pct'] = 0

            # 负值统计
            try:
                r = query_one(conn, f"SELECT COUNT(*) FROM `{table}` WHERE `{cname}` < 0")
                cp['negative_count'] = r[0]
            except Exception:
                cp['negative_count'] = 0

        elif is_date:
            try:
                r = query_one(conn, f"""
                    SELECT MIN(`{cname}`), MAX(`{cname}`)
                    FROM `{table}` WHERE `{cname}` IS NOT NULL
                """)
                cp['min'] = str(r[0]) if r[0] else None
                cp['max'] = str(r[1]) if r[1] else None
            except Exception:
                cp['min'] = cp['max'] = None

        elif is_text:
            try:
                r = query_one(conn, f"""
                    SELECT MIN(CHAR_LENGTH(`{cname}`)), MAX(CHAR_LENGTH(`{cname}`)),
                           AVG(CHAR_LENGTH(`{cname}`))
                    FROM `{table}` WHERE `{cname}` IS NOT NULL AND `{cname}` != ''
                """)
                cp['min_length'] = r[0]
                cp['max_length'] = r[1]
                cp['avg_length'] = round(float(r[2]), 1) if r[2] is not None else None
            except Exception:
                cp['min_length'] = cp['max_length'] = cp['avg_length'] = None

        cp['is_numeric'] = is_numeric
        cp['is_date'] = is_date
        cp['is_text'] = is_text

        # 2d. Top N 值分布
        is_large = row_count > LARGE_TABLE_THRESHOLD
        # 大表 + 高基数列跳过全表 GROUP BY
        skip_distribution = is_large and cp['distinct_count'] > 1000
        if skip_distribution:
            cp['top_values'] = []
            cp['dist_skipped'] = True
        elif cp['distinct_count'] <= 500 or not is_numeric:
            try:
                _, dist_rows = query(conn, f"""
                    SELECT `{cname}` as val, COUNT(*) as cnt
                    FROM `{table}`
                    GROUP BY `{cname}`
                    ORDER BY cnt DESC
                    LIMIT {TOP_N}
                """)
                cp['top_values'] = [(str(r[0]) if r[0] is not None else 'NULL', r[1]) for r in dist_rows]
            except Exception:
                cp['top_values'] = []
        else:
            try:
                _, hi_rows = query(conn, f"""
                    SELECT `{cname}` as val, COUNT(*) as cnt
                    FROM `{table}`
                    GROUP BY `{cname}`
                    ORDER BY cnt DESC
                    LIMIT 5
                """)
                cp['top_values'] = [(str(r[0]) if r[0] is not None else 'NULL', r[1]) for r in hi_rows]
            except Exception:
                cp['top_values'] = []

        # 2e. 样本数据（前3行）
        try:
            _, sample_rows = query(conn, f"SELECT `{cname}` FROM `{table}` LIMIT 3")
            cp['samples'] = [str(r[0]) if r[0] is not None else 'NULL' for r in sample_rows]
        except Exception:
            cp['samples'] = []

        col_profiles.append(cp)

    result['column_profiles'] = col_profiles

    # 3. 重复行检测（跳过超大表，避免超时）
    if row_count <= LARGE_TABLE_THRESHOLD:
        all_cols = ', '.join([f'`{c["name"]}`' for c in columns])
        try:
            r = query_one(conn, f"""
                SELECT COUNT(*) FROM (
                    SELECT {all_cols}, COUNT(*) as dup_cnt
                    FROM `{table}`
                    GROUP BY {all_cols}
                    HAVING dup_cnt > 1
                ) t
            """)
            result['duplicate_groups'] = r[0]
        except Exception:
            result['duplicate_groups'] = '检测失败'
    else:
        result['duplicate_groups'] = f'跳过（表行数 {row_count:,} 超过阈值）'

    # 4. 数据质量评分
    quality_issues = []
    for cp in col_profiles:
        if cp['null_pct'] > 50:
            quality_issues.append(f"字段 `{cp['name']}` 空值率高达 {cp['null_pct']}%")
        if cp['empty_pct'] > 0 and cp['empty_pct'] > cp['null_pct']:
            quality_issues.append(f"字段 `{cp['name']}` 存在空字符串（{cp['empty_count']}条）")
        if cp.get('is_numeric') and cp.get('zero_pct', 0) > 80:
            quality_issues.append(f"字段 `{cp['name']}` 零值率 {cp['zero_pct']}%，可能为默认填充")
        if cp['distinct_count'] == 1 and cp['null_count'] == 0:
            quality_issues.append(f"字段 `{cp['name']}` 仅有1个唯一值，信息量为零")
        if cp.get('is_unique') and cp['key'] == '':
            quality_issues.append(f"字段 `{cp['name']}` 值全部唯一但未设置索引，可能是候选主键")
    result['quality_issues'] = quality_issues

    print(f"完成 ({len(col_profiles)} 列)")
    return result


def generate_markdown(profile):
    """生成单表的 Markdown 探查报告"""
    table = profile['table']
    lines = []
    lines.append(f"# 数据探查报告：{table}\n")
    lines.append(f"> 生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    # ── 概览 ──
    lines.append("## 一、表概览\n")
    lines.append(f"| 指标 | 值 |")
    lines.append(f"|------|-----|")
    lines.append(f"| 表名 | `{table}` |")
    lines.append(f"| 总行数 | {profile['row_count']:,} |")
    lines.append(f"| 总列数 | {profile['column_count']} |")
    dup = profile.get('duplicate_groups', '未检测')
    lines.append(f"| 完全重复行组数 | {dup} |")
    lines.append("")

    if profile['row_count'] == 0:
        lines.append("**⚠️ 此表为空表，无数据可探查。**\n")
        return '\n'.join(lines)

    # ── 表结构 ──
    lines.append("## 二、表结构\n")
    lines.append("| # | 字段名 | 数据类型 | 可空 | 键 | 备注 |")
    lines.append("|---|--------|----------|------|-----|------|")
    for i, col in enumerate(profile['columns'], 1):
        lines.append(f"| {i} | `{col['name']}` | {col['type']} | {col['null']} | {col['key']} | {col['comment'] or '-'} |")
    lines.append("")

    # ── 空值分析 ──
    lines.append("## 三、空值与完整性分析\n")
    lines.append("| 字段名 | 总行数 | NULL数 | NULL占比 | 空值数(含空串) | 空值占比 |")
    lines.append("|--------|--------|--------|----------|----------------|----------|")
    for cp in profile['column_profiles']:
        lines.append(
            f"| `{cp['name']}` | {cp['total']:,} | {cp['null_count']:,} | {cp['null_pct']}% "
            f"| {cp['empty_count']:,} | {cp['empty_pct']}% |"
        )
    lines.append("")

    # ── 唯一性分析 ──
    lines.append("## 四、唯一性分析\n")
    lines.append("| 字段名 | 唯一值数 | 唯一占比 | 是否完全唯一 |")
    lines.append("|--------|----------|----------|--------------|")
    for cp in profile['column_profiles']:
        lines.append(
            f"| `{cp['name']}` | {cp['distinct_count']:,} | {cp['distinct_pct']}% "
            f"| {'✅ 是' if cp['is_unique'] else '否'} |"
        )
    lines.append("")

    # ── 数值型字段统计 ──
    numeric_cols = [cp for cp in profile['column_profiles'] if cp.get('is_numeric')]
    if numeric_cols:
        lines.append("## 五、数值型字段统计\n")
        lines.append("| 字段名 | 最小值 | 最大值 | 均值 | 标准差 | 零值数 | 零值率 | 负值数 |")
        lines.append("|--------|--------|--------|------|--------|--------|--------|--------|")
        for cp in numeric_cols:
            lines.append(
                f"| `{cp['name']}` | {cp.get('min', '-')} | {cp.get('max', '-')} "
                f"| {cp.get('mean', '-')} | {cp.get('stddev', '-')} "
                f"| {cp.get('zero_count', '-')} | {cp.get('zero_pct', '-')}% "
                f"| {cp.get('negative_count', '-')} |"
            )
        lines.append("")

    # ── 日期型字段统计 ──
    date_cols = [cp for cp in profile['column_profiles'] if cp.get('is_date')]
    if date_cols:
        lines.append("## 六、日期型字段统计\n")
        lines.append("| 字段名 | 最早日期 | 最晚日期 |")
        lines.append("|--------|----------|----------|")
        for cp in date_cols:
            lines.append(f"| `{cp['name']}` | {cp.get('min', '-')} | {cp.get('max', '-')} |")
        lines.append("")

    # ── 文本型字段统计 ──
    text_cols = [cp for cp in profile['column_profiles'] if cp.get('is_text')]
    if text_cols:
        section = "六" if not date_cols else "七"
        if numeric_cols and not date_cols:
            section = "六"
        elif numeric_cols and date_cols:
            section = "七"
        elif not numeric_cols and date_cols:
            section = "六"
        elif not numeric_cols and not date_cols:
            section = "五"
        lines.append(f"## {section}、文本型字段长度统计\n")
        lines.append("| 字段名 | 最短长度 | 最长长度 | 平均长度 |")
        lines.append("|--------|----------|----------|----------|")
        for cp in text_cols:
            lines.append(
                f"| `{cp['name']}` | {cp.get('min_length', '-')} | {cp.get('max_length', '-')} "
                f"| {cp.get('avg_length', '-')} |"
            )
        lines.append("")

    # ── 字段值分布 (Top N) ──
    lines.append("## 字段值分布（Top 15）\n")
    for cp in profile['column_profiles']:
        if cp.get('dist_skipped'):
            lines.append(f"### `{cp['name']}`\n")
            lines.append(f"唯一值数：{cp['distinct_count']:,}　|　类型：{cp['type']}\n")
            lines.append("*（大表高基数列，跳过全表值分布统计）*\n")
            continue
        if not cp.get('top_values'):
            continue
        lines.append(f"### `{cp['name']}`\n")
        lines.append(f"唯一值数：{cp['distinct_count']:,}　|　类型：{cp['type']}\n")
        lines.append("| 值 | 出现次数 | 占比 |")
        lines.append("|----|----------|------|")
        for val, cnt in cp['top_values']:
            pct = round(cnt / cp['total'] * 100, 2) if cp['total'] > 0 else 0
            # 截断过长的值
            display_val = val if len(val) <= 60 else val[:57] + '...'
            lines.append(f"| {display_val} | {cnt:,} | {pct}% |")
        lines.append("")

    # ── 样本数据 ──
    lines.append("## 样本数据（前3行）\n")
    lines.append("| 字段名 | 样本1 | 样本2 | 样本3 |")
    lines.append("|--------|-------|-------|-------|")
    for cp in profile['column_profiles']:
        samples = cp.get('samples', [])
        s = [s if len(s) <= 40 else s[:37] + '...' for s in samples]
        while len(s) < 3:
            s.append('-')
        lines.append(f"| `{cp['name']}` | {s[0]} | {s[1]} | {s[2]} |")
    lines.append("")

    # ── 数据质量问题 ──
    issues = profile.get('quality_issues', [])
    lines.append("## 数据质量问题汇总\n")
    if issues:
        for issue in issues:
            lines.append(f"- ⚠️ {issue}")
    else:
        lines.append("✅ 未发现显著的数据质量问题。")
    lines.append("")

    return '\n'.join(lines)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    conn = get_connection()
    print(f"已连接到 {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")

    tables = get_tables(conn)
    print(f"共发现 {len(tables)} 张表\n")

    for i, table in enumerate(tables, 1):
        filepath = os.path.join(OUTPUT_DIR, f"{table}.md")
        if '--skip-existing' in sys.argv and os.path.exists(filepath):
            print(f"[{i}/{len(tables)}]  跳过已存在: {table}")
            continue
        print(f"[{i}/{len(tables)}]", end=' ')
        try:
            conn.ping(reconnect=True)
        except Exception:
            conn = get_connection()
        try:
            profile = profile_table(conn, table)
            md_content = generate_markdown(profile)
            filepath = os.path.join(OUTPUT_DIR, f"{table}.md")
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(md_content)
            print(f"  → 已保存: {filepath}")
        except Exception as e:
            print(f"  ✗ 探查 {table} 失败: {e}")

    conn.close()
    print(f"\n✅ 全部完成！报告已保存到: {OUTPUT_DIR}")


if __name__ == '__main__':
    main()
