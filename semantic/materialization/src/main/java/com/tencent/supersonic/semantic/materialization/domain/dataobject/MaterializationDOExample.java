package com.tencent.supersonic.semantic.materialization.domain.dataobject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MaterializationDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public MaterializationDOExample() {
        oredCriteria = new ArrayList<>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andNameIsNull() {
            addCriterion("name is null");
            return (Criteria) this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("name is not null");
            return (Criteria) this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("name =", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("name <>", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("name >", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("name >=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("name <", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("name <=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("name like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("name not like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("name in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("name not in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("name between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("name not between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeIsNull() {
            addCriterion("materialized_type is null");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeIsNotNull() {
            addCriterion("materialized_type is not null");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeEqualTo(String value) {
            addCriterion("materialized_type =", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeNotEqualTo(String value) {
            addCriterion("materialized_type <>", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeGreaterThan(String value) {
            addCriterion("materialized_type >", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeGreaterThanOrEqualTo(String value) {
            addCriterion("materialized_type >=", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeLessThan(String value) {
            addCriterion("materialized_type <", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeLessThanOrEqualTo(String value) {
            addCriterion("materialized_type <=", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeLike(String value) {
            addCriterion("materialized_type like", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeNotLike(String value) {
            addCriterion("materialized_type not like", value, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeIn(List<String> values) {
            addCriterion("materialized_type in", values, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeNotIn(List<String> values) {
            addCriterion("materialized_type not in", values, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeBetween(String value1, String value2) {
            addCriterion("materialized_type between", value1, value2, "materializedType");
            return (Criteria) this;
        }

        public Criteria andMaterializedTypeNotBetween(String value1, String value2) {
            addCriterion("materialized_type not between", value1, value2, "materializedType");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleIsNull() {
            addCriterion("update_cycle is null");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleIsNotNull() {
            addCriterion("update_cycle is not null");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleEqualTo(String value) {
            addCriterion("update_cycle =", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleNotEqualTo(String value) {
            addCriterion("update_cycle <>", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleGreaterThan(String value) {
            addCriterion("update_cycle >", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleGreaterThanOrEqualTo(String value) {
            addCriterion("update_cycle >=", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleLessThan(String value) {
            addCriterion("update_cycle <", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleLessThanOrEqualTo(String value) {
            addCriterion("update_cycle <=", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleLike(String value) {
            addCriterion("update_cycle like", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleNotLike(String value) {
            addCriterion("update_cycle not like", value, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleIn(List<String> values) {
            addCriterion("update_cycle in", values, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleNotIn(List<String> values) {
            addCriterion("update_cycle not in", values, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleBetween(String value1, String value2) {
            addCriterion("update_cycle between", value1, value2, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andUpdateCycleNotBetween(String value1, String value2) {
            addCriterion("update_cycle not between", value1, value2, "updateCycle");
            return (Criteria) this;
        }

        public Criteria andModelIdIsNull() {
            addCriterion("model_id is null");
            return (Criteria) this;
        }

        public Criteria andModelIdIsNotNull() {
            addCriterion("model_id is not null");
            return (Criteria) this;
        }

        public Criteria andModelIdEqualTo(Long value) {
            addCriterion("model_id =", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdNotEqualTo(Long value) {
            addCriterion("model_id <>", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdGreaterThan(Long value) {
            addCriterion("model_id >", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdGreaterThanOrEqualTo(Long value) {
            addCriterion("model_id >=", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdLessThan(Long value) {
            addCriterion("model_id <", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdLessThanOrEqualTo(Long value) {
            addCriterion("model_id <=", value, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdIn(List<Long> values) {
            addCriterion("model_id in", values, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdNotIn(List<Long> values) {
            addCriterion("model_id not in", values, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdBetween(Long value1, Long value2) {
            addCriterion("model_id between", value1, value2, "modelId");
            return (Criteria) this;
        }

        public Criteria andModelIdNotBetween(Long value1, Long value2) {
            addCriterion("model_id not between", value1, value2, "modelId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdIsNull() {
            addCriterion("database_id is null");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdIsNotNull() {
            addCriterion("database_id is not null");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdEqualTo(Long value) {
            addCriterion("database_id =", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdNotEqualTo(Long value) {
            addCriterion("database_id <>", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdGreaterThan(Long value) {
            addCriterion("database_id >", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdGreaterThanOrEqualTo(Long value) {
            addCriterion("database_id >=", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdLessThan(Long value) {
            addCriterion("database_id <", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdLessThanOrEqualTo(Long value) {
            addCriterion("database_id <=", value, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdIn(List<Long> values) {
            addCriterion("database_id in", values, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdNotIn(List<Long> values) {
            addCriterion("database_id not in", values, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdBetween(Long value1, Long value2) {
            addCriterion("database_id between", value1, value2, "databaseId");
            return (Criteria) this;
        }

        public Criteria andDatabaseIdNotBetween(Long value1, Long value2) {
            addCriterion("database_id not between", value1, value2, "databaseId");
            return (Criteria) this;
        }

        public Criteria andLevelIsNull() {
            addCriterion("level is null");
            return (Criteria) this;
        }

        public Criteria andLevelIsNotNull() {
            addCriterion("level is not null");
            return (Criteria) this;
        }

        public Criteria andLevelEqualTo(Integer value) {
            addCriterion("level =", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelNotEqualTo(Integer value) {
            addCriterion("level <>", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelGreaterThan(Integer value) {
            addCriterion("level >", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelGreaterThanOrEqualTo(Integer value) {
            addCriterion("level >=", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelLessThan(Integer value) {
            addCriterion("level <", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelLessThanOrEqualTo(Integer value) {
            addCriterion("level <=", value, "level");
            return (Criteria) this;
        }

        public Criteria andLevelIn(List<Integer> values) {
            addCriterion("level in", values, "level");
            return (Criteria) this;
        }

        public Criteria andLevelNotIn(List<Integer> values) {
            addCriterion("level not in", values, "level");
            return (Criteria) this;
        }

        public Criteria andLevelBetween(Integer value1, Integer value2) {
            addCriterion("level between", value1, value2, "level");
            return (Criteria) this;
        }

        public Criteria andLevelNotBetween(Integer value1, Integer value2) {
            addCriterion("level not between", value1, value2, "level");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("status is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("status is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(Integer value) {
            addCriterion("status =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(Integer value) {
            addCriterion("status <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(Integer value) {
            addCriterion("status >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("status >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(Integer value) {
            addCriterion("status <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(Integer value) {
            addCriterion("status <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<Integer> values) {
            addCriterion("status in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<Integer> values) {
            addCriterion("status not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(Integer value1, Integer value2) {
            addCriterion("status between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("status not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andDestinationTableIsNull() {
            addCriterion("destination_table is null");
            return (Criteria) this;
        }

        public Criteria andDestinationTableIsNotNull() {
            addCriterion("destination_table is not null");
            return (Criteria) this;
        }

        public Criteria andDestinationTableEqualTo(String value) {
            addCriterion("destination_table =", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableNotEqualTo(String value) {
            addCriterion("destination_table <>", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableGreaterThan(String value) {
            addCriterion("destination_table >", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableGreaterThanOrEqualTo(String value) {
            addCriterion("destination_table >=", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableLessThan(String value) {
            addCriterion("destination_table <", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableLessThanOrEqualTo(String value) {
            addCriterion("destination_table <=", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableLike(String value) {
            addCriterion("destination_table like", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableNotLike(String value) {
            addCriterion("destination_table not like", value, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableIn(List<String> values) {
            addCriterion("destination_table in", values, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableNotIn(List<String> values) {
            addCriterion("destination_table not in", values, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableBetween(String value1, String value2) {
            addCriterion("destination_table between", value1, value2, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andDestinationTableNotBetween(String value1, String value2) {
            addCriterion("destination_table not between", value1, value2, "destinationTable");
            return (Criteria) this;
        }

        public Criteria andPrincipalsIsNull() {
            addCriterion("principals is null");
            return (Criteria) this;
        }

        public Criteria andPrincipalsIsNotNull() {
            addCriterion("principals is not null");
            return (Criteria) this;
        }

        public Criteria andPrincipalsEqualTo(String value) {
            addCriterion("principals =", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsNotEqualTo(String value) {
            addCriterion("principals <>", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsGreaterThan(String value) {
            addCriterion("principals >", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsGreaterThanOrEqualTo(String value) {
            addCriterion("principals >=", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsLessThan(String value) {
            addCriterion("principals <", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsLessThanOrEqualTo(String value) {
            addCriterion("principals <=", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsLike(String value) {
            addCriterion("principals like", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsNotLike(String value) {
            addCriterion("principals not like", value, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsIn(List<String> values) {
            addCriterion("principals in", values, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsNotIn(List<String> values) {
            addCriterion("principals not in", values, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsBetween(String value1, String value2) {
            addCriterion("principals between", value1, value2, "principals");
            return (Criteria) this;
        }

        public Criteria andPrincipalsNotBetween(String value1, String value2) {
            addCriterion("principals not between", value1, value2, "principals");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIsNull() {
            addCriterion("created_at is null");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIsNotNull() {
            addCriterion("created_at is not null");
            return (Criteria) this;
        }

        public Criteria andCreatedAtEqualTo(Date value) {
            addCriterion("created_at =", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotEqualTo(Date value) {
            addCriterion("created_at <>", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtGreaterThan(Date value) {
            addCriterion("created_at >", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtGreaterThanOrEqualTo(Date value) {
            addCriterion("created_at >=", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtLessThan(Date value) {
            addCriterion("created_at <", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtLessThanOrEqualTo(Date value) {
            addCriterion("created_at <=", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIn(List<Date> values) {
            addCriterion("created_at in", values, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotIn(List<Date> values) {
            addCriterion("created_at not in", values, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtBetween(Date value1, Date value2) {
            addCriterion("created_at between", value1, value2, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotBetween(Date value1, Date value2) {
            addCriterion("created_at not between", value1, value2, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedByIsNull() {
            addCriterion("created_by is null");
            return (Criteria) this;
        }

        public Criteria andCreatedByIsNotNull() {
            addCriterion("created_by is not null");
            return (Criteria) this;
        }

        public Criteria andCreatedByEqualTo(String value) {
            addCriterion("created_by =", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByNotEqualTo(String value) {
            addCriterion("created_by <>", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByGreaterThan(String value) {
            addCriterion("created_by >", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByGreaterThanOrEqualTo(String value) {
            addCriterion("created_by >=", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByLessThan(String value) {
            addCriterion("created_by <", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByLessThanOrEqualTo(String value) {
            addCriterion("created_by <=", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByLike(String value) {
            addCriterion("created_by like", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByNotLike(String value) {
            addCriterion("created_by not like", value, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByIn(List<String> values) {
            addCriterion("created_by in", values, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByNotIn(List<String> values) {
            addCriterion("created_by not in", values, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByBetween(String value1, String value2) {
            addCriterion("created_by between", value1, value2, "createdBy");
            return (Criteria) this;
        }

        public Criteria andCreatedByNotBetween(String value1, String value2) {
            addCriterion("created_by not between", value1, value2, "createdBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIsNull() {
            addCriterion("updated_at is null");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIsNotNull() {
            addCriterion("updated_at is not null");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtEqualTo(Date value) {
            addCriterion("updated_at =", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotEqualTo(Date value) {
            addCriterion("updated_at <>", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtGreaterThan(Date value) {
            addCriterion("updated_at >", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtGreaterThanOrEqualTo(Date value) {
            addCriterion("updated_at >=", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtLessThan(Date value) {
            addCriterion("updated_at <", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtLessThanOrEqualTo(Date value) {
            addCriterion("updated_at <=", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIn(List<Date> values) {
            addCriterion("updated_at in", values, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotIn(List<Date> values) {
            addCriterion("updated_at not in", values, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtBetween(Date value1, Date value2) {
            addCriterion("updated_at between", value1, value2, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotBetween(Date value1, Date value2) {
            addCriterion("updated_at not between", value1, value2, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedByIsNull() {
            addCriterion("updated_by is null");
            return (Criteria) this;
        }

        public Criteria andUpdatedByIsNotNull() {
            addCriterion("updated_by is not null");
            return (Criteria) this;
        }

        public Criteria andUpdatedByEqualTo(String value) {
            addCriterion("updated_by =", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByNotEqualTo(String value) {
            addCriterion("updated_by <>", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByGreaterThan(String value) {
            addCriterion("updated_by >", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByGreaterThanOrEqualTo(String value) {
            addCriterion("updated_by >=", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByLessThan(String value) {
            addCriterion("updated_by <", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByLessThanOrEqualTo(String value) {
            addCriterion("updated_by <=", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByLike(String value) {
            addCriterion("updated_by like", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByNotLike(String value) {
            addCriterion("updated_by not like", value, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByIn(List<String> values) {
            addCriterion("updated_by in", values, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByNotIn(List<String> values) {
            addCriterion("updated_by not in", values, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByBetween(String value1, String value2) {
            addCriterion("updated_by between", value1, value2, "updatedBy");
            return (Criteria) this;
        }

        public Criteria andUpdatedByNotBetween(String value1, String value2) {
            addCriterion("updated_by not between", value1, value2, "updatedBy");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }


    }
}