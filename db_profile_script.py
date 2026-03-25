import pymysql
import os

output_dir = 'data_profiling_reports'
os.makedirs(output_dir, exist_ok=True)

try:
    conn = pymysql.connect(
        host='10.126.50.199',
        port=3306,
        user='fdeuser',
        password='FDE2026!',
        database='wh_op_baseline',
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )
    cur = conn.cursor()
    
    cur.execute('SHOW TABLES')
    tables = [row['Tables_in_wh_op_baseline'] for row in cur.fetchall()]
    print(f'Found {len(tables)} tables. Generating profiling reports...')
    
    for table in tables:
        print(f'Profiling table: {table}')
        md_content = f'# 数据探查报告：{table}\n\n'
        
        cur.execute(f'SELECT COUNT(*) as total FROM `{table}`')
        total_rows = cur.fetchone()['total']
        md_content += f'**表名**: `{table}`  \n'
        md_content += f'**总行数**: {total_rows}  \n\n'
        
        if total_rows == 0:
            md_content += '⚠️ **该表当前为空数据**。\n'
            # Save and continue
            with open(os.path.join(output_dir, f'{table}.md'), 'w', encoding='utf-8') as f:
                f.write(md_content)
            continue
            
        cur.execute(f'DESCRIBE `{table}`')
        columns = cur.fetchall()
        
        md_content += '## 字段探查结果\n\n'
        md_content += '| 字段名 | 数据类型 | 空值数量 | 空值率 | 唯一值数量 | 最小值 | 最大值 | 常见值分布 (Top 3) |\n'
        md_content += '|---|---|---|---|---|---|---|---|\n'
        
        for col in columns:
            col_name = col['Field']
            col_type = col['Type']
            
            # Use indexed counts for faster performance when skipping full scans
            cur.execute(f'SELECT COUNT(*) as null_count FROM `{table}` WHERE `{col_name}` IS NULL')
            null_count = cur.fetchone()['null_count']
            null_rate = f'{(null_count/total_rows)*100:.2f}%'
            
            dist_count = 'N/A'
            min_val = 'N/A'
            max_val = 'N/A'
            top_values = 'N/A'
            
            # Safe heuristics: if rows > 50,000, don't perform heavy aggregates
            if total_rows > 50000:
                dist_count = 'Skipped(>50k rows)'
                min_val = 'Skipped'
                max_val = 'Skipped'
                top_values = 'Skipped'
            else:
                try:
                    cur.execute(f'SELECT COUNT(DISTINCT `{col_name}`) as dist_count FROM `{table}`')
                    dist_count = str(cur.fetchone()['dist_count'])
                except:
                    pass
                
                if 'text' not in col_type.lower() and 'blob' not in col_type.lower() and 'json' not in col_type.lower() and 'long' not in col_type.lower():
                    try:
                        cur.execute(f'SELECT MIN(`{col_name}`) as min_v, MAX(`{col_name}`) as max_v FROM `{table}`')
                        min_max = cur.fetchone()
                        min_val = str(min_max['min_v']) if min_max['min_v'] is not None else 'N/A'
                        max_val = str(min_max['max_v']) if min_max['max_v'] is not None else 'N/A'
                    except Exception:
                        pass
                    
                    try:
                        cur.execute(f'SELECT `{col_name}` as val, COUNT(*) as c FROM `{table}` GROUP BY `{col_name}` ORDER BY c DESC LIMIT 3')
                        top_rows = cur.fetchall()
                        top_values_arr = []
                        for r in top_rows:
                            val_str = str(r['val']) if r['val'] is not None else 'NULL'
                            top_values_arr.append(val_str + '(' + str(r['c']) + ')')
                        top_values = ', '.join(top_values_arr)
                    except Exception:
                        pass
            
            min_val = min_val.replace('\n', ' ').replace('|', '!') 
            max_val = max_val.replace('\n', ' ').replace('|', '!')
            top_values = top_values.replace('\n', ' ').replace('|', '!')
            md_content += f'| {col_name} | {col_type} | {null_count} | {null_rate} | {dist_count} | {min_val} | {max_val} | {top_values} |\n'
            
        with open(os.path.join(output_dir, f'{table}.md'), 'w', encoding='utf-8') as f:
            f.write(md_content)
            
    conn.close()
    print(f'\nData profiling complete! Saved to folder: {output_dir}/')
except Exception as e:
    print('Error:', e)
