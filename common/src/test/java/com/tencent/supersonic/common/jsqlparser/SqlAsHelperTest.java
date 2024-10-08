package com.tencent.supersonic.common.jsqlparser;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;

class SqlAsHelperTest {

    @Test
    void getAsFields() {
        String sql = "WITH SalesData AS (\n" + "    SELECT \n" + "        SalesID,\n"
                + "        ProductID,\n" + "        Quantity,\n" + "        Price,\n"
                + "        (Quantity * Price) AS TotalSales\n" + "    FROM \n" + "        Sales\n"
                + ")\n" + "SELECT \n" + "    ProductID,\n"
                + "    SUM(TotalSales) AS TotalRevenue,\n" + "    COUNT(SalesID) AS NumberOfSales\n"
                + "FROM \n" + "    SalesData\n" + "WHERE \n" + "    Quantity > 10\n" + "GROUP BY \n"
                + "    ProductID\n" + "HAVING \n" + "    SUM(TotalSales) > 1000\n" + "ORDER BY \n"
                + "    TotalRevenue DESC";
        List<String> asFields = SqlAsHelper.getAsFields(sql);
        Assert.assertTrue(asFields.contains("NumberOfSales"));
        Assert.assertTrue(asFields.contains("TotalRevenue"));
        Assert.assertTrue(asFields.contains("TotalSales"));
    }
}
