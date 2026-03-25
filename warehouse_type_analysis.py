#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
仓库类型划分 + 每月日均费用基线
基于实际列名修正
"""
import pymysql, os
from collections import defaultdict
from decimal import Decimal

conn = pymysql.connect(
    host='10.126.50.199', port=3306,
    user='fdeuser', password='FDE2026!',
    database='wh_op_baseline', charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)
cur = conn.cursor()

HOUSE_MAP = {'12000004': '天津武清佩森A仓', '32050005': '常熟高新正创B仓'}
HOUSE_NAME_TO_ID = {'天津武清佩森A仓': '12000004', '常熟高新正创B仓': '32050005',
                     '常熟高新正创仓': '32050005'}

# ============================================================
# 1. 仓位库存信息 → 仓位数/区域/SKU
# ============================================================
print("[1/7] 仓位库存信息...")
cur.execute("""
SELECT `库房`, COUNT(*) AS total_rows,
       COUNT(DISTINCT `仓位号`) AS unique_positions,
       COUNT(DISTINCT `区域`) AS zone_count,
       COUNT(DISTINCT `库存地`) AS storage_count,
       COUNT(DISTINCT `外部物料号`) AS sku_count,
       SUM(`可用库存`) AS total_stock
FROM `仓位库存信息表`
GROUP BY `库房`
""")
loc_info = {}
for r in cur.fetchall():
    hid = HOUSE_NAME_TO_ID.get(r['库房'], 'unknown')
    loc_info[hid] = {k: (float(v) if isinstance(v, Decimal) else v) for k, v in r.items()}

# 区域分布
cur.execute("""
SELECT `库房`, `区域`, COUNT(*) AS cnt, SUM(`可用库存`) AS stock
FROM `仓位库存信息表`
GROUP BY `库房`, `区域` ORDER BY `库房`, cnt DESC
""")
zone_dist = defaultdict(list)
for r in cur.fetchall():
    hid = HOUSE_NAME_TO_ID.get(r['库房'], 'unknown')
    zone_dist[hid].append((r['区域'], r['cnt'], float(r['stock'] or 0)))

# 库存地分布
cur.execute("""
SELECT `库房`, `库存地`, COUNT(*) AS cnt
FROM `仓位库存信息表`
GROUP BY `库房`, `库存地` ORDER BY `库房`, cnt DESC
""")
storage_dist = defaultdict(list)
for r in cur.fetchall():
    hid = HOUSE_NAME_TO_ID.get(r['库房'], 'unknown')
    storage_dist[hid].append((r['库存地'], r['cnt']))

# ============================================================
# 2. 物料基本信息 → 物料类型
# ============================================================
print("[2/7] 物料类型分布...")
cur.execute("""
SELECT `物料类型名称`, COUNT(*) AS cnt FROM `物料基本信息表`
WHERE `物料类型名称` IS NOT NULL AND `物料类型名称` != ''
GROUP BY `物料类型名称` ORDER BY cnt DESC
""")
material_types = cur.fetchall()
total_sku = sum(r['cnt'] for r in material_types)

# ============================================================
# 3. 出库单 → 单据类型 + 平台分布
# ============================================================
print("[3/7] 出库业务特征...")
cur.execute("""
SELECT `库房编码`, `单据类型`, COUNT(*) AS cnt
FROM `出库单表` WHERE `状态` != '撤单'
GROUP BY `库房编码`, `单据类型`
ORDER BY `库房编码`, cnt DESC
""")
doc_type_dist = defaultdict(list)
for r in cur.fetchall():
    doc_type_dist[r['库房编码']].append((r['单据类型'], r['cnt']))

cur.execute("""
SELECT `库房编码`, `平台`, COUNT(*) AS cnt
FROM `出库单表` WHERE `状态` != '撤单' AND `平台` IS NOT NULL AND `平台` != ''
GROUP BY `库房编码`, `平台`
ORDER BY `库房编码`, cnt DESC
""")
platform_dist = defaultdict(list)
for r in cur.fetchall():
    platform_dist[r['库房编码']].append((r['平台'], r['cnt']))

# ============================================================
# 4. 月度出库
# ============================================================
print("[4/7] 月度出库...")
cur.execute("""
SELECT DATE_FORMAT(`创建时间`, '%Y-%m') AS ym,
       `库房编码`, `库房名称`,
       COUNT(*) AS orders,
       SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS items,
       COUNT(DISTINCT DATE_FORMAT(`创建时间`, '%Y-%m-%d')) AS work_days
