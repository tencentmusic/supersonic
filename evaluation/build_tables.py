import sqlite3
import os
import datetime
import yaml

def build_internet(path,day):
    imp_date=(datetime.datetime.now()+datetime.timedelta(days=day)).strftime("%Y-%m-%d")
    print(imp_date)
    conn = sqlite3.connect(path+'/internet.db')
    cursor = conn.cursor()
    create_table_query = '''
    CREATE TABLE IF NOT EXISTS company (
        `imp_date` varchar(50) ,
        `company_id` varchar(50) NOT NULL ,
        `company_name` varchar(50) NOT NULL ,
        `headquarter_address` varchar(50) NOT NULL ,
        `company_established_time` varchar(20) NOT NULL ,
        `founder` varchar(20) NOT NULL ,
        `ceo` varchar(20) NOT NULL ,
        `annual_turnover` bigint(15)  ,
        `employee_count` int(7) ,
        PRIMARY KEY (`company_id`)
    )
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO company (imp_date,company_id,company_name,headquarter_address,company_established_time,founder,ceo,annual_turnover,employee_count)
    VALUES (?, ?, ?,?, ?, ?,?, ?, ?)
    '''
    data = [
        (imp_date,"item_enterprise_13_131","百度集团","北京","2000","李彦宏","李彦宏",102300000000,40000),
        (imp_date,"item_enterprise_13_132","阿里巴巴集团","杭州","1999年","马云","张勇",376800000000,103699),
        (imp_date,"item_enterprise_13_133","深圳市腾讯计算机系统有限公司","深圳","1998","马化腾","刘炽平",321600000000,56310),
        (imp_date,"item_enterprise_13_134","北京京东世纪贸易有限公司","北京","1998","刘强东","刘强东",28800000000,179000),
        (imp_date,"item_enterprise_13_135","网易公司","杭州","1997","丁磊","丁磊",67500000000,20000)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
    CREATE TABLE IF NOT EXISTS brand (
        `imp_date` varchar(50) ,
        `brand_id` varchar(50) NOT NULL ,
        `brand_name` varchar(50) NOT NULL ,
        `brand_established_time` varchar(20) NOT NULL ,
        `company_id` varchar(50) NOT NULL ,
        `legal_representative` varchar(20) NOT NULL ,
        `registered_capital` bigint(15)  ,
        PRIMARY KEY (`brand_id`)
    )
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO brand (imp_date,brand_id,brand_name,brand_established_time,company_id,legal_representative,registered_capital)
    VALUES (?, ?, ?,?, ?, ?,?)
    '''
    data = [
        (imp_date,"item_enterprise_13_136","阿里云","2009年9月10日","item_enterprise_13_132","张勇",50000000),
        (imp_date,"item_enterprise_13_137","天猫","2012年1月11日","item_enterprise_13_132","张勇",100000000),
        (imp_date,"item_enterprise_13_138","腾讯游戏","2003","item_enterprise_13_133","马化腾",50000000),
        (imp_date,"item_enterprise_13_139","度小满","2018","item_enterprise_13_131","朱光",100000000),
        (imp_date,"item_enterprise_13_140","京东金融","2017","item_enterprise_13_134","刘强东",100000000)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
    CREATE TABLE IF NOT EXISTS company_revenue (
        `imp_date` varchar(50) ,
        `company_id` varchar(50) NOT NULL ,
        `brand_id` varchar(50) NOT NULL ,
        `revenue_proportion` double NOT NULL,
        `profit_proportion` double NOT NULL ,
        `expenditure_proportion` double NOT NULL
    )
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO company_revenue (imp_date,company_id,brand_id,revenue_proportion,profit_proportion,expenditure_proportion)
    VALUES (?, ?, ?,?, ?, ?)
    '''
    data = [
        (imp_date,"item_enterprise_13_131","item_enterprise_13_139",0.1,0.1,0.3),
        (imp_date,"item_enterprise_13_133","item_enterprise_13_138",0.8,0.8,0.6),
        (imp_date,"item_enterprise_13_134","item_enterprise_13_140",0.8,0.8,0.6),
        (imp_date,"item_enterprise_13_132","item_enterprise_13_137",0.8,0.8,0.6),
        (imp_date,"item_enterprise_13_132","item_enterprise_13_136",0.1,0.1,0.3)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
    CREATE TABLE IF NOT EXISTS company_brand_revenue (
        `imp_date` varchar(50) ,
        `year_time` varchar(10) NOT NULL , 
        `brand_id` varchar(50) NOT NULL ,
        `revenue` bigint(15) NOT NULL,
        `profit` bigint(15) NOT NULL , 
        `revenue_growth_year_on_year` double NOT NULL ,
        `profit_growth_year_on_year` double NOT NULL
    )
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO company_brand_revenue (imp_date,year_time,brand_id,revenue,profit,revenue_growth_year_on_year,profit_growth_year_on_year)
    VALUES (?, ?, ?,?, ?, ?,?)
    '''
    data = [
        (imp_date, "2018", "item_enterprise_13_138", 500000000, -300000000, 0.1, -0.1),
        (imp_date, "2019", "item_enterprise_13_136", 100000000000, 50000000000, 1, 0.5),
        (imp_date, "2018", "item_enterprise_13_137", 100000000000, 50000000000, 1, -0.1),
        (imp_date, "2018", "item_enterprise_13_139", 500000000, 50000000000, 0.1, 0.5),
        (imp_date, "2018", "item_enterprise_13_140", 100000000000, -300000000, 0.1, 0.5)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()
    conn.close()
def build_china_travel_agency(path,day):
    imp_date=(datetime.datetime.now()+datetime.timedelta(days=day)).strftime("%Y-%m-%d")
    print(imp_date)
    conn = sqlite3.connect(path+'/china_travel_agency.db')
    cursor = conn.cursor()
    create_table_query = '''
    CREATE TABLE IF NOT EXISTS `travel_agency` (
    `imp_date` varchar(50) ,
    `travel_agency_id` varchar(50) NOT NULL,
    `travel_agency_name` varchar(50) NOT NULL,
    `travel_agency_level` varchar(50) NOT NULL,
    `number_countrie_outbound_travel` int(7) ,
    `number_domestic_tourist_cities` int(7) ,
    `number_outbound_travel_routes` int(7) ,
    `number_domestic_travel_routes` int(7) ,
    `asia_ranking` int(7) ,
    `number_overseas_tourists_received` int(7) ,
    `number_overseas_companies` int(7) ,
    `number_holding_subsidiaries` int(7) ,
    `number_traveling_salesmen_business_relationships` int(7) ,
    `number_duty_free_shops` int(7) ,
    PRIMARY KEY (`travel_agency_id`)
)

    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO travel_agency (imp_date, travel_agency_id,travel_agency_name,travel_agency_level,number_countrie_outbound_travel,
    number_domestic_tourist_cities,number_outbound_travel_routes,number_domestic_travel_routes,
    asia_ranking,number_overseas_tourists_received,number_overseas_companies,number_holding_subsidiaries,
    number_traveling_salesmen_business_relationships,number_duty_free_shops)
    VALUES (?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?, ?, ?)
    '''
    data = [
        (imp_date,"item_enterprise_7_56","中国国旅","3A",50,30,500,1000,1,10000000,5,70,5000,100),
        (imp_date,"item_enterprise_7_57","中旅总社","4A",90,100,1000,2000,100,20000000,20,120,8000,180),
        (imp_date,"item_enterprise_7_58","中青旅控股股份有限公司","5A",50,30,500,2000,1,10000000,5,70,5000,100),
        (imp_date,"item_enterprise_7_59","中国康辉旅游集团有限公司","5A",50,30,1000,1000,100,10000000,5,70,8000,100),
        (imp_date,"item_enterprise_7_60","众信旅游集团股份有限公司","5A",90,30,1000,2000,1,20000000,20,70,8000,180)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
    CREATE TABLE IF NOT EXISTS `outbound_travel_routes` (
    `imp_date` varchar(50) ,
    `outbound_route_id` varchar(50) NOT NULL , 
    `outbound_route_name` varchar(50) NOT NULL ,
    `travel_agency_id` varchar(50) NOT NULL ,
    `outbound_departure_city` varchar(50) NOT NULL ,
    `outbound_days` bigint(5)  ,
    `adult_price` bigint(5)  ,
    `child_price` bigint(5)  ,
    `countries` bigint(5) ,
    `attractions` bigint(5) ,
    `total_ticket_price` bigint(5)  ,  
    PRIMARY KEY (`outbound_route_id`)
)
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO outbound_travel_routes (imp_date, outbound_route_id,outbound_route_name,travel_agency_id,
outbound_departure_city,outbound_days,adult_price,child_price,countries,attractions,total_ticket_price)
    VALUES (?, ?, ?,?, ?, ?,?,?, ?, ?,?)
    '''
    data = [
        (imp_date,"item_enterprise_7_61","德法瑞意深度纵览一价无忧","item_enterprise_7_59","北京",12,10900,8900,5,15,750),
        (imp_date,"item_enterprise_7_62","意大利全景+西西里精华深度纵览","item_enterprise_7_59","天津",20,18900,15900,10,25,2500),
        (imp_date,"item_enterprise_7_63","悦选意大利经典大城小镇书香之旅","item_enterprise_7_57","上海",20,10900,8900,5,15,2500),
        (imp_date,"item_enterprise_7_64","新西兰南岛双冰川双峡湾深度纯净之旅","item_enterprise_7_59","哈尔滨",12,18900,8900,5,15,2500),
        (imp_date,"item_enterprise_7_65","英国+爱尔兰+威尔士精选之旅","item_enterprise_7_57","深圳",12,18900,15900,10,15,750)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
    CREATE TABLE IF NOT EXISTS `country_outbound_travel` (
    `imp_date` varchar(50) ,
    `outbound_travel_route_id` varchar(50) NOT NULL , 
    `nation` varchar(20) NOT NULL ,
    `travel_days` int(6) NOT NULL ,
    `outbound_number_attractions` int(6) NOT NULL 
)
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO country_outbound_travel (imp_date, outbound_travel_route_id,nation,travel_days,outbound_number_attractions)
    VALUES (?, ?, ?,?, ?)
    '''
    data = [
        (imp_date,"item_enterprise_7_64","意大利",2,3),
        (imp_date,"item_enterprise_7_63","法国",4,5),
        (imp_date,"item_enterprise_7_62","德国",4,5),
        (imp_date,"item_enterprise_7_65","瑞士",4,3),
        (imp_date,"item_enterprise_7_61","英格兰",4,3)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()

    create_table_query = '''
    CREATE TABLE IF NOT EXISTS `domestic_travel_routes` (
    `imp_date` varchar(50) ,
    `domestic_travel_route_id` varchar(50) NOT NULL , 
    `domestic_travel_route_name` varchar(50) NOT NULL , 
    `travel_agency_id` varchar(50) NOT NULL ,
    `domestic_departure_city` varchar(50) NOT NULL ,
    `domestic_days` int(5)  ,
    `presale_price` int(8) NOT NULL ,
    `tour_price` int(8) NOT NULL ,
    `number_people_group` int(8) NOT NULL ,
    `personal_price` int(8) NOT NULL ,
    `domestic_number_attractions` int(6) NOT NULL ,  
    PRIMARY KEY (`domestic_travel_route_id`)
 )
    '''
    cursor.execute(create_table_query)
    insert_data_query = '''
    INSERT INTO domestic_travel_routes (imp_date, domestic_travel_route_id,domestic_travel_route_name,travel_agency_id,domestic_departure_city,domestic_days,presale_price,tour_price,number_people_group,personal_price,domestic_number_attractions)
    VALUES (?, ?, ?,?, ?, ?,?,?, ?, ?,?)
    '''
    data = [
        (imp_date,"item_enterprise_7_66","桂林深度跟团游","item_enterprise_7_60",'北京',4,2500,2000,2,3000,10),
        (imp_date,"item_enterprise_7_67","厦门休闲游","item_enterprise_7_56",'天津',8,6500,5000,5,7000,20),
        (imp_date,"item_enterprise_7_68","重庆红色之旅","item_enterprise_7_60",'上海',4,6500,2000,5,3000,20),
        (imp_date,"item_enterprise_7_69","云南古城游","item_enterprise_7_59",'哈尔滨',4,6500,2000,5,7000,20),
        (imp_date,"item_enterprise_7_70","上海时尚游","item_enterprise_7_59",'深圳',4,6500,2000,5,7000,10)
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()


    create_table_query = '''
        CREATE TABLE IF NOT EXISTS `cruise_route` (
    `imp_date` varchar(50) ,
    `cruise_route_id` varchar(50) NOT NULL , 
    `cruise_route_name` varchar(50) NOT NULL , 
    `travel_agency_id` varchar(50) NOT NULL ,
    `cruise_departure_city` varchar(50) NOT NULL ,
    `cruise_days` int(5)  ,
    `interior_cabin_price` int(8) NOT NULL ,
    `sea_view_room_price` int(8) NOT NULL ,
    `balcony_room_price` int(8) NOT NULL ,
    `sailing_area` varchar(50) NOT NULL ,
    `cruise_line` varchar(50) NOT NULL ,  
    PRIMARY KEY (`cruise_route_id`)
 )
        '''
    cursor.execute(create_table_query)
    insert_data_query = '''
        INSERT INTO cruise_route (imp_date, cruise_route_id,cruise_route_name,travel_agency_id,cruise_departure_city,cruise_days,interior_cabin_price,sea_view_room_price,balcony_room_price,sailing_area,cruise_line)
        VALUES (?, ?, ?,?, ?, ?,?,?, ?, ?,?)
        '''
    data = [
        (imp_date,"item_enterprise_7_71","南极摄影旅游","item_enterprise_7_57",'大连',6,4399,4799,5299,"日本航线","皇家加勒比国际游轮"),
        (imp_date,"item_enterprise_7_72","地中海巡游","item_enterprise_7_58",'天津',10,6399,6799,7399,"韩国航线","海洋亚特兰蒂游轮"),
        (imp_date,"item_enterprise_7_73","超凡体验来自未来的游轮","item_enterprise_7_60",'上海',10,4399,4799,5299,"南极航线","庞洛游轮"),
        (imp_date,"item_enterprise_7_74","超凡体验来自未来的游轮","item_enterprise_7_60",'深圳',10,6399,6799,5299,"南极航线","庞洛游轮"),
        (imp_date,"item_enterprise_7_75","超凡体验来自未来的游轮","item_enterprise_7_60",'天津',10,6399,4799,5299,"韩国航线","海洋亚特兰蒂游轮")
    ]
    cursor.executemany(insert_data_query, data)
    conn.commit()
    conn.close()
def build_table():
    current_directory = os.path.dirname(os.path.abspath(__file__))
    config_file=current_directory+"/config/config.yaml"
    with open(config_file, 'r') as file:
        config = yaml.safe_load(file)
    db_path=current_directory+"/data/"
    db_file=db_path+"internet.db"
    db_exist=os.path.exists(db_file)
    if db_exist:
        os.remove(db_file)
        print("db_file removed!")
    print(db_file)
    build_internet(db_path,0)
if __name__ == '__main__':
    current_directory = os.path.dirname(os.path.abspath(__file__))
    config_file=current_directory+"/config/config.yaml"
    with open(config_file, 'r') as file:
        config = yaml.safe_load(file)
    db_path=current_directory+"/data/"
    db_file=db_path+"internet.db"
    db_exist=os.path.exists(db_file)
    if db_exist:
        os.remove(db_file)
        print("db_file removed!")
    print(db_file)
    build_internet(db_path,-1)
    #build_china_travel_agency(path)


