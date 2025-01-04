WITH
  weekly_visits AS (
    SELECT
      YEAR (数据日期) AS _year_,
      WEEK (数据日期) AS _week_,
      SUM(访问次数) AS total_visits
    FROM
      超音数数据集
    WHERE
      (
        数据日期 >= '2024-11-18'
        AND 数据日期 <= '2024-11-25'
      )
    GROUP BY
      YEAR (数据日期),
      WEEK (数据日期)
  )
SELECT
  _year_,
  _week_,
  total_visits
FROM
  weekly_visits
WHERE
  (_year_ = YEAR (CURRENT_DATE))
ORDER BY
  total_visits DESC
LIMIT
  1