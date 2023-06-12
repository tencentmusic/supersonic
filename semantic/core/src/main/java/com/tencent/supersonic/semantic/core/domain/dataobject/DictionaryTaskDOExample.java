package com.tencent.supersonic.semantic.core.domain.dataobject;

import java.util.ArrayList;
import java.util.List;

public class DictionaryTaskDOExample {

    /**
     * s2_dictionary_task
     */
    protected String orderByClause;

    /**
     * s2_dictionary_task
     */
    protected boolean distinct;

    /**
     * s2_dictionary_task
     */
    protected List<Criteria> oredCriteria;

    /**
     * s2_dictionary_task
     */
    protected Integer limitStart;

    /**
     * s2_dictionary_task
     */
    protected Integer limitEnd;

    /**
     * @mbg.generated
     */
    public DictionaryTaskDOExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    /**
     * @mbg.generated
     */
    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    /**
     * @mbg.generated
     */
    public String getOrderByClause() {
        return orderByClause;
    }

    /**
     * @mbg.generated
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * @mbg.generated
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * @mbg.generated
     */
    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    /**
     * @mbg.generated
     */
    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    /**
     * @mbg.generated
     */
    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    /**
     * @mbg.generated
     */
    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    /**
     * @mbg.generated
     */
    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    /**
     * @mbg.generated
     */
    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    /**
     * @mbg.generated
     */
    public void setLimitStart(Integer limitStart) {
        this.limitStart = limitStart;
    }

    /**
     * @mbg.generated
     */
    public Integer getLimitStart() {
        return limitStart;
    }

    /**
     * @mbg.generated
     */
    public void setLimitEnd(Integer limitEnd) {
        this.limitEnd = limitEnd;
    }

    /**
     * @mbg.generated
     */
    public Integer getLimitEnd() {
        return limitEnd;
    }

    /**
     * s2_dictionary_task null
     */
    protected abstract static class GeneratedCriteria {

        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
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

        public Criteria andDomainIdIsNull() {
            addCriterion("domain_id is null");
            return (Criteria) this;
        }

        public Criteria andDomainIdIsNotNull() {
            addCriterion("domain_id is not null");
            return (Criteria) this;
        }

        public Criteria andDomainIdEqualTo(Long value) {
            addCriterion("domain_id =", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotEqualTo(Long value) {
            addCriterion("domain_id <>", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdGreaterThan(Long value) {
            addCriterion("domain_id >", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdGreaterThanOrEqualTo(Long value) {
            addCriterion("domain_id >=", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdLessThan(Long value) {
            addCriterion("domain_id <", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdLessThanOrEqualTo(Long value) {
            addCriterion("domain_id <=", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdIn(List<Long> values) {
            addCriterion("domain_id in", values, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotIn(List<Long> values) {
            addCriterion("domain_id not in", values, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdBetween(Long value1, Long value2) {
            addCriterion("domain_id between", value1, value2, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotBetween(Long value1, Long value2) {
            addCriterion("domain_id not between", value1, value2, "domainId");
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

        public Criteria andElapsedMsIsNull() {
            addCriterion("elapsed_ms is null");
            return (Criteria) this;
        }

        public Criteria andElapsedMsIsNotNull() {
            addCriterion("elapsed_ms is not null");
            return (Criteria) this;
        }

        public Criteria andElapsedMsEqualTo(Long value) {
            addCriterion("elapsed_ms =", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsNotEqualTo(Long value) {
            addCriterion("elapsed_ms <>", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsGreaterThan(Long value) {
            addCriterion("elapsed_ms >", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsGreaterThanOrEqualTo(Long value) {
            addCriterion("elapsed_ms >=", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsLessThan(Long value) {
            addCriterion("elapsed_ms <", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsLessThanOrEqualTo(Long value) {
            addCriterion("elapsed_ms <=", value, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsIn(List<Long> values) {
            addCriterion("elapsed_ms in", values, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsNotIn(List<Long> values) {
            addCriterion("elapsed_ms not in", values, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsBetween(Long value1, Long value2) {
            addCriterion("elapsed_ms between", value1, value2, "elapsedMs");
            return (Criteria) this;
        }

        public Criteria andElapsedMsNotBetween(Long value1, Long value2) {
            addCriterion("elapsed_ms not between", value1, value2, "elapsedMs");
            return (Criteria) this;
        }
    }

    /**
     * s2_dictionary_task
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    /**
     * s2_dictionary_task null
     */
    public static class Criterion {

        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

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
    }
}