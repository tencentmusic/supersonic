import sqlite3
import os
import datetime
import yaml

def build_internet(path):
    conn = sqlite3.connect(path)
    schema_path = os.path.join(os.path.dirname(__file__), "data", "internet_json", "schema.sql")
    with open(schema_path, "r", encoding="utf-8") as f:
        sql_script = f.read()
    conn.executescript(sql_script)
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
    # create database.sqlite
    db_file = os.path.join(os.path.dirname(__file__), "data", "tmp", "database.sqlite")
    if os.path.exists(db_file):
        os.remove(db_file)
        print("database.sqlite removed!")
    print(db_file)
    # insert data into database.sqlite
    conn = sqlite3.connect(db_file)
    schema_path = os.path.join(os.path.dirname(__file__), "data", "tmp", "schema.sql")
    with open(schema_path, "r", encoding="utf-8") as f:
        sql_script = f.read()
    conn.executescript(sql_script)
    conn.commit()
    conn.close()
if __name__ == '__main__':
    build_table()


