import datetime
import os
import sqlite3

from eval_config import load_config


def build_internet(path, day):
    imp_date = (datetime.datetime.now() + datetime.timedelta(days=day)).strftime("%Y-%m-%d")
    print(imp_date)
    conn = sqlite3.connect(path)
    cursor = conn.cursor()
    create_table_query = """
    CREATE TABLE IF NOT EXISTS company (
        `imp_date` varchar(50),
        `company_id` varchar(50) NOT NULL,
        `company_name` varchar(50) NOT NULL,
        `headquarter_address` varchar(50) NOT NULL,
        `company_established_time` varchar(20) NOT NULL,
        `founder` varchar(20) NOT NULL,
        `ceo` varchar(20) NOT NULL,
        `annual_turnover` bigint(15),
        `employee_count` int(7),
        PRIMARY KEY (`company_id`)
    )
    """
    cursor.execute(create_table_query)
    insert_data_query = """
    INSERT INTO company (imp_date, company_id, company_name, headquarter_address, company_established_time,
    founder, ceo, annual_turnover, employee_count)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    data = [
        (imp_date, "item_enterprise_13_131", "百度集团", "北京", "2000", "李彦宏", "李彦宏", 102300000000, 40000),
        (imp_date, "item_enterprise_13_132", "阿里巴巴集团", "杭州", "1999年", "马云", "张勇", 376800000000, 103699),
        (imp_date, "item_enterprise_13_133", "深圳市腾讯计算机系统有限公司", "深圳", "1998", "马化腾", "刘炽平", 321600000000, 56310),
        (imp_date, "item_enterprise_13_134", "北京京东世纪贸易有限公司", "北京", "1998", "刘强东", "刘强东", 28800000000, 179000),
        (imp_date, "item_enterprise_13_135", "网易公司", "杭州", "1997", "丁磊", "丁磊", 67500000000, 20000),
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()

    create_table_query = """
    CREATE TABLE IF NOT EXISTS brand (
        `imp_date` varchar(50),
        `brand_id` varchar(50) NOT NULL,
        `brand_name` varchar(50) NOT NULL,
        `brand_established_time` varchar(20) NOT NULL,
        `company_id` varchar(50) NOT NULL,
        `legal_representative` varchar(20) NOT NULL,
        `registered_capital` bigint(15),
        PRIMARY KEY (`brand_id`)
    )
    """
    cursor.execute(create_table_query)
    insert_data_query = """
    INSERT INTO brand (imp_date, brand_id, brand_name, brand_established_time, company_id, legal_representative, registered_capital)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    data = [
        (imp_date, "item_enterprise_13_136", "阿里云", "2009年9月10日", "item_enterprise_13_132", "张勇", 50000000),
        (imp_date, "item_enterprise_13_137", "天猫", "2012年1月11日", "item_enterprise_13_132", "张勇", 100000000),
        (imp_date, "item_enterprise_13_138", "腾讯游戏", "2003", "item_enterprise_13_133", "马化腾", 50000000),
        (imp_date, "item_enterprise_13_139", "度小满", "2018", "item_enterprise_13_131", "朱光", 100000000),
        (imp_date, "item_enterprise_13_140", "京东金融", "2017", "item_enterprise_13_134", "刘强东", 100000000),
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()

    create_table_query = """
    CREATE TABLE IF NOT EXISTS company_revenue (
        `imp_date` varchar(50),
        `company_id` varchar(50) NOT NULL,
        `brand_id` varchar(50) NOT NULL,
        `revenue_proportion` double NOT NULL,
        `profit_proportion` double NOT NULL,
        `expenditure_proportion` double NOT NULL
    )
    """
    cursor.execute(create_table_query)
    insert_data_query = """
    INSERT INTO company_revenue (imp_date, company_id, brand_id, revenue_proportion, profit_proportion, expenditure_proportion)
    VALUES (?, ?, ?, ?, ?, ?)
    """
    data = [
        (imp_date, "item_enterprise_13_131", "item_enterprise_13_139", 0.1, 0.1, 0.3),
        (imp_date, "item_enterprise_13_133", "item_enterprise_13_138", 0.8, 0.8, 0.6),
        (imp_date, "item_enterprise_13_134", "item_enterprise_13_140", 0.8, 0.8, 0.6),
        (imp_date, "item_enterprise_13_132", "item_enterprise_13_137", 0.8, 0.8, 0.6),
        (imp_date, "item_enterprise_13_132", "item_enterprise_13_136", 0.1, 0.1, 0.3),
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()

    create_table_query = """
    CREATE TABLE IF NOT EXISTS company_brand_revenue (
        `imp_date` varchar(50),
        `year_time` varchar(10) NOT NULL,
        `brand_id` varchar(50) NOT NULL,
        `revenue` bigint(15) NOT NULL,
        `profit` bigint(15) NOT NULL,
        `revenue_growth_year_on_year` double NOT NULL,
        `profit_growth_year_on_year` double NOT NULL
    )
    """
    cursor.execute(create_table_query)
    insert_data_query = """
    INSERT INTO company_brand_revenue (imp_date, year_time, brand_id, revenue, profit, revenue_growth_year_on_year, profit_growth_year_on_year)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    data = [
        (imp_date, "2018", "item_enterprise_13_138", 500000000, -300000000, 0.1, -0.1),
        (imp_date, "2019", "item_enterprise_13_136", 100000000000, 50000000000, 1, 0.5),
        (imp_date, "2018", "item_enterprise_13_137", 100000000000, 50000000000, 1, -0.1),
        (imp_date, "2018", "item_enterprise_13_139", 500000000, 50000000000, 0.1, 0.5),
        (imp_date, "2018", "item_enterprise_13_140", 100000000000, -300000000, 0.1, 0.5),
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()
    conn.close()


def build_table():
    config = load_config()
    os.makedirs(config.data_dir, exist_ok=True)
    if config.db_path.exists():
        config.db_path.unlink()
        print("db_file removed!")
    print(str(config.db_path))
    build_internet(str(config.db_path), 0)


if __name__ == "__main__":
    build_table()
