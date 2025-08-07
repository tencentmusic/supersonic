package com.tencent.supersonic.chat.server.userplugin.base.dao;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import net.hasor.dataql.fx.db.fxquery.FxQuery;
import net.hasor.dataql.fx.db.likemybatis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;


public abstract class DynamicDao {
    @Autowired
    protected DatabaseService databaseService;
    @Autowired
    protected SqlUtils sqlUtils;
    private DatabaseResp database;

    protected abstract void init();

    protected void setDatabaseRespById(Long id) {
        this.database = databaseService.getDatabase(id);
    }

    protected List<Map<String, Object>> selectBySql(String sql, Map<String, Object> paramMap) {
        init();
        sqlUtils = sqlUtils.init(database);
        try {
            FxQuery fxSql = this.getFxQuery(sql);
            String buildQueryString = fxSql.buildQueryString(paramMap);
            Object[] buildQueryParams = fxSql.buildParameterSource(paramMap).toArray();
            Date date = new Date();
            List<Map<String, Object>> res =
                    sqlUtils.jdbcTemplate().queryForList(buildQueryString, buildQueryParams);
            System.out.println(paramMap + " : " + (new Date().getTime() - date.getTime()) / 1000);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FxQuery getFxQuery(String fragmentString) throws Exception {
        if (!fragmentString.trim().startsWith("<select>")) {
            fragmentString = "<select>\r\n" + fragmentString + "\r\n</select>";
        }
        if (fragmentString.contains(" < ")) {
            fragmentString = fragmentString.replaceAll(" < ", " &lt; ");
        }
        if (fragmentString.contains(" <= ")) {
            fragmentString = fragmentString.replaceAll(" <= ", " &lt;= ");
        }
        SqlNode sqlNode = this.parseSqlNode(fragmentString.trim());
        FxQuery fxSql = new MybatisSqlQuery(sqlNode);
        return fxSql;
    }

    private synchronized SqlNode parseSqlNode(String fragmentString) throws Exception {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document =
                documentBuilder.parse(new ByteArrayInputStream(fragmentString.getBytes()));
        Element root = document.getDocumentElement();
        String tagName = root.getTagName();
        SqlNode sqlNode = new TextSqlNode("");
        if (!"select".equalsIgnoreCase(tagName)) {
            throw new RuntimeException("只能拿做查询");
        }

        this.parseNodeList(sqlNode, root.getChildNodes());
        return sqlNode;
    }

    private void parseNodeList(SqlNode sqlNode, NodeList nodeList) {
        int i = 0;

        for (int len = nodeList.getLength(); i < len; ++i) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == 3) {
                sqlNode.addChildNode(new TextSqlNode(node.getNodeValue().trim()));
            } else if (node.getNodeType() != 8) {
                String nodeName = node.getNodeName();
                Object childNode;
                if ("foreach".equalsIgnoreCase(nodeName)) {
                    childNode = this.parseForeachSqlNode(node);
                } else {
                    if (!"if".equalsIgnoreCase(nodeName)) {
                        throw new UnsupportedOperationException("Unsupported tags :" + nodeName);
                    }
                    childNode = new IfSqlNode(this.getNodeAttributeValue(node, "test"));
                }

                sqlNode.addChildNode((SqlNode) childNode);
                if (node.hasChildNodes()) {
                    this.parseNodeList((SqlNode) childNode, node.getChildNodes());
                }
            }
        }
    }

    private ForeachSqlNode parseForeachSqlNode(Node node) {
        ForeachSqlNode foreachSqlNode = new ForeachSqlNode();
        foreachSqlNode.setCollection(this.getNodeAttributeValue(node, "collection"));
        foreachSqlNode.setSeparator(this.getNodeAttributeValue(node, "separator"));
        foreachSqlNode.setClose(this.getNodeAttributeValue(node, "close"));
        foreachSqlNode.setOpen(this.getNodeAttributeValue(node, "open"));
        foreachSqlNode.setItem(this.getNodeAttributeValue(node, "item"));
        return foreachSqlNode;
    }

    private String getNodeAttributeValue(Node node, String attributeKey) {
        Node item = node.getAttributes().getNamedItem(attributeKey);
        return item != null ? item.getNodeValue() : null;
    }
}
