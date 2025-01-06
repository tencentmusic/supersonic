WITH
  recent_week AS (
    SELECT
      SUM(访问次数) AS _访问次数_,
      COUNT(DISTINCT 用户名) AS _访问用户数_
    FROM
      超音数数据集
    WHERE
      数据日期 >= '2024-12-20'
      AND 数据日期 <= '2024-12-27'
  ),
  first_week_december AS (
    SELECT
      SUM(访问次数) AS _访问次数_,
      COUNT(DISTINCT 用户名) AS _访问用户数_
    FROM
      超音数数据集
    WHERE
      数据日期 >= '2024-12-01'
      AND 数据日期 <= '2024-12-07'
  )
SELECT
  '最近7天' AS _时间段_,
  _访问次数_,
  _访问用户数_
FROM
  recent_week
UNION ALL
SELECT
  '12月第一个星期' AS _时间段_,
  _访问次数_,
  _访问用户数_
FROM
  first_week_december