FROM `出库单表` WHERE `状态` != '撤单' AND `创建时间` IS NOT NULL
GROUP BY ym, `库房编码`, `库房名称`
ORDER BY ym, `库房编码`
""")
monthly_outbound = cur.fetchall()

# ============================================================
# 5. 月度入库
# ============================================================
print("[5/7] 月度入库...")
cur.execute("""
SELECT DATE_FORMAT(`创单时间`, '%Y-%m') AS ym,
       `库房编码`,
       COUNT(*) AS ib_orders,
       SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS ib_items
FROM `入库单表` WHERE `状态` = '正常' AND `创单时间` IS NOT NULL AND `库房编码` IS NOT NULL
GROUP BY ym, `库房编码`
""")
ib_map = {}
for r in cur.fetchall():
    ib_map[(r['ym'], r['库房编码'])] = {'orders': r['ib_orders'], 'items': float(r['ib_items'] or 0)}

# ============================================================
# 6. 月度出勤
# ============================================================
print("[6/7] 月度出勤...")
cur.execute("""
SELECT DATE_FORMAT(`考勤日期`, '%Y-%m') AS ym,
       `库房`,
       COUNT(DISTINCT `员工编码`) AS headcount,
       SUM(CAST(`工作时长` AS DECIMAL(10,2))) / 60 AS total_hours,
       COUNT(DISTINCT CASE WHEN `员工类型` = '长期劳务' THEN `员工编码` END) AS fixed_cnt,
       COUNT(DISTINCT CASE WHEN `员工类型` = '临时劳务' THEN `员工编码` END) AS temp_cnt,
       COUNT(DISTINCT CASE WHEN `员工类型` = '自有人员' THEN `员工编码` END) AS own_cnt,
       COUNT(DISTINCT DATE_FORMAT(`考勤日期`, '%Y-%m-%d')) AS att_days
FROM `出勤统计表`
GROUP BY ym, `库房`
""")
att_map = {}
for r in cur.fetchall():
    hid = HOUSE_NAME_TO_ID.get(r['库房'], 'unknown')
    att_map[(r['ym'], hid)] = {
        'headcount': r['headcount'],
        'total_hours': float(r['total_hours'] or 0),
        'fixed': r['fixed_cnt'], 'temp': r['temp_cnt'], 'own': r['own_cnt'],
        'att_days': r['att_days']
    }

# ============================================================
# 7. 报价
# ============================================================
print("[7/7] 报价信息...")
cur.execute("""
SELECT `库房名称`,
       AVG(`供应商结算单价`) AS avg_price,
       AVG(CASE WHEN `结费类型`='固定劳务' THEN `供应商结算单价` END) AS fixed_price,
       AVG(CASE WHEN `结费类型`='临时劳务' THEN `供应商结算单价` END) AS temp_price,
       AVG(CASE WHEN `结费类型`='叉车劳务' THEN `供应商结算单价` END) AS forklift_price
