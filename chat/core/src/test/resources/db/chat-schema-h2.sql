CREATE TABLE `chat_context`
(
    `chat_id`        BIGINT NOT NULL , -- context chat id
    `modified_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP , -- row modify time
    `user`           varchar(64) DEFAULT NULL , -- row modify user
    `query_text`     LONGVARCHAR DEFAULT NULL , -- query text
    `semantic_parse` LONGVARCHAR DEFAULT NULL , -- parse data
    `ext_data`       LONGVARCHAR DEFAULT NULL , -- extend data
    PRIMARY KEY (`chat_id`)
);


CREATE TABLE `chat`
(
    `chat_id`       BIGINT NOT NULL ,-- AUTO_INCREMENT,
    `chat_name`     varchar(100) DEFAULT NULL,
    `create_time`   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
    `last_time`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
    `creator`       varchar(30)  DEFAULT NULL,
    `last_question` varchar(200) DEFAULT NULL,
    `is_delete`     INT DEFAULT '0' COMMENT 'is deleted',
    `is_top`        INT DEFAULT '0' COMMENT 'is top',
    PRIMARY KEY (`chat_id`)
) ;

CREATE TABLE `chat_query`
(
    `id`                BIGINT NOT NULL ,--AUTO_INCREMENT,
    `question_id`       BIGINT DEFAULT NULL,
    `create_time`       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `user_name`         varchar(150)  DEFAULT NULL COMMENT '',
    `question`          varchar(300)  DEFAULT NULL COMMENT '',
    `query_result`      LONGVARCHAR,
    `time`              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
    `state`             int(1) DEFAULT NULL,
    `data_content`      varchar(30)   DEFAULT NULL,
    `name`              varchar(100)  DEFAULT NULL,
    `scene_type`        int(2) DEFAULT NULL,
    `query_type`        int(2) DEFAULT NULL,
    `is_deleted`        int(1) DEFAULT NULL,
    `module`            varchar(30)   DEFAULT NULL,
    `entity`            LONGVARCHAR COMMENT '',
    `chat_id`     BIGINT DEFAULT NULL COMMENT 'chat id',
    `recommend`         text,
    `aggregator`        varchar(20)   DEFAULT 'trend',
    `top_num`           int DEFAULT NULL,
    `start_time`        varchar(30)   DEFAULT NULL,
    `end_time`          varchar(30)   DEFAULT NULL,
    `compare_recommend` LONGVARCHAR,
    `compare_entity`    LONGVARCHAR,
    `query_sql`         LONGVARCHAR,
    `columns`           varchar(2000) DEFAULT NULL,
    `result_list`       LONGVARCHAR,
    `main_entity`       varchar(5000) DEFAULT NULL,
    `semantic_text`     varchar(5000) DEFAULT NULL,
    `score`             int DEFAULT '0',
    `feedback`          varchar(1024) DEFAULT '',
    PRIMARY KEY (`id`)
) ;
