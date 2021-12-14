/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.criteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;

/**
 * Predicate is a expression that evaluates to true or false.
 * All boolean expressions are implemented as Predicate.
 * A predicate can have zero or more predicate arguments.
 * Default predicate operator is AND (conjunction).
 * Two constant predicates are Predicate.TRUE and Predicate.FALSE.
 * AND predicate with no argument evaluates to TRUE.
 * OR predicate with no argument evaluates to FALSE.
 * Negation of a Predicate creates a new Predicate.
 *
 * @author Pinaki Poddar
 * @author Fay Wang
 *
 * @since 2.0.0
 */
abstract class PredicateImpl extends ExpressionImpl<Boolean> implements Predicate {

    static final Expression<Boolean> TRUE_CONSTANT = new Expressions.Constant<>(true);
    static final Expression<Boolean> FALSE_CONSTANT = new Expressions.Constant<>(false);

    private static Predicate TRUE;
    private static Predicate FALSE;

    // Contents of a predicate can only be instances of ExpressionImpl, since we
    // need to call toKernelExpression(), which is internal OpenJPA method.
    protected final List<ExpressionImpl<Boolean>> _exps = Collections.synchronizedList(new ArrayList<>());
    private final BooleanOperator _op;
    private boolean _negated = false;

    /**
     * An AND predicate with no arguments.
     */
    protected PredicateImpl() {
        this(BooleanOperator.AND);
    }

    /**
     * A predicate with the given operator.
     */
    protected PredicateImpl(BooleanOperator op) {
        super(Boolean.class);
        _op = op;
    }

    /**
     * A predicate of given operator with given arguments.
     */
    protected PredicateImpl(BooleanOperator op, Predicate...restrictions) {
        this(op);
        if (restrictions == null || restrictions.length == 0) return;

    	for (Predicate p : restrictions) {
   			add(p);
    	}
    }

    /**
     * Adds the given predicate expression.
     */
    public PredicateImpl add(Expression<Boolean> s) {
    	synchronized (_exps) {
        	_exps.add((ExpressionImpl<Boolean>) s);
		}
        return this;
    }

    @Override
    public List<Expression<Boolean>> getExpressions() {
        List<Expression<Boolean>> result = new CopyOnWriteArrayList<>();
        if (_exps.isEmpty())
            return result;
        result.addAll(_exps);
        return result;
    }

    @Override
    public final BooleanOperator getOperator() {
        return _op;
    }

    public final boolean isEmpty() {
        return _exps.isEmpty();
    }

    /**
     * Is this predicate created by negating another predicate?
     */
    @Override
    public final boolean isNegated() {
        return _negated;
    }

    /**
     * Returns a new predicate as the negation of this predicate.
     * <br>
     * Note:
     * Default negation creates a Not expression with this receiver as delegate.
     * Derived predicates can return the inverse expression, if exists.
     * For example, NotEqual for Equal or LessThan for GreaterThanEqual etc.
     */
    @Override
    public PredicateImpl not() {
        return new Not(this);
    }

    protected PredicateImpl markNegated() {
        _negated = true;
        return this;
    }

    public static Predicate TRUE() {
    	if (TRUE == null) {
    		TRUE = of(TRUE_CONSTANT);
    	}
    	return TRUE;
    }

    public static Predicate FALSE() {
    	if (FALSE == null) {
    		FALSE = of(FALSE_CONSTANT);
    	}
    	return FALSE;
    }

    public static PredicateImpl of(Expression<Boolean> expr) {
        if (expr instanceof PredicateImpl) {
            return (PredicateImpl) expr;
        }
        return new Expr((ExpressionImpl<Boolean>) expr);
    }

    /* OPENJPA-2895 This filler method was a *really* bad idea.
       It assumes that implementations behave well; however a lot of them don't
       populate the _exps, in which case they get converted to TRUE without any
       reasonable cause, leading to horrible bugs.
    @Override
    org.apache.openjpa.kernel.exps.Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        if (_exps.isEmpty()) {
            return factory.newLiteral(_op == BooleanOperator.AND, Literal.TYPE_BOOLEAN);
        }
        throw new AbstractMethodError(this.getClass().getName());
    }
     */

    @Override
    org.apache.openjpa.kernel.exps.Expression toKernelExpression(ExpressionFactory factory, CriteriaQueryImpl<?> q) {

        org.apache.openjpa.kernel.exps.Expression result;

        if (_exps.isEmpty()) {
            Predicate nil = _op == BooleanOperator.AND ? TRUE() : FALSE();
            result = ((PredicateImpl) nil).toKernelExpression(factory, q);

        } else {

            BiFunction<org.apache.openjpa.kernel.exps.Expression,
                    org.apache.openjpa.kernel.exps.Expression,
                    org.apache.openjpa.kernel.exps.Expression> apply =
                    (e1,e2)-> _op == BooleanOperator.AND ? factory.and(e1, e2) : factory.or(e1, e2);

            result = null;

            for (ExpressionImpl<Boolean> exp : _exps) {

                org.apache.openjpa.kernel.exps.Expression item =
                        exp.toKernelExpression(factory, q);

                if (result == null) {
                    result = item;
                } else {
                    result = apply.apply(result, item);
                }
            }

        }

        return _negated ? factory.not(result) : result;
    }

    @Override
    public void acceptVisit(CriteriaExpressionVisitor visitor) {
        Expressions.acceptVisit(visitor, this, _exps.toArray(new Expression<?>[_exps.size()]));
    }

    @Override
    public StringBuilder asValue(AliasContext q) {
        boolean braces = _exps.size() > 1;
        StringBuilder buffer =  Expressions.asValue(q, _exps.toArray(new Expression<?>[_exps.size()]), " " +_op + " ");
        if (braces) buffer.insert(0, "(").append(")");
        if (isNegated()) buffer.insert(0, "NOT ");
        return buffer;
    }

    /**
     * Simple expression wrapper.
     */
    static class Expr extends PredicateImpl {
        public Expr(ExpressionImpl<Boolean> of) {
            add(of);
        }

        @Override
        Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> q) {
            return _exps.get(0).toValue(factory, q);
        }
    }

    /**
     * Concrete NOT predicate.
     */
    static class Not extends PredicateImpl {
        public Not(PredicateImpl of) {
            add(of);
            markNegated();
        }

    }

    /**
     * Concrete AND predicate.
     *
     */
    static class And extends PredicateImpl {
        public And(Expression<Boolean> x, Expression<Boolean> y) {
            super(BooleanOperator.AND);
            add(x).add(y);
        }

        public And(Predicate...restrictions) {
            super(BooleanOperator.AND, restrictions);
        }
    }

    /**
     * Concrete OR predicate.
     *
     */
    static class Or extends PredicateImpl {
        public Or(Expression<Boolean> x, Expression<Boolean> y) {
            super(BooleanOperator.OR);
            add(x).add(y);
        }

        public Or(Predicate...restrictions) {
            super(BooleanOperator.OR, restrictions);
        }
    }
}
