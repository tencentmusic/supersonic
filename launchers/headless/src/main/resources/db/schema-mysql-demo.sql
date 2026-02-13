-- S2VisitsDemo
CREATE TABLE IF NOT EXISTS `s2_user_department` (
      `user_name` varchar(200) NOT NULL,
       `department` varchar(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_pv_uv_statis` (
      `imp_date` varchar(200) NOT NULL,
      `user_name` varchar(200) NOT NULL,
      `page` varchar(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `s2_stay_time_statis` (
       `imp_date` varchar(200) NOT NULL,
       `user_name` varchar(200) NOT NULL,
       `stay_hours` DOUBLE NOT NULL,
       `page` varchar(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S2ArtistDemo
CREATE TABLE IF NOT EXISTS `singer` (
    `singer_name` varchar(200) NOT NULL,
    `act_area` varchar(200) NOT NULL,
    `song_name` varchar(200) NOT NULL,
    `genre` varchar(200) NOT NULL,
    `js_play_cnt` bigint DEFAULT NULL,
    `down_cnt` bigint DEFAULT NULL,
    `favor_cnt` bigint DEFAULT NULL
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `genre` (
    `g_name` varchar(20) NOT NULL , -- genre name
    `rating` INT ,
    `most_popular_in` varchar(50) ,
    PRIMARY KEY (`g_name`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S2CompanyDemo
CREATE TABLE IF NOT EXISTS `company` (
    `company_id` varchar(50) NOT NULL,
    `company_name` varchar(50) NOT NULL,
    `headquarter_address` varchar(50) NOT NULL,
    `company_established_time` varchar(20) NOT NULL,
    `founder` varchar(20) NOT NULL,
    `ceo` varchar(20) NOT NULL,
    `annual_turnover` bigint(15),
    `employee_count` int(7),
    PRIMARY KEY (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `brand` (
    `brand_id` varchar(50) NOT NULL,
    `brand_name` varchar(50) NOT NULL,
    `brand_established_time` varchar(20) NOT NULL,
    `company_id` varchar(50) NOT NULL,
    `legal_representative` varchar(20) NOT NULL,
    `registered_capital` bigint(15),
    PRIMARY KEY (`brand_id`),
    KEY `idx_company_id` (`company_id`),
    CONSTRAINT `fk_brand_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `brand_revenue` (
    `year_time` varchar(10) NOT NULL,
    `brand_id` varchar(50) NOT NULL,
    `revenue` bigint(15) NOT NULL,
    `profit` bigint(15) NOT NULL,
    `revenue_growth_year_on_year` double NOT NULL,
    `profit_growth_year_on_year` double NOT NULL,
    PRIMARY KEY (`year_time`, `brand_id`),
    KEY `idx_brand_id` (`brand_id`),
    CONSTRAINT `fk_brand_revenue_brand` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;