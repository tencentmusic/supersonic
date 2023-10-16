package com.tencent.supersonic.semantic.materialization.domain.dataobject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MaterializationRecordDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public MaterializationRecordDOExample() {
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

        public Criteria andMaterializationIdIsNull() {
            addCriterion("materialization_id is null");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdIsNotNull() {
            addCriterion("materialization_id is not null");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdEqualTo(Long value) {
            addCriterion("materialization_id =", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdNotEqualTo(Long value) {
            addCriterion("materialization_id <>", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdGreaterThan(Long value) {
            addCriterion("materialization_id >", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdGreaterThanOrEqualTo(Long value) {
            addCriterion("materialization_id >=", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdLessThan(Long value) {
            addCriterion("materialization_id <", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdLessThanOrEqualTo(Long value) {
            addCriterion("materialization_id <=", value, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdIn(List<Long> values) {
            addCriterion("materialization_id in", values, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdNotIn(List<Long> values) {
            addCriterion("materialization_id not in", values, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdBetween(Long value1, Long value2) {
            addCriterion("materialization_id between", value1, value2, "materializationId");
            return (Criteria) this;
        }

        public Criteria andMaterializationIdNotBetween(Long value1, Long value2) {
            addCriterion("materialization_id not between", value1, value2, "materializationId");
            return (Criteria) this;
        }

        public Criteria andElementTypeIsNull() {
            addCriterion("element_type is null");
            return (Criteria) this;
        }

        public Criteria andElementTypeIsNotNull() {
            addCriterion("element_type is not null");
            return (Criteria) this;
        }

        public Criteria andElementTypeEqualTo(String value) {
            addCriterion("element_type =", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeNotEqualTo(String value) {
            addCriterion("element_type <>", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeGreaterThan(String value) {
            addCriterion("element_type >", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeGreaterThanOrEqualTo(String value) {
            addCriterion("element_type >=", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeLessThan(String value) {
            addCriterion("element_type <", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeLessThanOrEqualTo(String value) {
            addCriterion("element_type <=", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeLike(String value) {
            addCriterion("element_type like", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeNotLike(String value) {
            addCriterion("element_type not like", value, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeIn(List<String> values) {
            addCriterion("element_type in", values, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeNotIn(List<String> values) {
            addCriterion("element_type not in", values, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeBetween(String value1, String value2) {
            addCriterion("element_type between", value1, value2, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementTypeNotBetween(String value1, String value2) {
            addCriterion("element_type not between", value1, value2, "elementType");
            return (Criteria) this;
        }

        public Criteria andElementIdIsNull() {
            addCriterion("element_id is null");
            return (Criteria) this;
        }

        public Criteria andElementIdIsNotNull() {
            addCriterion("element_id is not null");
            return (Criteria) this;
        }

        public Criteria andElementIdEqualTo(Long value) {
            addCriterion("element_id =", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdNotEqualTo(Long value) {
            addCriterion("element_id <>", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdGreaterThan(Long value) {
            addCriterion("element_id >", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdGreaterThanOrEqualTo(Long value) {
            addCriterion("element_id >=", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdLessThan(Long value) {
            addCriterion("element_id <", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdLessThanOrEqualTo(Long value) {
            addCriterion("element_id <=", value, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdIn(List<Long> values) {
            addCriterion("element_id in", values, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdNotIn(List<Long> values) {
            addCriterion("element_id not in", values, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdBetween(Long value1, Long value2) {
            addCriterion("element_id between", value1, value2, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementIdNotBetween(Long value1, Long value2) {
            addCriterion("element_id not between", value1, value2, "elementId");
            return (Criteria) this;
        }

        public Criteria andElementNameIsNull() {
            addCriterion("element_name is null");
            return (Criteria) this;
        }

        public Criteria andElementNameIsNotNull() {
            addCriterion("element_name is not null");
            return (Criteria) this;
        }

        public Criteria andElementNameEqualTo(String value) {
            addCriterion("element_name =", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameNotEqualTo(String value) {
            addCriterion("element_name <>", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameGreaterThan(String value) {
            addCriterion("element_name >", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameGreaterThanOrEqualTo(String value) {
            addCriterion("element_name >=", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameLessThan(String value) {
            addCriterion("element_name <", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameLessThanOrEqualTo(String value) {
            addCriterion("element_name <=", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameLike(String value) {
            addCriterion("element_name like", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameNotLike(String value) {
            addCriterion("element_name not like", value, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameIn(List<String> values) {
            addCriterion("element_name in", values, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameNotIn(List<String> values) {
            addCriterion("element_name not in", values, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameBetween(String value1, String value2) {
            addCriterion("element_name between", value1, value2, "elementName");
            return (Criteria) this;
        }

        public Criteria andElementNameNotBetween(String value1, String value2) {
            addCriterion("element_name not between", value1, value2, "elementName");
            return (Criteria) this;
        }

        public Criteria andDataTimeIsNull() {
            addCriterion("data_time is null");
            return (Criteria) this;
        }

        public Criteria andDataTimeIsNotNull() {
            addCriterion("data_time is not null");
            return (Criteria) this;
        }

        public Criteria andDataTimeEqualTo(String value) {
            addCriterion("data_time =", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeNotEqualTo(String value) {
            addCriterion("data_time <>", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeGreaterThan(String value) {
            addCriterion("data_time >", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeGreaterThanOrEqualTo(String value) {
            addCriterion("data_time >=", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeLessThan(String value) {
            addCriterion("data_time <", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeLessThanOrEqualTo(String value) {
            addCriterion("data_time <=", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeLike(String value) {
            addCriterion("data_time like", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeNotLike(String value) {
            addCriterion("data_time not like", value, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeIn(List<String> values) {
            addCriterion("data_time in", values, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeNotIn(List<String> values) {
            addCriterion("data_time not in", values, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeBetween(String value1, String value2) {
            addCriterion("data_time between", value1, value2, "dataTime");
            return (Criteria) this;
        }

        public Criteria andDataTimeNotBetween(String value1, String value2) {
            addCriterion("data_time not between", value1, value2, "dataTime");
            return (Criteria) this;
        }

        public Criteria andStateIsNull() {
            addCriterion("state is null");
            return (Criteria) this;
        }

        public Criteria andStateIsNotNull() {
            addCriterion("state is not null");
            return (Criteria) this;
        }

        public Criteria andStateEqualTo(String value) {
            addCriterion("state =", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateNotEqualTo(String value) {
            addCriterion("state <>", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateGreaterThan(String value) {
            addCriterion("state >", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateGreaterThanOrEqualTo(String value) {
            addCriterion("state >=", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateLessThan(String value) {
            addCriterion("state <", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateLessThanOrEqualTo(String value) {
            addCriterion("state <=", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateLike(String value) {
            addCriterion("state like", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateNotLike(String value) {
            addCriterion("state not like", value, "state");
            return (Criteria) this;
        }

        public Criteria andStateIn(List<String> values) {
            addCriterion("state in", values, "state");
            return (Criteria) this;
        }

        public Criteria andStateNotIn(List<String> values) {
            addCriterion("state not in", values, "state");
            return (Criteria) this;
        }

        public Criteria andStateBetween(String value1, String value2) {
            addCriterion("state between", value1, value2, "state");
            return (Criteria) this;
        }

        public Criteria andStateNotBetween(String value1, String value2) {
            addCriterion("state not between", value1, value2, "state");
            return (Criteria) this;
        }

        public Criteria andTaskIdIsNull() {
            addCriterion("task_id is null");
            return (Criteria) this;
        }

        public Criteria andTaskIdIsNotNull() {
            addCriterion("task_id is not null");
            return (Criteria) this;
        }

        public Criteria andTaskIdEqualTo(String value) {
            addCriterion("task_id =", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdNotEqualTo(String value) {
            addCriterion("task_id <>", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdGreaterThan(String value) {
            addCriterion("task_id >", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdGreaterThanOrEqualTo(String value) {
            addCriterion("task_id >=", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdLessThan(String value) {
            addCriterion("task_id <", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdLessThanOrEqualTo(String value) {
            addCriterion("task_id <=", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdLike(String value) {
            addCriterion("task_id like", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdNotLike(String value) {
            addCriterion("task_id not like", value, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdIn(List<String> values) {
            addCriterion("task_id in", values, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdNotIn(List<String> values) {
            addCriterion("task_id not in", values, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdBetween(String value1, String value2) {
            addCriterion("task_id between", value1, value2, "taskId");
            return (Criteria) this;
        }

        public Criteria andTaskIdNotBetween(String value1, String value2) {
            addCriterion("task_id not between", value1, value2, "taskId");
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

        public Criteria andRetryCountIsNull() {
            addCriterion("retry_count is null");
            return (Criteria) this;
        }

        public Criteria andRetryCountIsNotNull() {
            addCriterion("retry_count is not null");
            return (Criteria) this;
        }

        public Criteria andRetryCountEqualTo(Long value) {
            addCriterion("retry_count =", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotEqualTo(Long value) {
            addCriterion("retry_count <>", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountGreaterThan(Long value) {
            addCriterion("retry_count >", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountGreaterThanOrEqualTo(Long value) {
            addCriterion("retry_count >=", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountLessThan(Long value) {
            addCriterion("retry_count <", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountLessThanOrEqualTo(Long value) {
            addCriterion("retry_count <=", value, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountIn(List<Long> values) {
            addCriterion("retry_count in", values, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotIn(List<Long> values) {
            addCriterion("retry_count not in", values, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountBetween(Long value1, Long value2) {
            addCriterion("retry_count between", value1, value2, "retryCount");
            return (Criteria) this;
        }

        public Criteria andRetryCountNotBetween(Long value1, Long value2) {
            addCriterion("retry_count not between", value1, value2, "retryCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountIsNull() {
            addCriterion("source_count is null");
            return (Criteria) this;
        }

        public Criteria andSourceCountIsNotNull() {
            addCriterion("source_count is not null");
            return (Criteria) this;
        }

        public Criteria andSourceCountEqualTo(Long value) {
            addCriterion("source_count =", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountNotEqualTo(Long value) {
            addCriterion("source_count <>", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountGreaterThan(Long value) {
            addCriterion("source_count >", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountGreaterThanOrEqualTo(Long value) {
            addCriterion("source_count >=", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountLessThan(Long value) {
            addCriterion("source_count <", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountLessThanOrEqualTo(Long value) {
            addCriterion("source_count <=", value, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountIn(List<Long> values) {
            addCriterion("source_count in", values, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountNotIn(List<Long> values) {
            addCriterion("source_count not in", values, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountBetween(Long value1, Long value2) {
            addCriterion("source_count between", value1, value2, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSourceCountNotBetween(Long value1, Long value2) {
            addCriterion("source_count not between", value1, value2, "sourceCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountIsNull() {
            addCriterion("sink_count is null");
            return (Criteria) this;
        }

        public Criteria andSinkCountIsNotNull() {
            addCriterion("sink_count is not null");
            return (Criteria) this;
        }

        public Criteria andSinkCountEqualTo(Long value) {
            addCriterion("sink_count =", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountNotEqualTo(Long value) {
            addCriterion("sink_count <>", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountGreaterThan(Long value) {
            addCriterion("sink_count >", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountGreaterThanOrEqualTo(Long value) {
            addCriterion("sink_count >=", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountLessThan(Long value) {
            addCriterion("sink_count <", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountLessThanOrEqualTo(Long value) {
            addCriterion("sink_count <=", value, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountIn(List<Long> values) {
            addCriterion("sink_count in", values, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountNotIn(List<Long> values) {
            addCriterion("sink_count not in", values, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountBetween(Long value1, Long value2) {
            addCriterion("sink_count between", value1, value2, "sinkCount");
            return (Criteria) this;
        }

        public Criteria andSinkCountNotBetween(Long value1, Long value2) {
            addCriterion("sink_count not between", value1, value2, "sinkCount");
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