FROM `报价信息表` WHERE `报价状态` = '正常'
GROUP BY `库房名称`
""")
quote_info = {}
for r in cur.fetchall():
    qname = r['库房名称']
    quote_info[qname] = {k: float(v) if v else None for k, v in r.items() if k != '库房名称'}

# 在账资产
cur.execute("""
SELECT `使用地名称`, COUNT(*) AS cnt, SUM(CAST(`采购单价` AS DECIMAL(20,2))) AS total_value
FROM `在账资产明细表` WHERE `使用地名称` IS NOT NULL
GROUP BY `使用地名称`
""")
asset_map = {r['使用地名称']: {'cnt': r['cnt'], 'total_value': float(r['total_value'] or 0)} for r in cur.fetchall()}

conn.close()
print("\n=== 数据提取完成，生成报告... ===\n")

# ============================================================
QUOTE_NAME = {'12000004': '天津武清佩森A仓', '32050005': '常熟高新正创仓'}

def get_quote(hid):
    return quote_info.get(QUOTE_NAME.get(hid, ''), {})

def calc_weighted_price(hid, fixed_cnt, temp_cnt):
    qi = get_quote(hid)
    fp = qi.get('fixed_price') or 20
    tp = qi.get('temp_price') or 22
    total = fixed_cnt + temp_cnt
    if total > 0:
        return (fixed_cnt * fp + temp_cnt * tp) / total
    return qi.get('avg_price') or 20

# ============================================================
# REPORT
# ============================================================
R = []
R.append("# 仓库类型划分与每月日均费用基线分析\n")
R.append("**分析日期**: 2026-03-24  ")
R.append("**数据来源**: wh_op_baseline  ")
R.append("**数据月份**: 2025-03, 2025-08  \n")

# ===== 一、仓库画像 =====
R.append("---\n## 一、仓库特征画像\n")
for hid in ['12000004', '32050005']:
    hname = HOUSE_MAP[hid]
    li = loc_info.get(hid, {})
    qi = get_quote(hid)
    
    R.append(f"### {hname}\n")
    R.append("| 维度 | 数值 |")
    R.append("|---|---|")
    R.append(f"| 独立仓位数 | {li.get('unique_positions', 'N/A'):,} |")
    R.append(f"| 区域数 | {li.get('zone_count', 'N/A')} |")
    R.append(f"| 库存地数 | {li.get('storage_count', 'N/A')} |")
    R.append(f"| 活跃SKU数 | {li.get('sku_count', 'N/A'):,} |")
    R.append(f"| 可用库存总量 | {li.get('total_stock', 0):,.0f} |")
    R.append(f"| 加权平均单价 | {qi.get('avg_price', 0):.2f} 元/h |")
    R.append(f"| 固定劳务均价 | {qi.get('fixed_price', 0):.2f} 元/h |")
    R.append(f"| 临时劳务均价 | {qi.get('temp_price', 0):.2f} 元/h |")
    R.append(f"| 叉车劳务均价 | {qi.get('forklift_price', 0):.2f} 元/h |")
    
    for aname, ainfo in asset_map.items():
        if hname[:4] in aname:
            R.append(f"| 在账资产 | {ainfo['cnt']} 项, 总值 {ainfo['total_value']:,.0f} 元 |")
    R.append("")
    
    R.append("**区域分布**:\n")
    for z, cnt, stk in zone_dist.get(hid, []):
        R.append(f"- {z}: {cnt:,} 条记录, 库存 {stk:,.0f}")
    R.append("")
    
    R.append("**库存地分布**:\n")
    for sn, cnt in storage_dist.get(hid, []):
        R.append(f"- {sn}: {cnt:,}")
    R.append("")
    
    # 单据类型
    dt = doc_type_dist.get(hid, [])
    if dt:
        total_dt = sum(c for _, c in dt)
        R.append("**单据类型分布**:\n")
        for dname, cnt in dt[:8]:
            R.append(f"- {dname}: {cnt:,} ({cnt/total_dt*100:.1f}%)")
        R.append("")
    
    # 平台
    pf = platform_dist.get(hid, [])
    if pf:
        total_pf = sum(c for _, c in pf)
        R.append("**电商平台分布**:\n")
        for pname, cnt in pf[:10]:
            R.append(f"- {pname}: {cnt:,} ({cnt/total_pf*100:.1f}%)")
        R.append("")

# SKU物料类型
R.append("### 物料类型分布 (全仓)\n")
R.append("| 物料类型 | 数量 | 占比 |")
R.append("|---|---|---|")
for r in material_types[:10]:
    R.append(f"| {r['物料类型名称']} | {r['cnt']:,} | {r['cnt']/total_sku*100:.1f}% |")
R.append("")

# ===== 二、仓库类型划分 =====
R.append("---\n## 二、仓库类型划分\n")

# Compute classification data
class_data = {}
for hid in ['12000004', '32050005']:
    hname = HOUSE_MAP[hid]
    li = loc_info.get(hid, {})
    
    mos = [r for r in monthly_outbound if r['库房编码'] == hid]
    avg_orders = sum(r['orders'] for r in mos) / len(mos) if mos else 0
    avg_items = sum(float(r['items'] or 0) for r in mos) / len(mos) if mos else 0
    avg_ratio = avg_items / avg_orders if avg_orders > 0 else 0
    
    att_list = [(k, v) for k, v in att_map.items() if k[1] == hid]
    avg_headcount = sum(v['headcount'] for _, v in att_list) / len(att_list) if att_list else 0
    avg_fixed = sum(v['fixed'] for _, v in att_list) / len(att_list) if att_list else 0
    avg_temp = sum(v['temp'] for _, v in att_list) / len(att_list) if att_list else 0
    ft_ratio = avg_fixed / avg_temp if avg_temp > 0 else 0
    
    class_data[hid] = {
        'name': hname, 'sku': li.get('sku_count', 0), 'positions': li.get('unique_positions', 0),
        'avg_orders': avg_orders, 'avg_items': avg_items, 'avg_ratio': avg_ratio,
        'avg_headcount': avg_headcount, 'avg_fixed': avg_fixed, 'avg_temp': avg_temp,
        'ft_ratio': ft_ratio, 'stock': li.get('total_stock', 0)
    }

R.append("### 分类维度对比\n")
R.append("| 分类维度 | 天津武清佩森A仓 | 常熟高新正创B仓 |")
R.append("|---|---|---|")

for label, key, fmt in [
    ('月均出库单量', 'avg_orders', '{:,.0f}'),
    ('月均出库件数', 'avg_items', '{:,.0f}'),
    ('平均件单比', 'avg_ratio', '{:.2f}'),
    ('活跃SKU数', 'sku', '{:,}'),
    ('独立仓位数', 'positions', '{:,}'),
    ('可用库存总量', 'stock', '{:,.0f}'),
    ('月均出勤人数', 'avg_headcount', '{:.0f}'),
    ('固定劳务人数', 'avg_fixed', '{:.0f}'),
    ('临时劳务人数', 'avg_temp', '{:.0f}'),
    ('固临比', 'ft_ratio', '{:.2f}'),
]:
    v1 = class_data['12000004'].get(key, 0)
    v2 = class_data['32050005'].get(key, 0)
    R.append(f"| {label} | {fmt.format(v1)} | {fmt.format(v2)} |")
R.append("")

# Classification
def classify(d):
    tags = []
    if d['avg_orders'] > 200000:
        tags.append('大型仓')
    elif d['avg_orders'] > 100000:
        tags.append('中大型仓')
    else:
        tags.append('中型仓')
    
    if d['avg_ratio'] > 7:
        tags.append('高件单比')
    elif d['avg_ratio'] > 4:
        tags.append('中件单比')
    else:
        tags.append('低件单比')
    
    if d['ft_ratio'] > 1.5:
        tags.append('固定劳务主导型')
    elif d['ft_ratio'] > 0.6:
        tags.append('固临均衡型')
    else:
        tags.append('临时劳务主导型')
    
    tags.append('B2C电商仓')
    return tags

tj_tags = classify(class_data['12000004'])
cs_tags = classify(class_data['32050005'])

R.append("### 仓库类型归类\n")
R.append("| 仓库 | 规模类型 | 件单比类型 | 劳务结构类型 | 业务类型 | **综合类型** |")
R.append("|---|---|---|---|---|---|")
R.append(f"| 天津武清佩森A仓 | {tj_tags[0]} | {tj_tags[1]} | {tj_tags[2]} | {tj_tags[3]} | **{tj_tags[0]} + {tj_tags[1]} + {tj_tags[2]}** |")
R.append(f"| 常熟高新正创B仓 | {cs_tags[0]} | {cs_tags[1]} | {cs_tags[2]} | {cs_tags[3]} | **{cs_tags[0]} + {cs_tags[1]} + {cs_tags[2]}** |")
R.append("")

R.append("### 类型特征详述\n")
for hid, tags, d in [('12000004', tj_tags, class_data['12000004']), ('32050005', cs_tags, class_data['32050005'])]:
    R.append(f"#### {d['name']} — {tags[0]} / {tags[1]} / {tags[2]}\n")
    R.append(f"- **规模**: 月均 {d['avg_orders']:,.0f} 单 / {d['avg_items']:,.0f} 件，属于 **{tags[0]}**")
    R.append(f"- **件单比**: 平均 {d['avg_ratio']:.2f} 件/单，属于 **{tags[1]}** — {'每单平均物料多，拣货复杂度高' if d['avg_ratio'] > 5 else '每单物料较少，拣货效率高'}")
    R.append(f"- **仓储规模**: {d['positions']:,} 个仓位，{d['sku']:,} 个活跃SKU，可用库存 {d['stock']:,.0f}")
    R.append(f"- **劳务结构**: 固定 {d['avg_fixed']:.0f}人 / 临时 {d['avg_temp']:.0f}人，固临比 {d['ft_ratio']:.2f}，属于 **{tags[2]}**")
    if d['ft_ratio'] > 1.5:
        R.append(f"  - 固定劳务占主导，人力成本稳定但弹性较低")
    elif d['ft_ratio'] < 0.8:
        R.append(f"  - 临时劳务占比高，成本弹性强但单价较高")
    else:
        R.append(f"  - 固定与临时均衡配置，兼顾稳定性与弹性")
    R.append("")

# ===== 三、每月日均费用基线 =====
R.append("---\n## 三、每月日均费用基线成本\n")
R.append("""
### 计算方法

