-- S2VisitsDemo
CREATE TABLE IF NOT EXISTS s2_user_department (
    user_name varchar(200) NOT NULL,
    department varchar(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS s2_pv_uv_statis (
    imp_date DATE NOT NULL,
    user_name varchar(200) NOT NULL,
    page varchar(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS s2_stay_time_statis (
    imp_date DATE NOT NULL,
    user_name varchar(200) NOT NULL,
    stay_hours double precision NOT NULL,
    page varchar(200) NOT NULL
);

-- S2ArtistDemo
CREATE TABLE IF NOT EXISTS singer (
    singer_name varchar(200) NOT NULL,
    act_area varchar(200) NOT NULL,
    song_name varchar(200) NOT NULL,
    genre varchar(200) NOT NULL,
    js_play_cnt bigint DEFAULT NULL,
    down_cnt bigint DEFAULT NULL,
    favor_cnt bigint DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS genre (
    g_name varchar(20) NOT NULL PRIMARY KEY,
    rating integer,
    most_popular_in varchar(50)
);

-- S2CompanyDemo
CREATE TABLE IF NOT EXISTS company (
    company_id varchar(50) NOT NULL,
    company_name varchar(50) NOT NULL,
    headquarter_address varchar(50) NOT NULL,
    company_established_time varchar(20) NOT NULL,
    founder varchar(20) NOT NULL,
    ceo varchar(20) NOT NULL,
    annual_turnover bigint,
    employee_count integer,
    PRIMARY KEY (company_id)
);

CREATE TABLE IF NOT EXISTS brand (
    brand_id varchar(50) NOT NULL,
    brand_name varchar(50) NOT NULL,
    brand_established_time varchar(20) NOT NULL,
    company_id varchar(50) NOT NULL,
    legal_representative varchar(20) NOT NULL,
    registered_capital bigint,
    PRIMARY KEY (brand_id),
    CONSTRAINT fk_brand_company FOREIGN KEY (company_id) REFERENCES company (company_id)
);

CREATE TABLE IF NOT EXISTS brand_revenue (
    year_time varchar(10) NOT NULL,
    brand_id varchar(50) NOT NULL,
    revenue bigint NOT NULL,
    profit bigint NOT NULL,
    revenue_growth_year_on_year double precision NOT NULL,
    profit_growth_year_on_year double precision NOT NULL,
    PRIMARY KEY (year_time, brand_id),
    CONSTRAINT fk_brand_revenue_brand FOREIGN KEY (brand_id) REFERENCES brand (brand_id)
);