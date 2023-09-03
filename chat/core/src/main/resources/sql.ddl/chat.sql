CREATE TABLE `chat_context`
(
    `chat_id`        bigint(20) NOT NULL COMMENT 'context chat id',
    `modified_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'row modify time',
    `user`           varchar(64) DEFAULT NULL COMMENT 'row modify user',
    `query_text`     text DEFAULT NULL COMMENT 'query text',
    `semantic_parse` text DEFAULT NULL COMMENT 'parse data',
    `ext_data`       text DEFAULT NULL COMMENT 'extend data',
    PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `chat`
(
    `chat_id`       bigint(8) NOT NULL AUTO_INCREMENT,
    `chat_name`     varchar(100) DEFAULT NULL,
    `create_time`   datetime     DEFAULT NULL,
    `last_time`     datetime     DEFAULT NULL,
    `creator`       varchar(30)  DEFAULT NULL,
    `last_question` varchar(200) DEFAULT NULL,
    `is_delete`     int(2) DEFAULT '0' COMMENT 'is deleted',
    `is_top`        int(2) DEFAULT '0' COMMENT 'is top',
    PRIMARY KEY (`chat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `chat_query`
(
    `id`                bigint(11) NOT NULL AUTO_INCREMENT,
    `question_id`       bigint(11) DEFAULT NULL,
    `create_time`       datetime      DEFAULT NULL,
    `user_name`         varchar(150)  DEFAULT NULL COMMENT '',
    `question`          varchar(300)  DEFAULT NULL COMMENT '',
    `query_result`      mediumtext,
    `time`              datetime      DEFAULT NULL,
    `state`             int(1) DEFAULT NULL,
    `data_content`      varchar(30)   DEFAULT NULL,
    `name`              varchar(100)  DEFAULT NULL,
    `scene_type`        int(2) DEFAULT NULL,
    `query_type`        int(2) DEFAULT NULL,
    `is_deleted`        int(1) DEFAULT NULL,
    `module`            varchar(30)   DEFAULT NULL,
    `entity`            mediumtext COMMENT '',
    `chat_id`     bigint(8) DEFAULT NULL COMMENT 'chat id',
    `recommend`         text,
    `aggregator`        varchar(20)   DEFAULT 'trend',
    `top_num`           int(3) DEFAULT NULL,
    `start_time`        varchar(30)   DEFAULT NULL,
    `end_time`          varchar(30)   DEFAULT NULL,
    `compare_recommend` text,
    `compare_entity`    text,
    `query_sql`         text,
    `columns`           varchar(2000) DEFAULT NULL,
    `result_list`       text,
    `main_entity`       varchar(5000) DEFAULT NULL,
    `semantic_text`     varchar(5000) DEFAULT NULL,
    `score`             int(11) DEFAULT '0',
    `feedback`          varchar(1024) DEFAULT '',
    PRIMARY KEY (`id`),
    KEY                 `common` (`question_id`),
    KEY                 `common1` (`user_name`),
    KEY                 `common2` (`chat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;