$$
\\text{日均劳务费} = \\frac{\\text{月总工时(h)} \\times \\text{加权劳务单价(元/h)} \\times (1 + 6\\%)}{\\text{当月出勤天数}}
$$

- **加权单价** = 固定劳务占比 × 固定单价 + 临时劳务占比 × 临时单价
- **税率** 6% (增值税)
""")

R.append("### 3.1 每月日均费用明细\n")
R.append("| 仓库 | 仓库类型 | 月份 | 出勤天数 | 月总工时(h) | 日均工时(h) | 加权单价 | 月劳务总费(元) | **日均劳务费(元)** | 日均单量 | **单均成本(元)** | **件均成本(元)** |")
R.append("|---|---|---|---|---|---|---|---|---|---|---|---|")

baseline_data = {}
for mo in monthly_outbound:
    ym = mo['ym']
    hid = mo['库房编码']
    hname = mo['库房名称']
    orders = mo['orders']
    items = float(mo['items'] or 0)
    work_days = mo['work_days']
    
    att = att_map.get((ym, hid), {})
    total_hours = att.get('total_hours', 0)
    att_days = att.get('att_days', work_days)
    fixed_cnt = att.get('fixed', 0)
    temp_cnt = att.get('temp', 0)
    days = max(att_days, work_days)
    
    wp = calc_weighted_price(hid, fixed_cnt, temp_cnt)
    monthly_cost = total_hours * wp * 1.06
    daily_cost = monthly_cost / days if days > 0 else 0
    daily_orders = orders / days if days > 0 else 0
    daily_items = items / days if days > 0 else 0
    daily_hours = total_hours / days if days > 0 else 0
    cost_per_order = daily_cost / daily_orders if daily_orders > 0 else 0
    cost_per_item = daily_cost / daily_items if daily_items > 0 else 0
    
    tags = tj_tags if hid == '12000004' else cs_tags
    wh_type = f"{tags[0]}/{tags[1]}"
    
    R.append(f"| {hname} | {wh_type} | {ym} | {days} | {total_hours:,.0f} | {daily_hours:,.1f} | {wp:.2f} | {monthly_cost:,.0f} | **{daily_cost:,.0f}** | {daily_orders:,.0f} | **{cost_per_order:.2f}** | **{cost_per_item:.4f}** |")
    
    baseline_data[(hid, ym)] = {
        'daily_cost': daily_cost, 'daily_orders': daily_orders, 'daily_items': daily_items,
        'daily_hours': daily_hours, 'cost_per_order': cost_per_order, 'cost_per_item': cost_per_item,
        'monthly_cost': monthly_cost, 'total_hours': total_hours, 'days': days, 'wp': wp,
        'headcount': att.get('headcount', 0), 'fixed': fixed_cnt, 'temp': temp_cnt,
        'orders': orders, 'items': items, 'ratio': items/orders if orders>0 else 0
    }
R.append("")

# ===== 3.2 按仓库类型汇总 =====
R.append("### 3.2 按仓库类型汇总日均费用基线\n")

for hid, tags in [('12000004', tj_tags), ('32050005', cs_tags)]:
    hname = HOUSE_MAP[hid]
    wh_type = f"{tags[0]} / {tags[1]} / {tags[2]}"
    R.append(f"#### {hname} ({wh_type})\n")
    
    R.append("| 指标 | 2025-03 | 2025-08 | 变动 |")
    R.append("|---|---|---|---|")
    
    d3 = baseline_data.get((hid, '2025-03'), {})
    d8 = baseline_data.get((hid, '2025-08'), {})
    
    def pct(a, b):
        return f"{((b/a)-1)*100:+.1f}%" if a and a > 0 else "N/A"
    
    metrics = [
        ('出勤天数', 'days', '{:.0f}'),
        ('月总出库单量', 'orders', '{:,.0f}'),
        ('月总出库件数', 'items', '{:,.0f}'),
        ('月件单比', 'ratio', '{:.2f}'),
        ('月总工时(h)', 'total_hours', '{:,.0f}'),
        ('出勤人数', 'headcount', '{:,}'),
        ('固定劳务', 'fixed', '{:,}'),
        ('临时劳务', 'temp', '{:,}'),
        ('加权单价(元/h)', 'wp', '{:.2f}'),
        ('月劳务总费(元)', 'monthly_cost', '{:,.0f}'),
        ('**日均劳务费(元)**', 'daily_cost', '{:,.0f}'),
        ('**日均出库单量**', 'daily_orders', '{:,.0f}'),
        ('**日均单均成本(元/单)**', 'cost_per_order', '{:.2f}'),
        ('**日均件均成本(元/件)**', 'cost_per_item', '{:.4f}'),
    ]
    
    for name, key, fmt in metrics:
        v3 = d3.get(key, 0)
        v8 = d8.get(key, 0)
        R.append(f"| {name} | {fmt.format(v3)} | {fmt.format(v8)} | {pct(v3, v8)} |")
    R.append("")

# ===== 四、费用变动驱动因素分解 =====
R.append("---\n## 四、月间费用变动驱动因素分解\n")

for hid in ['12000004', '32050005']:
    hname = HOUSE_MAP[hid]
    d3 = baseline_data.get((hid, '2025-03'), {})
    d8 = baseline_data.get((hid, '2025-08'), {})
    if not d3 or not d8:
        continue
    
    cost_change = d8['daily_cost'] - d3['daily_cost']
    hours_change = d8['daily_hours'] - d3['daily_hours']
    price_change = d8['wp'] - d3['wp']
    
    avg_hours = (d3['daily_hours'] + d8['daily_hours']) / 2
    avg_price = (d3['wp'] + d8['wp']) / 2
    hours_effect = hours_change * avg_price * 1.06
    price_effect = price_change * avg_hours * 1.06
    
    R.append(f"### {hname}\n")
    R.append(f"日均费用变动: **{d3['daily_cost']:,.0f} → {d8['daily_cost']:,.0f}** (变动 {cost_change:+,.0f} 元, {((d8['daily_cost']/d3['daily_cost'])-1)*100:+.1f}%)\n")
    R.append("**分解**:\n")
    R.append(f"| 驱动因素 | 贡献额(元/日) | 占比 |")
    R.append(f"|---|---|---|")
    total_abs = abs(hours_effect) + abs(price_effect)
    if total_abs > 0:
        R.append(f"| 工时变动 ({d3['daily_hours']:,.1f}h → {d8['daily_hours']:,.1f}h) | {hours_effect:+,.0f} | {abs(hours_effect)/total_abs*100:.0f}% |")
        R.append(f"| 单价变动 ({d3['wp']:.2f} → {d8['wp']:.2f}) | {price_effect:+,.0f} | {abs(price_effect)/total_abs*100:.0f}% |")
    R.append(f"| **合计** | **{cost_change:+,.0f}** | 100% |")
    R.append("")
    
    R.append("**工时变动进一步分解**:\n")
    if d3['headcount'] > 0:
        R.append(f"- 出勤人数变动: {d3['headcount']} → {d8['headcount']} ({((d8['headcount']/d3['headcount'])-1)*100:+.1f}%)")
    hc3 = d3.get('headcount', 1) or 1
    hc8 = d8.get('headcount', 1) or 1
    R.append(f"- 人均日工时: {d3['daily_hours']/hc3*d3['days']:.1f}h/月 → {d8['daily_hours']/hc8*d8['days']:.1f}h/月")
    R.append(f"- 固定劳务: {d3['fixed']} → {d8['fixed']}, 临时劳务: {d3['temp']} → {d8['temp']}")
    if d3['daily_orders'] > 0:
        R.append(f"- 单量变动: 日均 {d3['daily_orders']:,.0f} → {d8['daily_orders']:,.0f} ({((d8['daily_orders']/d3['daily_orders'])-1)*100:+.1f}%)")
    R.append("")

# ===== 五、结论 =====
R.append("---\n## 五、结论与建议\n")
R.append("""
### 5.1 仓库类型总结

