WITH
  _average_stay_duration_ AS (
    SELECT
      AVG(停留时长) AS _avg_duration_
    FROM
      超音数数据集
  )
SELECT
  用户名,
  SUM(停留时长) AS _total_stay_duration_
FROM
  超音数数据集
GROUP BY
  用户名
HAVING
  SUM(停留时长) > (
    SELECT
      _avg_duration_ * 1.5
    FROM
      _average_stay_duration_
  )
  OR SUM(停留时长) < (
    SELECT
      _avg_duration_ * 0.5
    FROM
      _average_stay_duration_
  )