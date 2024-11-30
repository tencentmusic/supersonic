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

CREATE TABLE IF NOT EXISTS `singer` (
    `singer_name` varchar(200) NOT NULL,
    `act_area` varchar(200) NOT NULL,
    `song_name` varchar(200) NOT NULL,
    `genre` varchar(200) NOT NULL,
    `js_play_cnt` bigint DEFAULT NULL,
    `down_cnt` bigint DEFAULT NULL,
    `favor_cnt` bigint DEFAULT NULL
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- S2ArtistDemo
CREATE TABLE IF NOT EXISTS `genre` (
    `g_name` varchar(20) NOT NULL , -- genre name
    `rating` INT ,
    `most_popular_in` varchar(50) ,
    PRIMARY KEY (`g_name`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `artist` (
    `artist_name` varchar(50) NOT NULL , -- genre name
    `citizenship` varchar(20) ,
    `gender` varchar(20) ,
    `g_name` varchar(50)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `files` (
     `f_id` bigINT NOT NULL,
     `artist_name` varchar(50) ,
    `file_size` varchar(20) ,
    `duration` varchar(20) ,
    `formats` varchar(20) ,
    PRIMARY KEY (`f_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `song` (
    `imp_date` varchar(50) ,
    `song_name` varchar(50) ,
    `artist_name` varchar(50) ,
    `country` varchar(20) ,
    `f_id` bigINT ,
    `g_name` varchar(20) ,
    `rating` int ,
    `languages` varchar(20) ,
    `releasedate` varchar(50) ,
    `resolution` bigINT NOT NULL
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;