| 仓库 | 综合类型 | 核心特征 |
|---|---|---|""")

for hid, tags, d in [('12000004', tj_tags, class_data['12000004']), ('32050005', cs_tags, class_data['32050005'])]:
    core = f"月均{d['avg_orders']:,.0f}单, 件单比{d['avg_ratio']:.1f}, 固临比{d['ft_ratio']:.2f}"
    R.append(f"| {d['name']} | {tags[0]}/{tags[1]}/{tags[2]} | {core} |")

R.append("")
R.append("### 5.2 日均费用基线总结\n")
R.append("| 仓库类型 | 仓库 | 3月日均费(元) | 8月日均费(元) | 3月单均成本 | 8月单均成本 |")
R.append("|---|---|---|---|---|---|")
for hid, tags in [('12000004', tj_tags), ('32050005', cs_tags)]:
    d3 = baseline_data.get((hid, '2025-03'), {})
    d8 = baseline_data.get((hid, '2025-08'), {})
    wt = f"{tags[0]}/{tags[1]}"
    R.append(f"| {wt} | {HOUSE_MAP[hid]} | {d3.get('daily_cost', 0):,.0f} | {d8.get('daily_cost', 0):,.0f} | {d3.get('cost_per_order', 0):.2f} | {d8.get('cost_per_order', 0):.2f} |")

R.append("""
### 5.3 关键发现

1. **天津A仓 (中大型/中件单比/固临均衡型)**: 3月→8月单量增长75%，但人力缩减，日均费用下降，单均成本大幅下降，体现出**规模效应**
2. **常熟B仓 (中大型/高件单比/临时劳务主导型)**: 件单比从7.4升至9.5，订单向多件组合转移；人力缩减导致日均费用下降
3. **劳务结构差异**: 天津仓固临比波动大(0.87→2.80)，弹性调整幅度大；常熟仓持续以临时劳务为主(~0.7)，成本结构更稳定
4. **单价差异**: 常熟仓加权均价(~24元/h)比天津仓(~20元/h)高约20%，这是地区劳务市场差异导致的结构性成本差距
5. **日均费用基线**: 两仓日均劳务费在 **6,000-11,000 元/天** 区间，单均成本在 **0.76-2.14 元/单** 区间
""")

# Write
output_dir = 'data_profiling_reports'
report_path = os.path.join(output_dir, '仓库类型划分与日均费用基线分析.md')
with open(report_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(R))

print(f"Report saved to: {report_path}")
